## 📜 API Documentation

The gateway is the single entry point for the IQ Key Value platform. It proxies requests to downstream services after JWT validation, header sanitization, and context propagation.

---

### Routes

All API versioning follows the `/api/v1/{service}/{resource}` path-based convention.

| Route ID           | Upstream Service | Predicate                          | Auth                      | Description                      |
| ------------------ | ---------------- | ---------------------------------- | ------------------------- | -------------------------------- |
| `iam-jwks`         | IAM service      | `Path=/.well-known/**`             | Public                    | JWKS endpoint for JWT validation |
| `iam-api`          | IAM service      | `Path=/api/v1/iam/**`              | Public (configurable)     | All IAM API endpoints            |
| `billing-webhooks` | Billing service  | `Path=/api/v1/billing/webhooks/**` | Public (Stripe signature) | Stripe webhook ingestion         |
| `billing-api`      | Billing service  | `Path=/api/v1/billing/**`          | Protected (JWT required)  | All billing API endpoints        |

Public paths are configured via `iqkv.gateway.public-paths` in `application.yml`.

---

### Downstream Services

| Service         | Base Path            | Default URI             |
| --------------- | -------------------- | ----------------------- |
| IAM service     | `/api/v1/iam/**`     | `http://localhost:8080` |
| Billing service | `/api/v1/billing/**` | `http://localhost:8081` |

---

### Aggregated Swagger UI

The gateway aggregates OpenAPI specs from all downstream services at `/swagger-ui.html`.

| Name            | Spec URL                         |
| --------------- | -------------------------------- |
| IAM Service     | `{IAM_SERVICE_URI}/api-docs`     |
| Billing Service | `{BILLING_SERVICE_URI}/api-docs` |
| Gateway         | `/api-docs`                      |

---

### Downstream Headers

After JWT validation, the gateway injects the following headers on every authenticated request:

| Header               | Source                  | Description                                        |
| -------------------- | ----------------------- | -------------------------------------------------- |
| `X-User-ID`          | JWT `userId` claim      | User identifier                                    |
| `X-Username`         | JWT `username` claim    | Username                                           |
| `X-User-Email`       | JWT `email` claim       | User email address                                 |
| `X-User-Authorities` | JWT `authorities` claim | Comma-separated authority list (e.g. `ADMIN,USER`) |
| `X-Tenant-ID`        | JWT `tenant_id` claim   | Tenant identifier                                  |
| `X-Correlation-ID`   | Generated / propagated  | Request correlation ID for distributed tracing     |

The gateway strips all of these headers from the incoming client request before JWT processing to prevent identity spoofing.

---

### Interactive Documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the gateway is running locally.
