document.addEventListener("DOMContentLoaded", function () {
    const classFilter = document.getElementById("classFilter");
    const termFilter = document.getElementById("termFilter");
    const studentSearch = document.getElementById("studentSearch");
    const tableBody = document.getElementById("studentTableBody");
    const studentCount = document.getElementById("studentCount");
    const emptyMessage = document.getElementById("emptyMessage");
    const riskServiceUrl = document.querySelector("[data-risk-service-url]")?.dataset.riskServiceUrl
        || "http://localhost:8081";

    // Stop safely if this script is loaded on another page.
    if (
        !classFilter ||
        !termFilter ||
        !studentSearch ||
        !tableBody ||
        !studentCount ||
        !emptyMessage
    ) {
        return;
    }

    let rows = [];

    async function loadStudents() {
        studentCount.textContent = "Loading students...";
        try {
            const response = await fetch("/api/students");
            if (!response.ok) {
                throw new Error("Student service returned " + response.status);
            }

            const students = await response.json();
            tableBody.replaceChildren();

            for (const student of students) {
                const row = document.createElement("tr");
                row.dataset.class = student.className || "";
                row.dataset.term = student.term || "";
                row.innerHTML = `
                    <td>${escapeHtml(student.studentCode)}</td>
                    <td>${escapeHtml(student.studentCode)}</td>
                    <td>${escapeHtml(student.className)}</td>
                    <td>${escapeHtml(student.term)}</td>
                    <td><span class="risk-badge">Not assessed</span></td>
                    <td>
                        <button class="text-link risk-button" type="button">Assess risk</button>
                        <div class="risk-details"></div>
                    </td>
                `;
                row.querySelector(".risk-button").addEventListener("click", function () {
                    assessRisk(student.studentId, row);
                });
                tableBody.appendChild(row);
            }

            rows = Array.from(tableBody.querySelectorAll("tr"));
            filterStudents();
        } catch (error) {
            studentCount.textContent = "Unable to load students";
            emptyMessage.hidden = false;
            emptyMessage.textContent =
                "Student data is unavailable. Start the Student Data Service and verify PostgreSQL.";
        }
    }

    async function assessRisk(studentId, row) {
        const badge = row.querySelector(".risk-badge");
        const button = row.querySelector(".risk-button");
        badge.textContent = "Loading";
        button.disabled = true;

        try {
            const url = new URL(
                "/api/risk/students/" + encodeURIComponent(studentId),
                riskServiceUrl
            );
            const response = await fetch(url);
            const result = await response.json();
            if (!response.ok) {
                throw new Error(result.detail || result.message || "Risk assessment failed");
            }
            const modelAvailable = ["HIGH", "MEDIUM", "LOW"].includes(result.modelRiskTier)
                && Number.isFinite(result.modelProbability);
            const modelExpected = result.riskModel === "LOGISTIC_REGRESSION";
            const tier = modelAvailable
                ? result.modelRiskTier
                : modelExpected ? "Model unavailable" : result.riskTier;
            badge.textContent = tier;
            badge.className = "risk-badge "
                + (["HIGH", "MEDIUM", "LOW"].includes(tier)
                    ? tier.toLowerCase() : "unavailable");
            button.textContent = modelAvailable
                ? "Model " + Math.round(result.modelProbability * 100) + "% · " + tier
                : modelExpected
                    ? "Heuristic " + result.riskTier + " · Score " + result.riskScore
                    : "Score " + result.riskScore;
            const modelFactors = Array.isArray(result.modelContributingFactors)
                ? result.modelContributingFactors
                    .filter(factor => Number.isFinite(factor.logOddsContribution)
                        && factor.logOddsContribution > 0)
                    .map(factor => factor.feature + " " + factor.value + "% (model log-odds +"
                        + factor.logOddsContribution.toFixed(3) + " vs synthetic mean "
                        + factor.referenceMean + "%)")
                : [];
            const explanationFactors = modelAvailable
                ? (modelFactors.length
                    ? modelFactors
                    : ["No feature has a positive contribution relative to its synthetic training mean."])
                : result.contributingFactors || [];
            row.querySelector(".risk-details").textContent = [
                modelAvailable ? result.modelNote : "",
                ...explanationFactors,
                ...(result.recommendedInterventions || [])
            ].filter(Boolean).join(" ");
        } catch (error) {
            badge.textContent = "Unavailable";
            button.textContent = "Assessment unavailable";
            button.title = error.message;
        } finally {
            button.disabled = false;
        }
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function filterStudents() {
        const selectedClass = classFilter.value;
        const selectedTerm = termFilter.value;
        const searchText = studentSearch.value
            .trim()
            .toLowerCase();

        let visibleCount = 0;

        rows.forEach(function (row) {
            const studentId = row.cells[0].textContent
                .trim()
                .toLowerCase();

            const studentName = row.cells[1].textContent
                .trim()
                .toLowerCase();

            const studentClass = row.dataset.class;
            const studentTerm = row.dataset.term;

            const matchesClass =
                !selectedClass || studentClass === selectedClass;

            const matchesTerm =
                !selectedTerm || studentTerm === selectedTerm;

            const matchesSearch =
                !searchText ||
                studentId.includes(searchText) ||
                studentName.includes(searchText);

            const isVisible =
                matchesClass && matchesTerm && matchesSearch;

            row.hidden = !isVisible;

            if (isVisible) {
                visibleCount++;
            }
        });

        studentCount.textContent =
            visibleCount + (visibleCount === 1 ? " student" : " students");

        emptyMessage.hidden = visibleCount !== 0;
    }

    classFilter.addEventListener("change", filterStudents);
    termFilter.addEventListener("change", filterStudents);
    studentSearch.addEventListener("input", filterStudents);

    loadStudents();
});