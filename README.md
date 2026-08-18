> ## 🤔 What is this service all about?
>
> - Reactive API gateway — the single entry point for the iQ Key Value microservices platform.
> - Handles JWT validation, header sanitization, user/tenant context propagation, and response security hardening.
> - Make the project easy to maintain with **8 issue templates**.
> - Quick-start documentation
> - Manage issues with **20 issue labels**.
> - Make _community healthier_ with all the guides like code of conduct, contributing, support, security...
> - Learn more with the [official GitHub guide on creating repositories from a template](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/creating-a-repository-from-a-template).

---

# 🌐 iQ Key Value Gateway Service

Reactive API gateway providing JWT authentication, header sanitization, user/tenant context propagation, and observability across the iQ Key Value microservices platform.

## Overview

This is the front door to the iQ Key Value microservices ecosystem. Built on Spring Cloud Gateway with WebFlux, it handles all cross-cutting concerns — authentication, correlation tracking, tenant extraction, and response security hardening — so downstream services receive a clean, enriched request context. For detailed documentation, please refer to the [docs](./docs) directory.

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## Key Features

- **Reactive Gateway**: Spring Cloud Gateway with WebFlux — non-blocking I/O throughout the filter chain
- **JWT Authentication**: RS256 validation via JWK Set URI exposed by the IAM service; public paths bypass auth via `iqkv.gateway.public-paths`
- **Header Sanitization**: Strips all `X-User-*`, `X-Tenant-ID`, `X-Organization-ID`, and `X-Audit-*` headers from incoming requests before JWT processing to prevent identity spoofing
- **Context Propagation**: Extracts `userId`, `username`, `email`, `authorities`, and `tenant_id` from the validated JWT; captures client IP and User-Agent for auditing — all forwarded as typed headers to downstream services
- **Correlation Tracking**: Generates or propagates `X-Correlation-ID` on every request; echoes it back on the response
- **Response Security Headers**: Injects `X-Content-Type-Options`, `X-Frame-Options`, `X-XSS-Protection`, and `Referrer-Policy` on all responses
- **Custom Gateway Metrics**: Detailed tracking of request rates, latencies, and errors by `route_id` and `tenant_id`
- **Aggregated Swagger UI**: SpringDoc proxies downstream `/api-docs` endpoints through the gateway at `/swagger-ui.html`
- **Observability**: Prometheus metrics at `/actuator/prometheus`, health probes at `/actuator/health`, structured JSON logging with MDC context
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo (80% coverage gate), ArchUnit, oxfmt, Husky git hooks

## Prerequisites

- Java 25 (Eclipse Temurin)
- Maven 3.9+
- Node.js >= 22.15.0 & pnpm >= 10.33.2
- Docker & Docker Compose

## Quick Start

```bash
# Clone the repository
git clone https://github.com/IQKV/foundation-gateway-service.git

# Navigate to project directory
cd foundation-gateway-service

# Install git hooks
pnpm install

# Start local dev infrastructure (Postgres, RabbitMQ, MailHog, etc.)
docker compose up -d

# Run the application (requires IAM service on localhost:8082)
mvn spring-boot:run -Dspring-boot.run.profiles=local -P local
# → Gateway API:  http://localhost:8080
# → Actuator:     http://localhost:8081/actuator/health
# → Swagger UI:   http://localhost:8080/swagger-ui.html
```

## Filter Chain

Filters execute in order. Each `GlobalFilter` is `Ordered` — lower numbers run first on the request path, last on the response path.

| Order   | Filter                         | Responsibility                                                         |
| ------- | ------------------------------ | ---------------------------------------------------------------------- |
| `-201`  | `MonitoringFilter`             | Record request metrics (rate, duration, status, tenant)                |
| `-200`  | `CorrelationIdFilter`          | Generate or propagate `X-Correlation-ID`; store in exchange attributes |
| `-190`  | `HeaderSanitizationFilter`     | Strip spoofable headers (`X-User-*`, `X-Tenant-ID`, `X-Audit-*`, etc.) |
| `-180`  | `AuditContextFilter`           | Extract client IP and User-Agent for audit context propagation         |
| `-100`  | `JwtContextPropagationFilter`  | Extract user/tenant claims from validated JWT; set downstream headers  |
| `MIN+1` | `ResponseTransformationFilter` | Add security response headers; echo `X-Correlation-ID` to client       |

Spring Security OAuth2 Resource Server handles JWT signature validation (RS256 via JWKS) before the `JwtContextPropagationFilter` runs.

## Downstream Headers

After the filter chain, downstream services receive the following headers on every authenticated request:

