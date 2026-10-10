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
- Retains the explainable weighted heuristic for backwards-compatible fields.
- Trains a lightweight logistic-regression classifier once from bundled
  synthetic examples and returns its probability, tier, fitted-feature
  contributions, and supportive recommendations alongside the heuristic.

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

The frontend calls `/api/students` on the Student Data Service and uses the
configured Risk Service URL (default `http://localhost:8081`).

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

## Risk heuristic and synthetic ML proof of concept

The legacy `riskScore`, `riskTier`, `indicators`, and `contributingFactors`
remain the weighted heuristic response. It is retained for compatibility; it
is distinct from the logistic-regression prediction.

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

The added `modelProbability` estimates the synthetic label
`synthetic_support_need`, not real-world dropout. Training data is bundled at
`risk-recommendation-service/src/main/resources/ml/risk-training-data.csv` and
contains only synthetic rows. Its label is `1` when at least two available
features are below the existing monitoring thresholds (attendance below 75%,
assessment average below 60%, participation below 40%, or assignment
completion below 50%); otherwise it is `0`. The current file has 80 unique
rows, balanced between the two synthetic labels.

The service fits a deterministic Java logistic-regression model once when the
Risk & Recommendation Service starts. It uses full-batch gradient descent with
a fixed learning rate and L2 regularization; no model dependency or external
service is required. Features are the four percentage values
already retrieved from the Student Data Service: attendance, mean assessment
percentage, participation, and assignment completion. No missing values are
imputed: if any feature is unavailable or outside 0–100, model fields are
returned as unavailable while the existing partial-data heuristic response
remains available.

`modelRiskTier` uses `HIGH` at probability >= 0.60, `MEDIUM` at >= 0.35, and
`LOW` below 0.35. `modelContributingFactors` reports each fitted coefficient
and the coefficient-weighted feature difference from the synthetic training
mean in log-odds units. This is a model explanation, not evidence of causation.
Recommendations use positive model contributions to suggest supportive staff
follow-up and continue to require human judgement.

This model is only a proof of concept trained on a small synthetic dataset. It
has not been validated on real student outcomes and its probabilities must not
be interpreted as real dropout probabilities, causal effects, or grounds for
disciplinary, exclusion, or other high-impact decisions. No real-world
accuracy claim is made. No held-out metrics are reported: the rows are
synthetic and their labels are generated directly from the threshold rule, so
a holdout score would measure reproduction of that artificial rule rather
than validated outcome prediction.

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

Earlier baseline builds and context-only tests were run before the Stage 8
ML changes. That historical result does not verify the current source tree.
The Risk Service suite now includes context, heuristic calculation,
recommendation, model training/prediction, and mocked inter-service HTTP
tests. No browser-automation test framework is configured. Run the test
commands above before release.

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

- The student page displays the student code in both the “Student ID” and “Student” columns because no student-name field currently exists.
- Browser-level dashboard behavior is not covered by an automated frontend test framework.

## Potential issues

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
- Real-world validated dropout prediction, training data, or model evaluation.
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

## Stage 4 intervention case management

Stage 4 adds an independent intervention-case module to the Student Data Service. It stores only a student ID and does not copy student personal information.

### Case endpoints

```http
POST  /api/interventions
GET   /api/interventions
GET   /api/interventions/{caseId}
GET   /api/interventions/student/{studentId}
PATCH /api/interventions/{caseId}
GET   /api/interventions/{caseId}/activities
GET   /api/interventions/follow-ups/due
GET   /api/interventions/follow-ups/overdue
```

Case searches support pagination and filtering by student, status, priority, category, and assigned staff identifier. Results use stable `updatedAt` and `caseId` ordering. Case updates enforce the permitted status transitions and require a resolution summary before closing. Case activity history is append-only through the API and is returned read-only.

The Stage 4 migration is:

```text
database/migrations/V3__intervention_case_management.sql
```

No automatic migration framework is configured. Review and apply V2 before V3, manually and in a controlled environment; neither migration has been applied to `eduguard_db` by this implementation. The application remains configured with `spring.jpa.hibernate.ddl-auto=validate`.

Stage 4 does not call the risk service, create cases automatically from risk scores, send notifications, or modify the dashboard. Authentication and staff authorization are not enforced by the current application.

## Stage 5 notification management

Stage 5 adds an independent notification module under the Student Data Service. It supports persistent `EMAIL` and `IN_APP` notification requests, reusable allow-listed templates, idempotency keys, delivery status, safe delivery-attempt history, and in-app read tracking.

