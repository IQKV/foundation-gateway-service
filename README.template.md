# Foundation Gateway Service 🌐

Reactive API gateway — the single entry point for all client traffic in the IQKV platform. Handles JWT validation, header sanitization, user/tenant context propagation, platform mode consistency enforcement, and response security hardening.

## About

The Gateway service owns all cross-cutting concerns so downstream services receive a clean, enriched request context:

- **Authentication enforcement** — validates RS256 JWTs issued by the IAM service; public paths bypass auth via `iqkv.gateway.public-paths`
- **Identity propagation** — extracts user/tenant claims from the validated JWT and forwards them as trusted headers to downstream services; no downstream service needs to re-parse the token
- **Security hardening** — strips spoofable context headers (`X-User-*`, `X-Tenant-ID`, `X-Organization-ID`) from every inbound request before JWT processing; adds security response headers on every reply
- **Platform mode consistency** — queries IAM's `/actuator/info` on startup and every 60 s to verify `ROLLOUT_MODE` matches; blocks all traffic with `503` if a mismatch is detected
- **Single-tenant auto-injection** — in `SINGLE_TENANT` mode, injects `X-Tenant-ID` from the configured default tenant key for requests that carry no tenant context
- **Correlation tracking** — generates or propagates `X-Correlation-ID` on every request; echoes it back on the response
- **Aggregated Swagger UI** — proxies OpenAPI specs from IAM and Billing into a single UI at `/swagger-ui.html`

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## Filter Chain

Filters execute in order. Lower numbers run first on the request path, last on the response path.

| Order     | Filter                         | Responsibility                                                                         |
| --------- | ------------------------------ | -------------------------------------------------------------------------------------- |
| `HIGHEST` | `PlatformModeGuardFilter`      | Block all traffic with `503` if rollout mode mismatches IAM                            |
| `-200`    | `CorrelationIdFilter`          | Generate or propagate `X-Correlation-ID`                                               |
| `-190`    | `HeaderSanitizationFilter`     | Strip spoofable context headers (`X-User-*`, `X-Tenant-ID`, etc.) from client requests |
| `-100`    | `JwtContextPropagationFilter`  | Extract JWT claims; set downstream headers                                             |
| `-50`     | `TenantContextFilter`          | In `SINGLE_TENANT` mode, inject `X-Tenant-ID` when absent                              |
| `MIN+1`   | `ResponseTransformationFilter` | Add security response headers; echo `X-Correlation-ID` to client                       |

Spring Security OAuth2 Resource Server handles JWT signature validation (RS256 via JWKS) before the `JwtContextPropagationFilter` runs.

## Downstream Headers

After the filter chain, every authenticated request to a downstream service carries:

| Header               | Source                                                       | Description                    |
| -------------------- | ------------------------------------------------------------ | ------------------------------ |
| `X-User-ID`          | JWT `userId` claim                                           | User UUID                      |
| `X-Username`         | JWT `username` claim                                         | Username                       |
| `X-User-Email`       | JWT `email` claim                                            | User email                     |
| `X-User-Authorities` | JWT `authorities` claim                                      | Comma-separated authority list |
| `X-Tenant-ID`        | JWT `tenant_id` claim (or default key in single-tenant mode) | Tenant key                     |
| `X-Correlation-ID`   | Generated / propagated                                       | Request trace ID               |

> The gateway strips all of these headers from the inbound client request before JWT processing. Only the gateway sets them — downstream services can trust them unconditionally.

## Routes

| Route ID           | Upstream              | Predicate                          | Auth                                                        |
| ------------------ | --------------------- | ---------------------------------- | ----------------------------------------------------------- |
| `iam-jwks`         | `IAM_SERVICE_URI`     | `Path=/.well-known/**`             | Public                                                      |
| `iam-api`          | `IAM_SERVICE_URI`     | `Path=/api/v1/iam/**`              | Public paths via `iqkv.gateway.public-paths`; otherwise JWT |
| `billing-webhooks` | `BILLING_SERVICE_URI` | `Path=/api/v1/billing/webhooks/**` | Public (Stripe signature)                                   |
| `billing-api`      | `BILLING_SERVICE_URI` | `Path=/api/v1/billing/**`          | JWT                                                         |

### Public paths (no JWT required)

```
/api/v1/iam/**               # All IAM endpoints (auth, signup, password reset, etc.)
/.well-known/**              # JWKS endpoint
/api/v1/billing/webhooks/**  # Stripe webhook receiver
/actuator/**
/swagger-ui/**
/api-docs/**
```

> Update `iqkv.gateway.public-paths` in `application.yml` to tighten or expand the public surface as your platform matures.

## Prerequisites

- Java 25 (Eclipse Temurin)
- Maven 3.9+
- Node.js >= 22.15.0 & pnpm >= 10.33.0 (git hooks)
- Docker & Docker Compose

## Quick Start

```bash
# Clone the repository
git clone https://github.com/IQKV/foundation-gateway-service.git
cd foundation-gateway-service

# Install git hooks
pnpm install

# Start local dev infrastructure
docker compose up -d

# Run the application (requires IAM service on localhost:8080)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Pdev
# → Gateway:  http://localhost:8080
# → Actuator: http://localhost:8081/actuator/health
# → Swagger:  http://localhost:8080/swagger-ui.html
```

## Environment Variables