| Header               | Source                  | Description                                        |
| -------------------- | ----------------------- | -------------------------------------------------- |
| `X-User-ID`          | JWT `userId` claim      | User identifier                                    |
| `X-Username`         | JWT `username` claim    | Username                                           |
| `X-User-Email`       | JWT `email` claim       | User email address                                 |
| `X-User-Authorities` | JWT `authorities` claim | Comma-separated authority list (e.g. `ADMIN,USER`) |
| `X-Tenant-ID`        | JWT `tenant_id` claim   | Tenant identifier                                  |
| `X-Correlation-ID`   | Generated / propagated  | Request correlation ID for distributed tracing     |
| `X-Audit-IP`         | Client IP address       | Original client IP address for audit logging       |
| `X-Audit-UA`         | Client User-Agent       | Original client User-Agent for audit logging       |
| `X-Audit-Source`     | Configured source       | Gateway identifier (e.g. `web-gateway`)            |

**Security note**: The gateway strips all of these headers from the incoming client request before JWT processing. Only the gateway sets them after successful validation — clients cannot spoof user identity by injecting headers.

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
                    jwk-set-uri: http://foundation-iam-service:8080/.well-known/jwks.json
```

## Routes

Routes are defined in `application.yml` under `spring.cloud.gateway.server.webflux.routes`:

| Route ID           | URI                   | Predicate                          | Auth                      | Description                              |
| ------------------ | --------------------- | ---------------------------------- | ------------------------- | ---------------------------------------- |
| `iam-jwks`         | `IAM_SERVICE_URI`     | `Path=/.well-known/**`             | Public                    | JWKS endpoint for JWT validation         |
| `iam-api`          | `IAM_SERVICE_URI`     | `Path=/api/v1/iam/**`              | Public (configurable)     | All IAM API endpoints                    |
| `billing-webhooks` | `BILLING_SERVICE_URI` | `Path=/api/v1/billing/webhooks/**` | Public (Stripe signature) | Stripe webhook ingestion                 |
| `billing-api`      | `BILLING_SERVICE_URI` | `Path=/api/v1/billing/**`          | Protected                 | All billing API endpoints (requires JWT) |

Public path patterns are configured via `iqkv.gateway.public-paths` and enforced by `SecurityConfig` + `GatewayProperties`.

## Environment Variables

| Variable               | Default                                       | Description                             |
| ---------------------- | --------------------------------------------- | --------------------------------------- |
| `SERVER_PORT`          | `8080`                                        | Gateway API port                        |
| `MANAGEMENT_PORT`      | `8081`                                        | Actuator / management port              |
| `IAM_SERVICE_URI`      | `http://localhost:8082`                       | Base URI of the IAM service             |
| `IAM_JWKS_URI`         | `http://localhost:8082/.well-known/jwks.json` | JWK Set URI for JWT validation          |
| `BILLING_SERVICE_URI`  | `http://localhost:8084`                       | Base URI of the Billing service         |
| `IQKV_IAM_SERVICE_URL` | `http://localhost:8083`                       | IAM service URL for platform mode guard |
| `CORS_ALLOWED_ORIGINS` | `*`                                           | Allowed CORS origin patterns            |

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
docker build -t iqkv/foundation-gateway-service:latest .

# Run with full platform (gateway + SonarQube)
docker compose -f compose.container.yaml up -d
```

The Dockerfile uses a multi-stage build: Maven compiles in `eclipse-temurin:25-jdk-alpine`, the runtime stage uses `eclipse-temurin:25-jre-alpine` with a non-root `appuser` and layered JAR extraction for optimal cache reuse.

## Monitoring

| Endpoint                   | Description                  |
| -------------------------- | ---------------------------- |
| `GET /actuator/health`     | Liveness + readiness probes  |
| `GET /actuator/metrics`    | Application metrics          |
| `GET /actuator/prometheus` | Prometheus scrape endpoint   |
| `GET /swagger-ui.html`     | Aggregated API documentation |
| `GET /api-docs`            | Gateway OpenAPI spec         |

A Grafana dashboard (`docker/grafana/`) provides real-time visibility into gateway health, routing metrics, and JVM memory. It uses Prometheus as the data source and auto-refreshes every 30 seconds.

## Project Structure

```
src/main/java/com/iqkv/foundation/gatewayservice/
├── infrastructure/
│   ├── config/
│   │   ├── GatewayProperties.java      # @ConfigurationProperties for iqkv.gateway.*
│   │   └── SecurityConfig.java         # WebFlux security, JWT converter, public paths
│   └── security/
│       ├── CorrelationIdFilter.java     # Generate/propagate X-Correlation-ID (order -200)
│       ├── HeaderSanitizationFilter.java # Strip spoofable headers (order -190)
│       ├── JwtContextPropagationFilter.java # Enrich downstream headers (order -100)
│       └── ResponseTransformationFilter.java # Security headers + correlation echo (order MIN+1)
└── shared/
    ├── exception/                       # Common exception types
    └── util/                            # Utility classes
```

## License

This project is licensed under the Apache License. See the [LICENSE](LICENSE) file for details.

## Contributing

Please read our [Contributing Guidelines](.github/CONTRIBUTING.md) and [Code of Conduct](.github/CODE_OF_CONDUCT.md).
