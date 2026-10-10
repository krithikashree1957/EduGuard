# Stage 7: Dashboard integration

The dashboard now brings together the services and APIs available on `main`:

* `GET /api/students` and its `className` / `term` filters provide cohort data.
* `GET {risk-service-base-url}/api/risk/students/{studentId}` supplies live
  risk tiers, contributing factors, and suggested actions.
* `GET /api/interventions`, `GET /api/interventions/follow-ups/overdue`, and
  `POST /api/interventions` provide case tracking and educator-initiated case
  creation.
* `GET /api/integrations/resources/academic` and
  `GET /api/integrations/resources/wellbeing` provide resource lists.

The page filters by class and term and calculates cohort risk counts from the
returned assessments. Risk service failures are displayed as unavailable
assessments rather than as a risk tier. Search filters the displayed cohort by
student code. Student identifiers and resource descriptions are inserted as
text, not interpreted as HTML.

Cases are created only after an educator explicitly submits the case form.
The request includes the selected student ID and the Stage 4 case fields; no
case is automatically created from a risk score. Stage 5 notification APIs
were not present in the checked-out `main` when this dashboard was integrated,
so the page does not show notifications or attempt delivery.

## Configuration and prerequisites

Configure `eduguard.risk-service.base-url` or the
`EDUGUARD_RISK_SERVICE_BASE_URL` environment variable for the risk service URL.
The default is `http://localhost:8081`. The risk service must allow browser
requests from the Student Data Service origin.

Run both Spring Boot services and configure PostgreSQL as documented in the
root README. The Stage 4 schema migration must be applied manually before case
operations work:

1. Review and apply `database/migrations/V2__student_support_data.sql`.
2. Review and apply `database/migrations/V3__intervention_case_management.sql`.

The application keeps Hibernate schema validation enabled. If the intervention
migration is not applied, the dashboard still handles case API failures
separately, but cases cannot be loaded or saved. Academic and wellbeing
resources may be mock results; the UI labels these as examples and does not
present them as live referrals.

No notification delivery, authentication, authorization, or new database
schema is introduced by this dashboard integration.
