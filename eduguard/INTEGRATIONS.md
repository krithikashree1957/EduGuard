# Stage 6: External integrations

## Architecture and current providers

The isolated `com.eduguard.eduguard.integration` package exposes the
`ResourceIntegrationService` interface and `ResourceIntegrationAdapter`
extension point. Academic and wellbeing adapters normalize provider responses
into `SupportResource` records; HTTP DTOs remain separate. No database,
student, risk, intervention, notification, or dashboard types are used.

Both built-in adapters default to clearly labelled local mock fixtures. No
external provider is connected. The academic fixture links to Khan Academy's
public learning materials; it is not a tutoring referral. The wellbeing fixture
links to NHS mental health information for young people. It is general
information, not counselling, diagnosis, or clinical care, and describes UK
services.

## Endpoints

* `GET /api/integrations/resources/academic`
* `GET /api/integrations/resources/wellbeing`
* `GET /api/integrations/health`

Resource endpoints accept optional `category` (up to 60 characters), `keyword`
(up to 100 characters), and `pageSize` (1–50, default 20). Search is applied
locally to normalized provider results. Valid searches without matches return
an empty `resources` array. Example:

```http
GET /api/integrations/resources/academic?category=learning-resources&pageSize=10
```

Example response:

```json
{
  "integration": "ACADEMIC",
  "source": "MOCK",
  "resources": [
    {
      "resourceId": "mock-academic-1",
      "title": "Khan Academy free learning resources (mock)",
      "description": "Public educational lessons and practice. This is a learning resource, not a tutoring referral.",
      "category": "learning-resources",
      "provider": "Khan Academy",
      "url": "https://www.khanacademy.org/",
      "source": "MOCK"
    }
  ]
}
```

Health reports each adapter's `enabled`, configured `mode`, and `states`.
`MOCK` is present for local fixtures; external mode reports `AVAILABLE` or
`UNAVAILABLE`. Health checks are on-demand and make no startup network calls.
Failures return a safe `error` object with a stable code and no provider
exception text or credentials.

## Configuration

In `application.properties`, configure
`eduguard.integrations.{academic|wellbeing}.enabled` and `.mode` (`mock` or
`external`). Mock mode works offline without credentials. External mode
expects a provider implementing:

* `GET {base-url}/resources?limit=50`, returning a JSON array of records with
  `id`, `title`, `description`, `category`, `provider`, and `url`.
* `GET {base-url}/health`, returning a successful 2xx response when available.

Set provider URLs and optional bearer tokens only through environment
variables:

* `EDUGUARD_ACADEMIC_BASE_URL`, `EDUGUARD_ACADEMIC_API_KEY`
* `EDUGUARD_WELLBEING_BASE_URL`, `EDUGUARD_WELLBEING_API_KEY`

Per-provider connection and read timeouts are configured as
`eduguard.integrations.{academic|wellbeing}.connect-timeout` and
`.read-timeout` (defaults: 2s and 3s). External calls send only a fixed result
limit and optional authorization header. Search terms and student data are
never sent upstream; filtering happens locally. Do not enable external mode
unless the configured provider follows the documented response contract.

## Calling from another Java module

Inject `ResourceIntegrationService` and call
`search(IntegrationType.ACADEMIC, new ResourceQuery(category, keyword, pageSize))`
or use `health()`. Consumers depend on the service and normalized model, not on
provider request/response DTOs.

## Tests

Run from the `eduguard` module:

```powershell
.\mvnw.cmd test
```

Tests use mock HTTP exchanges and local fake clients; no provider, network,
credentials, or database migration is required by the integration tests.