### Notification endpoints

```http
POST  /api/notifications
GET   /api/notifications?recipientReference=...
GET   /api/notifications/{notificationId}
PATCH /api/notifications/{notificationId}/read
```

The request accepts an opaque recipient reference, event type, channel, supported template ID, bounded string parameters, and an optional idempotency key. Repeating an idempotency key returns the existing notification instead of creating another record. Template parameters are substituted as plain text only; arbitrary template execution and HTML rendering are not supported.

Email delivery defaults to safe mock mode:

```properties
eduguard.notifications.email-mode=mock
```

Mock email results are marked `MOCK_SIMULATED`, not as real SMTP delivery. Real SMTP mode requires environment-provided `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, and `SMTP_SENDER` values and must be enabled explicitly with `EDUGUARD_NOTIFICATION_EMAIL_MODE=smtp`. Automated tests do not send email.

The Stage 5 migration is:

```text
database/migrations/V4__notification_management.sql
```

Review and apply V2, V3, and V4 manually and in order in a controlled environment; no migration was applied to `eduguard_db`. Stage 5 does not modify the dashboard, call the risk service, depend on Stage 4 classes, or send automatic reminders. Authentication and recipient ownership are not enforced by the current application, so a supplied recipient reference does not prove access authorization.

## Stage 8 security, tests, and integration audit

### Services and runtime configuration

The Student Data Service runs on port `8080` and requires PostgreSQL. Set
`DB_PASSWORD` in the process environment; the repository does not provide a
database-password fallback. Its default URL is
`jdbc:postgresql://localhost:5432/eduguard_db`, and its current configured
database username is `postgres`. Hibernate schema validation is enabled for
normal application startup.

Start the Student Data Service before the Risk & Recommendation Service. The
risk service runs on port `8081` and fetches
`/api/students/{studentId}/performance` over HTTP. Configure its downstream
base URL with `EDUGUARD_STUDENT_SERVICE_BASE_URL`; the legacy
`student.service.base-url` property remains a fallback. Configure the URL
published to both frontend pages with `EDUGUARD_RISK_SERVICE_BASE_URL`.

Notification email delivery defaults to `mock`, and academic/wellbeing
integrations default to `mock`. Automated tests use mock delivery and mock
provider clients; they do not send email or contact real providers. External
provider URLs and API keys are configured with `EDUGUARD_ACADEMIC_BASE_URL`,
`EDUGUARD_ACADEMIC_API_KEY`, `EDUGUARD_WELLBEING_BASE_URL`, and
`EDUGUARD_WELLBEING_API_KEY`. See [INTEGRATIONS.md](./eduguard/INTEGRATIONS.md)
for provider contracts and per-provider timeouts. The optional
`EDUGUARD_NOTIFICATION_MAX_ATTEMPTS` setting defaults to `3`; SMTP credentials
are required only when explicitly switching email delivery to SMTP mode, as
documented in Stage 5 above.

### Database setup

Review `database/eduguard_setup.sql` for the base student/performance schema
and fictional sample data. Review and apply V2, V3, then V4 manually, only
after confirming the target database and migration contents. The application
does not run migrations automatically; tests and documentation in this
repository did not apply migrations or alter database records.

### Test execution and current coverage

Run each Maven project independently:

```powershell
cd D:\Eduguard\eduguard
.\mvnw.cmd -B test

cd D:\Eduguard\risk-recommendation-service
.\mvnw.cmd -B test
```

The Student Data Service context-load test requires a working PostgreSQL
connection even though it disables Hibernate schema validation for that test.
The risk service has no database dependency. Controller/service tests use
mocks, and external-integration tests use local mock clients.

The current test coverage includes student/performance response contracts and
missing-data behavior, risk calculation and student-service HTTP handling,
support validation and duplicate barriers, intervention creation/status and
follow-up behavior, notification mock delivery/idempotency, external-resource
query/health/failure behavior, and dashboard page routing. There is no
configured frontend/browser automation framework. Database-backed workflows
and full browser-to-database end-to-end runs remain dependent on a configured
PostgreSQL instance and manually applied migrations.

### Security limitations

No authentication or authorization is implemented. Student performance,
support, intervention, notification, and risk endpoints do not verify an
authenticated user or record ownership. In particular, notification reads
accept a recipient reference without proving recipient ownership. Do not
expose these services to untrusted networks or use them with real student
records until identity integration and server-side role/record authorization
are implemented and verified. The risk-service CORS allow-list is not an
authorization control.
