# EduGuard
An enterprise student dropout risk and intervention management platform that identifies at-risk students using student performance, attendance, fee, and engagement data, with explainable risk scoring, targeted interventions, and cohort analytics using Java, Spring Boot, microservices, Thymeleaf, and PostgreSQL.

## Verified baseline

EduGuard currently consists of two independent Spring Boot Maven services.

### Student Data Service

Location: `D:\Eduguard\eduguard`

The Student Data Service:

- Serves the Thymeleaf dashboard and student page.
- Provides student and performance REST APIs.
- Reads student data with Spring Data JPA.
- Connects to PostgreSQL.

It uses the default Spring Boot port `8080`.

### Risk & Recommendation Service

Location: `D:\Eduguard\risk-recommendation-service`

The Risk & Recommendation Service:

- Provides student risk assessments.
- Calls the Student Data Service over HTTP.
- Calculates an explainable weighted heuristic.
- Returns a risk score, risk tier, contributing factors, and recommendations.

It uses port `8081` and does not directly access PostgreSQL.

### Frontend

The Student Data Service serves the Thymeleaf frontend:

- Dashboard: `http://localhost:8080/`
- Student risk list: `http://localhost:8080/students`

Frontend files are under `eduguard/src/main/resources`, including:

- `templates/dashboard.html`
- `templates/student.html`
- `static/js/dashboard.js`
- `static/js/student.js`
- `static/css/dashboard.css`

The frontend calls `/api/students` on the Student Data Service and calls the Risk Service at `http://localhost:8081`.

### Database

The Student Data Service uses PostgreSQL with this database URL:

```text
jdbc:postgresql://localhost:5432/eduguard_db
```

The application uses:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

The database setup script is `database/eduguard_setup.sql`. It defines the `students`, `attendance`, `assessments`, and `engagement` tables and contains fictional local-development sample data. The script should not be imported automatically without confirming the target database.

## Existing REST APIs

### List students

```http
GET /api/students
```

Optional query parameters:

- `className`
- `term`

Examples:

```text
GET http://localhost:8080/api/students
GET http://localhost:8080/api/students?className=CSE-A
GET http://localhost:8080/api/students?term=2026-T1
GET http://localhost:8080/api/students?className=CSE-A&term=2026-T1
```

Implemented by `StudentController`, which calls `StudentRepository`.

Runtime verification returned HTTP `200 OK` and 10 students with IDs 1 through 10.

### Student performance

```http
GET /api/students/{studentId}/performance
```

Example:

```text
GET http://localhost:8080/api/students/1/performance
```

Implemented by `StudentPerformanceController`. It reads student details, attendance, assessments, and engagement using the corresponding Spring Data repositories.

Runtime verification for student ID 1 returned HTTP `200 OK` with attendance, assessment, and engagement data. An unknown student returned HTTP `404 Not Found`.

### Risk assessment

```http
GET /api/risk/students/{studentId}
```

Example:

```text
GET http://localhost:8081/api/risk/students/1
```

Implemented by `RiskAssessmentController`. It first calls:

```http
GET http://localhost:8080/api/students/{studentId}/performance
```

Runtime verification for student ID 1 returned HTTP `200 OK`, a risk score of `12.4`, the `LOW` tier, contributing factors, and a recommendation. The risk service successfully obtained data from the Student Data Service. An unknown student returned HTTP `404 Not Found`.

## Risk heuristic

The current risk implementation is an explainable weighted heuristic. It is **not** a trained machine-learning model.

| Indicator | Weight |
|---|---:|
| Attendance | 40% |
| Assessment average | 30% |
| Participation | 15% |
| Assignment completion | 15% |

Values are normalized and constrained to 0–100. If some data is unavailable, the calculation uses the available weighted inputs.

| Risk score | Tier |
|---:|---|
| 60 or higher | `HIGH` |
| 35 to below 60 | `MEDIUM` |
| Below 35 | `LOW` |

The service describes the result as supporting human review and not being a definitive prediction.

## Build and test

The services are independent Maven projects. Run commands from each service directory.

### Student Data Service

```powershell
cd D:\Eduguard\eduguard
.\mvnw.cmd -B -DskipTests package
.\mvnw.cmd -B test
```

### Risk & Recommendation Service

```powershell
cd D:\Eduguard\risk-recommendation-service
.\mvnw.cmd -B -DskipTests package
.\mvnw.cmd -B test
```

Both package builds completed with `BUILD SUCCESS` and exit code `0`.

Both existing test suites passed:

- Student Data Service: 1 test, 0 failures, 0 errors, 0 skipped.
- Risk & Recommendation Service: 1 test, 0 failures, 0 errors, 0 skipped.

The current tests are Spring context-load tests. They do not cover all API contracts, risk calculations, service communication, or browser behavior.

### Start the services manually

