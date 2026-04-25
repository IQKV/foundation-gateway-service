# Project Name 🌐

<!-- TEMPLATE: This README.template.md is a starter template. Copy parts into your real README.md and replace placeholders. -->

<details>
  <summary><strong>How to use this template (click to expand)</strong></summary>

1. Rename the title above to your service name and optionally add a logo right below it.
2. Add badges (build, coverage, license) under the title.
3. Fill each section below with your actual service content (keep the section order if you like it).
4. Replace placeholder filter tables, route tables, and environment variable tables with real values.
5. Update the downstream headers table to reflect the claims your JWT actually carries.
6. Keep the "Documentation" links if you want quick access to docs, or remove them in your final README.md.
7. Remove this guidance block after you finish customizing.

</details>

- Add your service logo.
- Write a short introduction — what the gateway does and which platform it belongs to.
- If you are using badges, add them here.

<details>
  <summary><strong>Badge examples (optional)</strong></summary>

- Build: <code>![CI](https://img.shields.io/github/actions/workflow/status/ORG/REPO/build-nodejs-project.yml?label=CI)</code>
- Coverage: <code>![Coverage](https://img.shields.io/badge/coverage-80%25-brightgreen)</code>
- License: <code>![License](https://img.shields.io/github/license/ORG/REPO)</code>
- Java: <code>![Java](https://img.shields.io/badge/java-21-blue)</code>
- Spring Boot: <code>![Spring Boot](https://img.shields.io/badge/spring--boot-3.x-brightgreen)</code>

</details>

## :beginner: About

Add a detailed introduction about the service here — what it does, which microservices it fronts, and what cross-cutting concerns it owns.

## 📚 Documentation

- [API Documentation](docs/api/README.md)
- [Architecture Overview](docs/architecture/README.md)
- [Deployment Guide](docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## Key Features

- **Reactive Gateway**: Spring Cloud Gateway with WebFlux — non-blocking I/O throughout the filter chain
- **JWT Authentication**: RS256 validation via JWK Set URI; public paths bypass auth via `iqkv.gateway.public-paths`
- **Header Sanitization**: Strips spoofable context headers from incoming requests before JWT processing
- **Context Propagation**: Extracts user/tenant claims from the validated JWT and forwards them as typed headers
- **Correlation Tracking**: Generates or propagates `X-Correlation-ID` on every request
- **Response Security Headers**: Injects security headers on all responses
- **Observability**: Prometheus metrics, health probes, structured JSON logging
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo coverage gate, ArchUnit, oxfmt, Husky git hooks

## Prerequisites

- Java 25 (Eclipse Temurin)
- Maven 3.9+
- Node.js >= 22.15.0 & pnpm >= 10.33.0
- Docker & Docker Compose

## Quick Start

```bash
# Clone the repository
git clone https://github.com/ORG/REPO.git

# Navigate to project directory
cd REPO

# Install git hooks
pnpm install

# Start local dev infrastructure
docker compose up -d

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=local -P dev
# → Gateway API:  http://localhost:8080
# → Actuator:     http://localhost:8081/actuator/health
# → Swagger UI:   http://localhost:8080/swagger-ui.html
```

## Filter Chain

Filters execute in order. Each `GlobalFilter` is `Ordered` — lower numbers run first on the request path, last on the response path.

| Order   | Filter                         | Responsibility                                                         |
| ------- | ------------------------------ | ---------------------------------------------------------------------- |
| `-200`  | `CorrelationIdFilter`          | Generate or propagate `X-Correlation-ID`; store in exchange attributes |
| `-190`  | `HeaderSanitizationFilter`     | Strip spoofable context headers from incoming requests                 |
| `-100`  | `JwtContextPropagationFilter`  | Extract user/tenant claims from validated JWT; set downstream headers  |
| `MIN+1` | `ResponseTransformationFilter` | Add security response headers; echo `X-Correlation-ID` to client       |

> Add or remove rows to match your actual filter chain. Document the `getOrder()` value for each filter.

## Downstream Headers

After the filter chain, downstream services receive the following headers on every authenticated request:

| Header               | Source                  | Description                    |
| -------------------- | ----------------------- | ------------------------------ |
| `X-User-ID`          | JWT `userId` claim      | User identifier                |
| `X-Username`         | JWT `username` claim    | Username                       |
| `X-User-Email`       | JWT `email` claim       | User email address             |
| `X-User-Authorities` | JWT `authorities` claim | Comma-separated authority list |
| `X-Tenant-ID`        | JWT `tenant_id` claim   | Tenant identifier              |
| `X-Correlation-ID`   | Generated / propagated  | Request correlation ID         |

> Update this table to match the claims in your JWT and the headers your `JwtContextPropagationFilter` actually sets.

**Security note**: The gateway strips all context headers from the incoming client request before JWT processing. Only the gateway sets them after successful validation.

## Consuming Gateway Context

Downstream services can read the enriched headers directly from the request:

```java
@GetMapping("/protected")
public ResponseEntity<?> protectedEndpoint(
    @RequestHeader("X-User-ID") Long userId,
    @RequestHeader("X-Username") String username,
    @RequestHeader("X-User-Email") String email,
    @RequestHeader("X-User-Authorities") String authorities,
    @RequestHeader("X-Tenant-ID") String tenantId,
    @RequestHeader("X-Correlation-ID") String correlationId
) {
    List<String> authorityList = Arrays.asList(authorities.split(","));
    // ...
}
```

Services that need to validate JWTs independently can point to the same JWKS endpoint:

```yaml
spring:
    security:
        oauth2:
            resourceserver:
                jwt:
                    jwk-set-uri: http://your-iam-service:8080/.well-known/jwks.json
```

## Routes

Routes are defined in `application.yml` under `spring.cloud.gateway.routes`:

| Route ID           | URI             | Predicate                          | Auth      |
| ------------------ | --------------- | ---------------------------------- | --------- |
| `service-a`        | `SERVICE_A_URI` | `Path=/api/v1/service-a/**`        | Protected |
| `service-b-public` | `SERVICE_B_URI` | `Path=/api/v1/service-b/public/**` | Public    |

> Replace with your actual routes. Public path patterns are configured via `iqkv.gateway.public-paths`.

## Environment Variables

| Variable               | Default                                       | Description                    |
| ---------------------- | --------------------------------------------- | ------------------------------ |
| `SERVER_PORT`          | `8080`                                        | Gateway API port               |
| `MANAGEMENT_PORT`      | `8081`                                        | Actuator / management port     |
| `IAM_SERVICE_URI`      | `http://localhost:8080`                       | Base URI of the IAM service    |
| `IAM_JWKS_URI`         | `http://localhost:8080/.well-known/jwks.json` | JWK Set URI for JWT validation |
| `CORS_ALLOWED_ORIGINS` | `*`                                           | Allowed CORS origin patterns   |

> Add rows for each downstream service URI and any other environment-specific variables.

## Maven Commands

```bash
# Build and run all tests (skip Checkstyle during development)
mvn clean verify -Dcheckstyle.skip=true

# Run tests only
mvn test -Dcheckstyle.skip=true

# Explicit Checkstyle check
mvn checkstyle:check

# Coverage report → target/site/jacoco/index.html
mvn jacoco:report

# Production build
mvn clean package -P production
```

## Docker

```bash
# Build image
docker build -t ORG/REPO:latest .

# Run with full platform stack
docker compose -f compose.container.yaml up -d
```

## Monitoring

| Endpoint                   | Description                  |
| -------------------------- | ---------------------------- |
| `GET /actuator/health`     | Liveness + readiness probes  |
| `GET /actuator/metrics`    | Application metrics          |
| `GET /actuator/prometheus` | Prometheus scrape endpoint   |
| `GET /swagger-ui.html`     | Aggregated API documentation |
| `GET /api-docs`            | Gateway OpenAPI spec         |

## Project Structure

```
src/main/java/com/example/gatewayservice/
├── infrastructure/
│   ├── config/
│   │   ├── GatewayProperties.java      # @ConfigurationProperties for gateway.*
│   │   └── SecurityConfig.java         # WebFlux security, JWT converter, public paths
│   └── security/
│       ├── CorrelationIdFilter.java     # Generate/propagate X-Correlation-ID
│       ├── HeaderSanitizationFilter.java # Strip spoofable headers
│       ├── JwtContextPropagationFilter.java # Enrich downstream headers
│       └── ResponseTransformationFilter.java # Security headers + correlation echo
└── shared/
    ├── exception/                       # Common exception types
    └── util/                            # Utility classes
```

---

<details>
  <summary><strong>✅ Pre-publish checklist (remove in final README)</strong></summary>

- [ ] Title updated and logo added
- [ ] Badges added (CI, coverage, license, Java, Spring Boot)
- [ ] About section completed
- [ ] Filter chain table reflects actual filters and their `getOrder()` values
- [ ] Downstream headers table matches JWT claims and propagation filter
- [ ] Routes table reflects actual `application.yml` routes
- [ ] Environment variables table is complete
- [ ] Project structure tree updated if packages differ
- [ ] Links verified (docs, external resources)
- [ ] Guidance blocks removed before publishing

</details>

---

## 🧩 Boilerplate Architecture

- **Rendering model**: Reactive — Spring Cloud Gateway + WebFlux, non-blocking I/O end-to-end
- **Security**: Spring Security OAuth2 Resource Server; JWT RS256 via JWKS; public path list in `GatewayProperties`
- **Filter chain**: Ordered `GlobalFilter` beans — sanitization → JWT propagation → tenant context resolution → response hardening
- **Platform rollout mode**: Controlled via `iqkv.platform.rollout-mode` (`MULTI_TENANT` | `SINGLE_TENANT`); must be identical across IAM, Billing, and Gateway; `PlatformModeGuardFilter` queries IAM `/actuator/info` on startup and every 60 s, comparing `platform.rollout-mode`; readiness set to `REFUSING_TRAFFIC` on mismatch; service fails readiness on invalid/missing local mode
- **Single-tenant mode**: `TenantContextFilter` auto-injects `X-Tenant-ID` from `iqkv.tenancy.default-tenant-key` when no tenant context is present (covers unauthenticated sign-in and pre-auth requests); `EntitlementSubjectResolver` selects `USER` subject scope; multi-tenant mode preserves existing tenant-selection flow
- **Configuration**: Type-safe `@ConfigurationProperties` records; environment-variable-driven for container deployments
- **Observability**: Micrometer + Prometheus; structured JSON logging with MDC; health probes for Kubernetes readiness/liveness
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo (80% gate), ArchUnit, oxfmt, commit convention enforcement

> See [AGENTS.md](AGENTS.md) for detailed project structure, DDD patterns, and AI agent guidelines.
