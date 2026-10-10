document.addEventListener("DOMContentLoaded", function () {
    const tableBody = document.getElementById("dashboardStudents");
    if (!tableBody) {
        return;
    }

    const counts = { HIGH: 0, MEDIUM: 0, LOW: 0 };
    const rows = [];

    async function loadDashboard() {
        try {
            const studentsResponse = await fetch("/api/students");
            if (!studentsResponse.ok) {
                throw new Error("Student data unavailable");
            }

            const students = await studentsResponse.json();
            const assessments = await Promise.all(students.map(async function (student) {
                const response = await fetch(
                    "http://localhost:8081/api/risk/students/" + student.studentId
                );
                if (!response.ok) {
                    return { student, error: "Risk assessment unavailable" };
                }
                return { student, result: await response.json() };
            }));

            for (const assessment of assessments) {
                const student = assessment.student;
                const result = assessment.result;
                if (result && counts[result.riskTier] !== undefined) {
                    counts[result.riskTier]++;
                }

                const row = document.createElement("tr");
                const tier = result ? result.riskTier : "UNAVAILABLE";
                const factors = result
                    ? result.contributingFactors.join(" ")
                    : assessment.error;
                const recommendation = result
                    ? result.recommendedInterventions[0]
                    : "Review service availability";
                row.innerHTML =
                    "<td>" + escapeHtml(student.studentCode) + "</td>" +
                    "<td><strong>" + escapeHtml(student.studentCode) + "</strong></td>" +
                    "<td>" + escapeHtml(student.className) + "</td>" +
                    "<td><span class=\"risk-badge\">" + tier + "</span></td>" +
                    "<td>" + escapeHtml(factors) + "</td>" +
                    "<td>" + escapeHtml(recommendation) + "</td>";
                rows.push(row);
            }

            tableBody.replaceChildren(...rows);
            updateSummary(students.length);
            document.getElementById("dashboardStatus").textContent =
                "Live results support educator review and are not definitive dropout predictions.";
        } catch (error) {
            document.getElementById("dashboardStatus").textContent =
                "Live dashboard data is unavailable. Verify both services are running.";
        }
    }

    function updateSummary(total) {
        document.getElementById("totalStudents").textContent = total;
        for (const tier of ["HIGH", "MEDIUM", "LOW"]) {
            const percent = total ? Math.round((counts[tier] / total) * 100) : 0;
            const key = tier.toLowerCase();
            document.getElementById(key + "RiskCount").textContent = counts[tier];
            document.getElementById(key + "RiskPercent").textContent = percent + "%";
            document.getElementById(key + "RiskBar").style.width = percent + "%";
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

    loadDashboard();
});
