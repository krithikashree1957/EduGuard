```javascript
// ===== EduGuard Student List =====
// Uses illustrative mock data from students.html.

document.addEventListener("DOMContentLoaded", function () {
    const classFilter = document.getElementById("classFilter");
    const termFilter = document.getElementById("termFilter");
    const studentSearch = document.getElementById("studentSearch");
    const tableBody = document.getElementById("studentTableBody");
    const studentCount = document.getElementById("studentCount");
    const emptyMessage = document.getElementById("emptyMessage");

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

    const rows = Array.from(tableBody.querySelectorAll("tr"));

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

    // Set the correct initial count.
    filterStudents();
});
```