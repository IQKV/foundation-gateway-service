# 🌐 IQ Key Value Gateway Service

> Reactive API gateway providing intelligent routing, JWT authentication, and tenant context propagation across microservices.

## Table of Contents

- [Business Purpose](#business-purpose)
- [Overview](#overview)
- [What It Demonstrates](#what-it-demonstrates)
- [Architecture Patterns](#architecture-patterns)
- [Technical Highlights](#technical-highlights)
- [Use Cases Implemented](#use-cases-implemented)
- [API Examples](#api-examples)
- [Learning Points](#learning-points)
- [Adapting for Your Domain](#adapting-for-your-domain)
- [Integration with Downstream Services](#integration-with-downstream-services)
- [Deployment Guide](docs/deployment/README.md)

## Business Purpose

A centralized entry point for the IQ Key Value microservices platform that handles:

- **Intelligent Routing** - Dynamic request routing to downstream services with path-based and header-based versioning
- **Authentication Gateway** - JWT validation and user context propagation to all protected services
- **Multi-Tenancy** - Tenant context extraction from headers or JWT claims and propagation downstream
- **Request Transformation** - Header enrichment, correlation ID generation, and context propagation
- **Observability** - Distributed tracing, Prometheus metrics, and structured logging

## Overview

This is the front door to the IQ Key Value microservices ecosystem. Built on Spring Cloud Gateway with reactive programming, it provides a single entry point for all client requests while handling cross-cutting concerns like authentication, authorization, and observability.

## What It Demonstrates

### 🌐 Reactive Gateway Patterns

- Spring Cloud Gateway with WebFlux for non-blocking I/O
- Reactive filter chains with ordered execution (`GlobalFilter` + `Ordered`)
- Reactive JWT validation with OAuth2 Resource Server`

### 🔐 Authentication & Authorization

- JWT validation using RSA256 with JWK Set endpoint
- User context extraction (userId, username, email, authorities, permissions, organizationId)
- Authority propagation via headers (X-User-Authorities, X-User-Email, X-User-Permissions, X-Organization-ID)
- Header sanitization to prevent spoofing attacks (removes all user/tenant context headers from incoming requests)
- Public path pattern matching (exact and wildcard `/**`)
- MDC logging with user and tenant context

### 🏢 Multi-Tenancy Support

- Priority-based tenant extraction (JWT claims → X-Tenant-ID header)
- Tenant context stored in exchange attributes and propagated downstream
- Tenant ID forwarded via X-Tenant-ID header to all services

### 🎯 Observability & Monitoring

- Correlation ID generation and propagation across all requests
- OpenTelemetry distributed tracing with OTLP export
- Prometheus metrics for gateway operations
- Structured JSON logging with MDC context (user ID, tenant ID, correlation ID, trace ID)

## Architecture Patterns

### Reactive Filter Chain

```
Request Flow:
1. CorrelationIdFilter          → Generate/extract correlation ID, set MDC context
2. TenantExtractionFilter       → Extract tenant context from JWT or header
3. JwtAuthenticationFilter      → Validate JWT and extract user context
5. ApiVersionRoutingFilter      → Handle API versioning
7. RequestTransformationFilter  → Enrich headers with user/tenant context
8. Route to downstream service
9. ResponseTransformationFilter → Add security headers, remove internal headers
```

### API Design

- Centralized routing configuration in YAML
- Path-based and header-based API versioning
- Public vs protected endpoint segregation
- Consistent error responses with Problem Details (RFC 7807)

## Technical Highlights

### Reactive Programming

- Non-blocking I/O with Project Reactor (Mono/Flux)
- `ReactiveSecurityContextHolder` for JWT validation
- Reactive filter chains with `flatMap` and `transformDeferred`

### Security Features

- JWT validation with RSA256 public key via JWK Set URI
- CORS configuration per environment
- Security header injection on all responses
- Internal header removal from responses
- Route protection with authority-based access control

### Operational Features

- Docker containerization
- Health checks and actuator endpoints
- Structured JSON logging
- Prometheus metrics export
- OpenAPI documentation via SpringDoc

## Use Cases Implemented

### Request Routing

- Route requests to downstream services

### Authentication Flow

- Validate JWT tokens from Authorization header
- Extract user context (userId, username, email, authorities, permissions)
- Sanitize incoming headers to prevent spoofing (removes X-User-\*, X-Tenant-ID, X-Organization-ID)
- Propagate user context to downstream services via headers:
    - `X-User-ID` - User identifier
    - `X-Username` - Username
    - `X-User-Email` - User email address
    - `X-User-Authorities` - Comma-separated list of authorities (e.g., `ADMIN,USER`)
    - `X-User-Permissions` - Comma-separated list of permissions
    - `X-Tenant-ID` - Tenant identifier
    - `X-Organization-ID` - Organization identifier
- Skip authentication for public paths

### Multi-Tenancy

- Extract tenant from X-Tenant-ID header or JWT claims
- Tenant context propagation to all downstream services

### Request Transformation

- Add correlation ID to all requests
- Sanitize incoming headers (remove X-User-\*, X-Tenant-ID, X-Organization-ID to prevent spoofing)
- Propagate user and tenant context headers
- Add gateway version header
- Remove internal headers from requests

### Response Transformation

- Add security headers (X-Content-Type-Options, X-Frame-Options, etc.)
- Add correlation headers for tracing
- Remove internal service headers from responses
- Consistent error response format

## API Examples

### Monitoring Endpoints

- `/actuator/health` - Health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics
- `/swagger-ui.html` - Aggregated API documentation

### Grafana Dashboard

A Grafana dashboard is available at `docs/monitoring/grafana-dashboard.json` providing real-time visibility into:

- Gateway health: uptime, request rate, error rate, p95 latency
- Routing metrics: request rate by route, response time percentiles (p50/p95/p99)
- JVM memory: heap/non-heap usage, GC pause time, thread count

The dashboard uses Prometheus as the data source and auto-refreshes every 30 seconds.

## Learning Points

This implementation serves as a reference for:

- Building reactive API gateways with Spring Cloud Gateway
- JWT validation and user context propagation
- Feature-based access control at the gateway level
- Multi-tenant request routing and context isolation
- Correlation ID tracking across services
- API versioning strategies (path and header-based)
- Request/response transformation patterns
- Reactive programming with Project Reactor
- Observability in distributed systems (tracing, metrics, structured logging)
- Type-safe configuration with Java records and Bean Validation

## Adapting for Your Domain

### API Gateway Patterns

- SaaS applications with tenant isolation and feature gating
- Microservices architectures requiring a unified entry point
- Mobile app backends with centralized authentication
- E-commerce platforms with multiple backend services

### Authentication Gateway

- Centralized authentication for microservices
- Token validation and context propagation
- Multi-tenant access control
- Public vs protected endpoint segregation

### Request Transformation

- Header enrichment for downstream services
- Correlation ID generation for distributed tracing
- User and tenant context extraction and forwarding

## Integration with Downstream Services

### Consuming Gateway Context

<details>
<summary>Click to expand gateway context consumption example</summary>

Downstream services receive enriched headers from the gateway:

```java
@GetMapping("/protected")
public ResponseEntity<?> protectedEndpoint(
    @RequestHeader("X-User-ID") Long userId,
    @RequestHeader("X-Username") String username,
    @RequestHeader("X-User-Email") String email,
    @RequestHeader("X-User-Authorities") String authorities,
    @RequestHeader("X-Tenant-ID") String tenantId,
    @RequestHeader("X-Organization-ID") Long organizationId,
    @RequestHeader("X-Correlation-ID") String correlationId
) {
    List<String> authorityList = Arrays.asList(authorities.split(","));
    logger.info("Request from user {} (tenant: {}) correlation: {}", username, tenantId, correlationId);
    return ResponseEntity.ok(/* response */);
}
```

**Security Note**: The gateway sanitizes all incoming user/tenant context headers before processing. This prevents clients from spoofing user identity by injecting malicious headers. Only the gateway sets these headers after JWT validation.

</details>

### JWT Validation Configuration

<details>
<summary>Click to expand JWT validation configuration</summary>

Services can validate JWTs independently using the same JWK Set:

```yaml
spring:
    security:
        oauth2:
            resourceserver:
                jwt:
                    jwk-set-uri: http://foundation-iam-service:8080/.well-known/jwks.json
```

</details>

---

**Use this as a blueprint** for building reactive API gateways with intelligent routing, JWT authentication, and multi-tenant support in your microservices architecture.