| Variable               | Default                                       | Description                                                                   |
| ---------------------- | --------------------------------------------- | ----------------------------------------------------------------------------- |
| `ROLLOUT_MODE`         | `MULTI_TENANT`                                | Platform mode: `MULTI_TENANT` or `SINGLE_TENANT` — must match IAM and Billing |
| `SERVER_PORT`          | `8080`                                        | Gateway API port                                                              |
| `MANAGEMENT_PORT`      | `8081`                                        | Actuator / management port                                                    |
| `IAM_SERVICE_URI`      | `http://localhost:8080`                       | IAM service base URI (for routing)                                            |
| `IAM_SERVICE_URL`      | `http://foundation-iam-service:8080`          | IAM service URL for platform mode guard check                                 |
| `IAM_JWKS_URI`         | `http://localhost:8080/.well-known/jwks.json` | JWK Set URI for JWT validation                                                |
| `BILLING_SERVICE_URI`  | `http://localhost:8081`                       | Billing service base URI (for routing)                                        |
| `CORS_ALLOWED_ORIGINS` | `*`                                           | Allowed CORS origin patterns                                                  |
| `DEFAULT_TENANT_KEY`   | _(empty)_                                     | Default tenant key injected in `SINGLE_TENANT` mode                           |

> Copy `.env.example` to `.env.local` / `.env.uat` / `.env.prd` and fill in values per environment.

## Maven Commands

```bash
# Build and test (skip Checkstyle during development)
./mvnw clean verify -Dcheckstyle.skip=true

# Run tests only
./mvnw test -Dcheckstyle.skip=true

# Explicit Checkstyle check
./mvnw checkstyle:check

# Coverage report → target/site/jacoco/index.html
./mvnw jacoco:report

# Production build
./mvnw clean package -Pproduction
```

## Docker

```bash
# Build image
docker build -t iqkv/foundation-gateway-service:latest .

# Run with full platform stack
docker compose -f compose.container.yaml up -d
```

## Monitoring

| Endpoint                   | Description                                                      |
| -------------------------- | ---------------------------------------------------------------- |
| `GET /actuator/health`     | Liveness + readiness probes; `REFUSING_TRAFFIC` on mode mismatch |
| `GET /actuator/metrics`    | Application metrics                                              |
| `GET /actuator/prometheus` | Prometheus scrape endpoint                                       |
| `GET /swagger-ui.html`     | Aggregated API docs (IAM + Billing + Gateway)                    |
| `GET /api-docs`            | Gateway OpenAPI spec                                             |

## Project Structure

```
src/main/java/com/iqkv/foundation/gatewayservice/
├── infrastructure/
│   ├── config/
│   │   ├── GatewayProperties.java            # @ConfigurationProperties — public paths, tenancy, IAM URL
│   │   ├── SecurityConfig.java               # WebFlux security, JWT converter, public path matcher
│   │   ├── PlatformModeGuardFilter.java      # Startup + periodic rollout mode consistency check
│   │   └── PlatformConfigurationProperties.java  # rollout-mode binding
│   └── security/
│       ├── CorrelationIdFilter.java          # Generate/propagate X-Correlation-ID (order -200)
│       ├── HeaderSanitizationFilter.java     # Strip spoofable headers (order -190)
│       ├── JwtContextPropagationFilter.java  # Enrich downstream headers from JWT (order -100)
│       ├── TenantContextFilter.java          # Auto-inject X-Tenant-ID in single-tenant mode (order -50)
│       └── ResponseTransformationFilter.java # Security headers + correlation echo (order MIN+1)
└── shared/
    └── exception/                            # Common exception types, global error handler
```

## License

This project is licensed under the Apache License. See the [LICENSE](LICENSE) file for details.

## Contributing

Please read our [Contributing Guidelines](.github/CONTRIBUTING.md) and [Code of Conduct](.github/CODE_OF_CONDUCT.md).

---

## 🧩 Boilerplate Architecture

- **Rendering model**: Reactive — Spring Cloud Gateway + WebFlux, non-blocking I/O end-to-end
- **Security**: Spring Security OAuth2 Resource Server; JWT RS256 validated via JWKS URI (IAM service); public path list in `GatewayProperties`; all context headers stripped from inbound requests before JWT processing
- **Filter chain**: Six ordered `GlobalFilter` beans — platform guard → correlation ID → header sanitization → JWT propagation → tenant context resolution → response hardening
- **Platform rollout mode**: Controlled via `ROLLOUT_MODE` (`MULTI_TENANT` | `SINGLE_TENANT`); must be identical across IAM, Billing, and Gateway; `PlatformModeGuardFilter` queries IAM `/actuator/info` on startup and every 60 s, comparing `platform.rollout-mode`; readiness set to `REFUSING_TRAFFIC` on mismatch; fail-open on transient IAM unavailability
- **Single-tenant mode**: `TenantContextFilter` auto-injects `X-Tenant-ID` from `DEFAULT_TENANT_KEY` when no tenant context is present (covers unauthenticated sign-in and pre-auth requests); multi-tenant mode preserves existing tenant-selection flow from JWT
- **Aggregated Swagger UI**: springdoc-openapi webflux variant; proxies `/api-docs` from IAM and Billing into a single UI at `/swagger-ui.html`
- **Observability**: Micrometer + Prometheus; structured JSON logging with MDC; health probes for Kubernetes readiness/liveness
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo (80% gate), ArchUnit, commit convention enforcement

> See [AGENTS.md](AGENTS.md) for repository structure, DDD patterns, and agent guidelines.