Start the Student Data Service first because the Risk Service calls it:

```powershell
cd D:\Eduguard\eduguard
.\mvnw.cmd spring-boot:run
```

In a second terminal:

```powershell
cd D:\Eduguard\risk-recommendation-service
.\mvnw.cmd spring-boot:run
```

## Verification evidence

The verified baseline established the following:

- PostgreSQL authentication and database access succeeded during the Student Data Service context test.
- Hibernate initialized successfully and schema validation reported no errors.
- Both application ports were reachable during runtime verification.
- `GET /api/students` returned HTTP `200`.
- The performance endpoint returned HTTP `200` for a student ID obtained from the real student-list response.
- The risk endpoint returned HTTP `200`.
- The Risk Service successfully called the Student Data Service.
- The dashboard loaded and displayed 10 live student rows with risk results.
- The student page loaded and its risk-assessment interaction worked.
- Unknown student IDs returned HTTP `404`.

## Confirmed issues

- The `/interventions` navigation link returns HTTP `404 Not Found`.
- The student page displays the student code in both the “Student ID” and “Student” columns because no student-name field currently exists.
- Automated tests are currently limited to application-context loading.

## Potential issues

- The Risk Service uses a hard-coded downstream URL: `http://localhost:8080`.
- The performance response changes shape when attendance or engagement data is missing.
- Attendance and engagement repositories return `Optional`, but the SQL schema does not enforce one record per student.
- No authentication or authorization implementation was found.
- No visible timeout, retry, circuit breaker, or resilience policy exists for the inter-service call.
- The dashboard makes one risk request per student, which may not scale efficiently for large cohorts.
- Controller input validation is limited.

## Existing functionality and future functionality

### Existing and verified

- Student listing and filtering by class and term.
- Student performance retrieval.
- Attendance, assessment, and engagement data retrieval.
- Explainable heuristic risk assessment.
- Risk-tier and recommendation display.
- Thymeleaf dashboard and student risk list.
- Internal REST communication between the services.
- PostgreSQL-backed student data.

### Not implemented or not verified

The following should not currently be described as existing features:

- Role-based authorization.
- Intervention creation and tracking.
- Recipient-specific notifications.
- Email, SMS, or other external integrations.
- Trained machine-learning prediction, model training, or model evaluation.
- Production deployment.
- Complete analytics and cohort reporting.
- Audit history.
- User accounts or educator identity management.

## Baseline limitations

This is a local verified baseline. It does not establish production readiness, security readiness, scalability, prediction accuracy, or complete intervention-management functionality. Behavior with large datasets, service failure, and duplicate performance records remains unverified.

## Stage 2 support data extension

Stage 2 adds a separate backend model for contextual student-support information without changing the existing student, performance, or risk APIs.

### Support records

The Student Data Service now models three separate historical record types:

- `SupportBarrier`: student-reported educational barriers such as commuting difficulties or limited study resources.
- `FinancialSupport`: minimal financial-support request information such as support type and workflow status.
- `WellbeingRequest`: student-initiated requests for a wellbeing-related check-in or assistance.

These records use foreign keys to `students`, timestamps, bounded text fields, and status/severity checks. Sensitive support records are not included in the existing student-list or performance responses. The current application has no authentication or role-based authorization, so these endpoints are not yet access-controlled.

### Stage 2 endpoints

```http
GET  /api/students/{studentId}/support/barriers
POST /api/students/{studentId}/support/barriers

GET  /api/students/{studentId}/support/financial
POST /api/students/{studentId}/support/financial

GET  /api/students/{studentId}/support/wellbeing
POST /api/students/{studentId}/support/wellbeing
```

POST requests validate required fields and allowed status/severity values. Unknown students return `404 Not Found`, invalid request bodies return `400 Bad Request`, and duplicate active barriers of the same type return `409 Conflict`.

The existing weighted risk heuristic was not changed in Stage 2. Support data is contextual information for later human-reviewed workflows; it is not automatically treated as proof of dropout risk.

### Database migration status

The review-only migration script is:

```text
database/migrations/V2__student_support_data.sql
```

No migration framework is currently configured in the project. The script is prepared for review but has **not** been applied to `eduguard_db`. No live records or tables were modified.

Before applying it, review the foreign keys, indexes, status checks, retention requirements, and authorization plan. The existing application context test uses `spring.jpa.hibernate.ddl-auto=none` for the test run because the new migration is intentionally unapplied; this test does not validate the new tables against the live database.

### Stage 2 verification

The Student Data Service completed:

```text
Tests run: 5
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

This includes the existing context test and focused controller tests for successful creation, validation failures, and separate support collections. The Risk & Recommendation Service's existing context test also passed:

```text
Tests run: 1
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

The new support endpoints were not runtime-tested against the live application because doing so would require applying the approved migration first, and the running services were not restarted.
