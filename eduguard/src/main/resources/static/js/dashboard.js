document.addEventListener("DOMContentLoaded", function () {
    const elements = {
        tableBody: document.getElementById("dashboardStudents"),
        classFilter: document.getElementById("dashboardClassFilter"),
        termFilter: document.getElementById("dashboardTermFilter"),
        search: document.getElementById("dashboardSearch"),
        status: document.getElementById("dashboardStatus"),
        coverage: document.getElementById("assessmentCoverage"),
        empty: document.getElementById("dashboardEmpty"),
        caseFormPanel: document.getElementById("case-form-panel"),
        caseForm: document.getElementById("caseForm"),
        caseFormMessage: document.getElementById("caseFormMessage"),
        caseStudentId: document.getElementById("caseStudentId"),
        caseStudentLabel: document.getElementById("caseStudentLabel"),
        caseLoadStatus: document.getElementById("caseLoadStatus"),
        caseSummary: document.getElementById("caseSummary"),
        casesBody: document.getElementById("interventionCases"),
        casesEmpty: document.getElementById("casesEmpty"),
        academicResources: document.getElementById("academicResources"),
        wellbeingResources: document.getElementById("wellbeingResources")
    };

    if (!elements.tableBody || !elements.classFilter || !elements.termFilter) {
        return;
    }

    const riskServiceUrl = document.querySelector("[data-risk-service-url]")?.dataset.riskServiceUrl
        || "http://localhost:8081";
    const riskCounts = { HIGH: 0, MEDIUM: 0, LOW: 0 };
    let students = [];
    let assessments = [];
    let caseRecords = [];
    let cohortRequest = 0;

    async function requestJson(url, options) {
        const response = await fetch(url, options);
        if (!response.ok) {
            const error = new Error("Request failed");
            error.status = response.status;
            throw error;
        }
        return response.json();
    }

    function textElement(tagName, text, className) {
        const element = document.createElement(tagName);
        element.textContent = text == null ? "" : String(text);
        if (className) {
            element.className = className;
        }
        return element;
    }

    function addCell(row, value, className) {
        const cell = document.createElement("td");
        cell.textContent = value == null || value === "" ? "—" : String(value);
        if (className) {
            cell.className = className;
        }
        row.append(cell);
        return cell;
    }

    function populateFilter(select, values, label) {
        const selected = select.value;
        select.replaceChildren(new Option("All " + label, ""));
        values.forEach(function (value) {
            select.add(new Option(value, value));
        });
        if (values.includes(selected)) {
            select.value = selected;
        }
    }

    async function loadStudents() {
        elements.status.textContent = "Loading student records...";
        try {
            students = await requestJson("/api/students");
            if (!Array.isArray(students)) {
                throw new Error("Invalid student response");
            }
            populateFilter(elements.classFilter, uniqueSorted(students.map(student => student.className)), "classes");
            populateFilter(elements.termFilter, uniqueSorted(students.map(student => student.term)), "terms");
            renderCases(caseRecords);
            await loadCohort();
        } catch (error) {
            elements.status.textContent = "Student records are unavailable. Verify the Student Data Service.";
            elements.coverage.textContent = "Cohort assessments unavailable.";
            renderAssessments([]);
            updateSummary(0);
        }
    }

    async function loadCohort() {
        const requestId = ++cohortRequest;
        const selectedClass = elements.classFilter.value;
        const selectedTerm = elements.termFilter.value;
        const query = new URLSearchParams();
        if (selectedClass) query.set("className", selectedClass);
        if (selectedTerm) query.set("term", selectedTerm);

        elements.status.textContent = "Loading risk assessments...";
        elements.coverage.textContent = "Assessing selected cohort...";
        const endpoint = query.size ? "/api/students?" + query.toString() : "/api/students";

        try {
            const cohortStudents = await requestJson(endpoint);
            if (!Array.isArray(cohortStudents)) {
                throw new Error("Invalid student response");
            }
            const results = await Promise.all(cohortStudents.map(async function (student) {
                try {
                    const url = new URL(
                        "/api/risk/students/" + encodeURIComponent(student.studentId),
                        riskServiceUrl
                    );
                    const risk = await requestJson(url);
                    return { student, risk };
                } catch (error) {
                    return { student, error };
                }
            }));
            if (requestId !== cohortRequest) {
                return;
            }

            assessments = results;
            updateSummary(cohortStudents.length);
            renderAssessments(assessments);
            const available = assessments.filter(item => item.risk).length;
            elements.coverage.textContent =
                available + " of " + cohortStudents.length + " students have a risk assessment.";
            elements.status.textContent = cohortStudents.length
                ? "Live assessments support educator review; they are not definitive predictions."
                : "No students are available in this cohort.";
        } catch (error) {
            if (requestId !== cohortRequest) {
                return;
            }
            elements.coverage.textContent = "Cohort assessments unavailable.";
            elements.status.textContent = "Could not load this cohort. Verify the Student Data Service.";
            assessments = [];
            updateSummary(0);
            renderAssessments([]);
        }
    }

    function uniqueSorted(values) {
        return [...new Set(values.filter(value => typeof value === "string" && value.trim()))]
            .sort((left, right) => left.localeCompare(right));
    }

    function updateSummary(total) {
        riskCounts.HIGH = 0;
        riskCounts.MEDIUM = 0;
        riskCounts.LOW = 0;
        assessments.forEach(function (item) {
            if (item.risk && Object.hasOwn(riskCounts, item.risk.riskTier)) {
                riskCounts[item.risk.riskTier]++;
            }
        });

        document.getElementById("totalStudents").textContent = String(total);
        ["HIGH", "MEDIUM", "LOW"].forEach(function (tier) {
            const key = tier.toLowerCase();
            const percentage = total ? Math.round((riskCounts[tier] / total) * 100) : 0;
            document.getElementById(key + "RiskCount").textContent = String(riskCounts[tier]);
            document.getElementById(key + "RiskPercent").textContent = percentage + "%";
            document.getElementById(key + "RiskBar").style.width = percentage + "%";
        });
    }

    function renderAssessments(items) {
        const search = document.getElementById("dashboardSearch").value.trim().toLowerCase();
        const visible = items.filter(function (item) {
            return !search || String(item.student.studentCode || "").toLowerCase().includes(search);
        });
        const rows = visible.map(function (item) {
            return createStudentRow(item);
        });
        elements.tableBody.replaceChildren(...rows);
        elements.empty.hidden = visible.length > 0;
    }

    function createStudentRow(item) {
        const student = item.student;
        const risk = item.risk;
        const row = document.createElement("tr");
        addCell(row, student.studentCode);
        addCell(row, student.className);
        addCell(row, student.term);

        const tierCell = document.createElement("td");
        const tier = risk && ["HIGH", "MEDIUM", "LOW"].includes(risk.riskTier)
            ? risk.riskTier : "UNAVAILABLE";
        tierCell.append(textElement("span", tier, "risk-badge " + tier.toLowerCase() + "-badge"));
        row.append(tierCell);

        const explanationCell = document.createElement("td");
        if (risk) {
            const details = document.createElement("details");
            const summary = textElement("summary", "View explanation");
            details.append(summary);
            const factors = Array.isArray(risk.contributingFactors)
                ? risk.contributingFactors : [];
            const factorList = document.createElement("ul");
            factors.forEach(factor => factorList.append(textElement("li", factor)));
            details.append(factorList);
            explanationCell.append(details);
        } else {
            explanationCell.textContent = "Risk assessment unavailable";
        }
        row.append(explanationCell);

        const recommendationCell = document.createElement("td");
        const recommendations = risk && Array.isArray(risk.recommendedInterventions)
            ? risk.recommendedInterventions : [];
        recommendationCell.textContent = recommendations.length
            ? recommendations.join(" ")
            : risk ? "No suggested action available." : "Review service availability.";
        row.append(recommendationCell);

        const actionCell = document.createElement("td");
        const createButton = textElement("button", "Record support case", "primary-button");
        createButton.type = "button";
        createButton.addEventListener("click", function () {
            openCaseForm(student);
        });
        actionCell.append(createButton);
        row.append(actionCell);
        return row;
    }

    function openCaseForm(student) {
        elements.caseForm.reset();
        elements.caseStudentId.value = String(student.studentId);
        elements.caseStudentLabel.textContent = "For student " + student.studentCode;
        elements.caseFormPanel.hidden = false;
        elements.caseFormPanel.scrollIntoView({ behavior: "smooth", block: "start" });
        document.getElementById("caseTitle").focus();
    }

    async function submitCase(event) {
        event.preventDefault();
        const submitButton = document.getElementById("saveCaseButton");
        submitButton.disabled = true;
        elements.caseFormMessage.textContent = "Saving support case...";
        const payload = {
            studentId: Number(elements.caseStudentId.value),
            title: document.getElementById("caseTitle").value.trim(),
            description: document.getElementById("caseDescription").value.trim(),
            category: document.getElementById("caseCategory").value,
            priority: document.getElementById("casePriority").value,
            assignedStaffId: optionalValue("caseAssignedStaff"),
            followUpDate: optionalValue("caseFollowUpDate")
        };
        try {
            await requestJson("/api/interventions", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            elements.caseFormMessage.textContent = "Support case saved.";
            elements.caseForm.reset();
            elements.caseFormPanel.hidden = true;
            await loadCases();
        } catch (error) {
            elements.caseFormMessage.textContent = error.status === 400
                ? "The case details are invalid. Check the required fields and follow-up date."
                : error.status === 404
                    ? "That student record no longer exists."
                    : "The support case could not be saved. Check service availability and database setup.";
        } finally {
            submitButton.disabled = false;
        }
    }

    function optionalValue(id) {
        const value = document.getElementById(id).value.trim();
        return value || null;
    }

    async function loadCases() {
        elements.caseLoadStatus.textContent = "Loading saved cases and follow-ups...";
        const [pageResult, overdueResult] = await Promise.allSettled([
            requestJson("/api/interventions?page=0&size=100"),
            requestJson("/api/interventions/follow-ups/overdue")
        ]);
        if (pageResult.status !== "fulfilled") {
            elements.caseLoadStatus.textContent =
                "Saved case data is unavailable. Confirm the Stage 4 migration and service are ready.";
            elements.caseSummary.replaceChildren();
            elements.casesBody.replaceChildren();
            elements.casesEmpty.hidden = true;
            return;
        }
        const page = pageResult.value;
        const cases = page && Array.isArray(page.content) ? page.content : [];
        const overdueCases = overdueResult.status === "fulfilled" && Array.isArray(overdueResult.value)
            ? overdueResult.value : null;
        const activeCount = cases.filter(item => item.status !== "CLOSED").length;
        elements.caseLoadStatus.textContent = overdueCases
            ? cases.length + " recent cases · " + activeCount + " open or active · "
                + overdueCases.length + " overdue follow-ups"
            : cases.length + " recent cases · " + activeCount + " open or active · "
                + "Overdue follow-ups are unavailable.";
        renderCases(cases);
    }

    function renderCases(cases) {
        caseRecords = cases;
        const studentCodes = new Map(students.map(student => [student.studentId, student.studentCode]));
        const rows = cases.map(function (item) {
            const row = document.createElement("tr");
            addCell(row, "#" + item.caseId);
            addCell(row, studentCodes.get(item.studentId) || "Student #" + item.studentId);
            addCell(row, item.category);
            addCell(row, item.priority);
            const statusCell = document.createElement("td");
            statusCell.append(textElement(
                "span",
                String(item.status || "").replaceAll("_", " "),
                "status-badge " + String(item.status || "").toLowerCase().replaceAll("_", "-")
            ));
            row.append(statusCell);
            addCell(row, item.followUpDate);
            return row;
        });
        elements.casesBody.replaceChildren(...rows);
        elements.casesEmpty.hidden = rows.length > 0;
    }

    async function loadResources(type, list, statusElementId) {
        const status = document.getElementById(statusElementId);
        try {
            const response = await requestJson("/api/integrations/resources/" + type + "?pageSize=10");
            const resources = Array.isArray(response.resources) ? response.resources : [];
            const items = resources.map(function (resource) {
                const item = document.createElement("li");
                const link = document.createElement("a");
                link.textContent = resource.title || "Support resource";
                const url = safeHttpUrl(resource.url);
                if (url) {
                    link.href = url;
                    link.target = "_blank";
                    link.rel = "noopener noreferrer";
                }
                item.append(link);
                item.append(textElement("p", resource.description));
                item.append(textElement(
                    "span",
                    (resource.provider || "Provider") + " · " + (resource.source || response.source || "UNKNOWN"),
                    "resource-source"
                ));
                return item;
            });
            list.replaceChildren(...items);
            status.textContent = resources.length
                ? (response.source === "MOCK" ? "Example resources — mock data, not live referrals." : "Available resources.")
                : "No resources are available.";
        } catch (error) {
            list.replaceChildren();
            status.textContent = "Resources are currently unavailable.";
        }
    }

    function safeHttpUrl(value) {
        try {
            const url = new URL(value);
            return url.protocol === "https:" || url.protocol === "http:" ? url.href : null;
        } catch (error) {
            return null;
        }
    }

    elements.classFilter.addEventListener("change", loadCohort);
    elements.termFilter.addEventListener("change", loadCohort);
    document.getElementById("dashboardSearch").addEventListener("input", function () {
        renderAssessments(assessments);
    });
    elements.caseForm.addEventListener("submit", submitCase);
    document.getElementById("cancelCaseButton").addEventListener("click", function () {
        elements.caseFormPanel.hidden = true;
    });
    document.getElementById("refreshCasesButton").addEventListener("click", loadCases);

    loadStudents();
    loadCases();
    loadResources("academic", elements.academicResources, "academicResourceStatus");
    loadResources("wellbeing", elements.wellbeingResources, "wellbeingResourceStatus");
});
