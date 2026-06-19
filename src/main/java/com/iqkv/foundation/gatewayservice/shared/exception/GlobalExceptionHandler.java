/*
 * Copyright 2026 IQKV Foundation Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.foundation.gatewayservice.shared.exception;

import java.nio.charset.StandardCharsets;

import com.iqkv.foundation.gatewayservice.infrastructure.monitoring.GatewayMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reactive global exception handler that maps gateway and security exceptions
 * to RFC 9457 Problem Detail JSON responses.
 *
 * <p>Ordered at {@code -2} to run before Spring's default
 * {@code DefaultWebExceptionHandler} (order {@code -1}).
 */
@Component
@Order(-2)
public class GlobalExceptionHandler implements WebExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final JsonMapper objectMapper;
  private final GatewayMetrics metrics;

  public GlobalExceptionHandler(final JsonMapper objectMapper,
                                final GatewayMetrics metrics) {
    this.objectMapper = objectMapper;
    this.metrics = metrics;
  }

  @Override
  public Mono<Void> handle(final ServerWebExchange exchange, final Throwable ex) {
    final ProblemDetail problem = toProblemDetail(ex);
    final ServerHttpResponse response = exchange.getResponse();

    response.setStatusCode(HttpStatus.valueOf(problem.getStatus()));
    response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

    final byte[] bytes = serialize(problem);
    final DataBuffer buffer = response.bufferFactory().wrap(bytes);
    return response.writeWith(Mono.just(buffer));
  }

  private ProblemDetail toProblemDetail(final Throwable ex) {
    if (ex instanceof RouteNotFoundException e) {
      log.warn("Route not found: {}", e.getMessage());
      return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
    if (ex instanceof DownstreamServiceException e) {
      log.error("Downstream service error: {}", e.getMessage(), e);
      metrics.recordDownstreamError("unknown", HttpStatus.BAD_GATEWAY.value());
      return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }
    if (ex instanceof JwtClaimExtractionException e) {
      log.warn("JWT claim extraction failed: {}", e.getMessage());
      metrics.recordAuthFailure("jwt_extraction_failed");
      return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
    if (ex instanceof RateLimitExceededException e) {
      log.warn("Rate limit exceeded: {}", e.getMessage());
      metrics.recordRateLimitHit("unknown");
      return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }
    if (ex instanceof InvalidBearerTokenException e) {
      log.warn("Invalid bearer token: {}", e.getMessage());
      metrics.recordAuthFailure("invalid_token");
      return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
    }
    if (ex instanceof AuthenticationException e) {
      log.warn("Authentication failed: {}", e.getMessage());
      metrics.recordAuthFailure("authentication_required");
      return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    if (ex instanceof AccessDeniedException e) {
      log.warn("Access denied: {}", e.getMessage());
      metrics.recordAuthFailure("access_denied");
      return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
    }
    if (ex instanceof PlanFeatureNotAvailableException e) {
      log.warn("Plan feature not available: featureCode={}, planMessage={}", e.getFeatureCode(), e.getMessage());
      final ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
      pd.setProperty("featureCode", e.getFeatureCode());
      return pd;
    }
    if (ex instanceof ResponseStatusException e) {
      log.warn("Response status exception: {}", e.getMessage());
      return ProblemDetail.forStatusAndDetail(
          HttpStatus.valueOf(e.getStatusCode().value()), e.getReason() != null ? e.getReason() : e.getMessage());
    }
    log.error("Unexpected gateway error: {}", ex.getMessage(), ex);
    return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred");
  }

  private byte[] serialize(final ProblemDetail problem) {
    try {
      return objectMapper.writeValueAsBytes(problem);
    } catch (Exception e) {
      log.error("Failed to serialize ProblemDetail", e);
      return ("{\"status\":500,\"detail\":\"An unexpected error occurred\"}")
          .getBytes(StandardCharsets.UTF_8);
    }
  }
}
