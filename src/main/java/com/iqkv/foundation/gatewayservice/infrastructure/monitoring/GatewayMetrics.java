/*
 * Copyright 2026 iQKV Foundation Team.
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

package com.iqkv.foundation.gatewayservice.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Central component for managing custom gateway metrics.
 */
@Component
public class GatewayMetrics {

  private final MeterRegistry registry;

  public GatewayMetrics(final MeterRegistry registry) {
    this.registry = registry;
  }

  /**
   * Records a request.
   *
   * @param routeId  the gateway route ID
   * @param method   the HTTP method
   * @param status   the HTTP status code
   * @param tenantId the tenant ID (if available)
   */
  public void recordRequest(final String routeId, final String method, final int status, final String tenantId) {
    Counter.builder("gateway.requests.total")
        .tag("route_id", routeId != null ? routeId : "unknown")
        .tag("method", method)
        .tag("status", String.valueOf(status))
        .tag("tenant_id", tenantId != null ? tenantId : "none")
        .description("Total number of requests handled by the gateway")
        .register(registry)
        .increment();
  }

  /**
   * Records request duration.
   *
   * @return a Timer.Sample to be stopped when the request completes
   */
  public Timer.Sample startTimer() {
    return Timer.start(registry);
  }

  /**
   * Stops the timer and records the duration.
   *
   * @param sample  the timer sample
   * @param routeId the gateway route ID
   * @param method  the HTTP method
   * @param status  the HTTP status code
   */
  public void stopTimer(final Timer.Sample sample, final String routeId, final String method, final int status) {
    sample.stop(Timer.builder("gateway.requests.duration")
        .tag("route_id", routeId != null ? routeId : "unknown")
        .tag("method", method)
        .tag("status", String.valueOf(status))
        .description("Duration of requests handled by the gateway")
        .register(registry));
  }

  /**
   * Records an authentication or authorization failure.
   *
   * @param reason the reason for failure
   */
  public void recordAuthFailure(final String reason) {
    Counter.builder("gateway.security.auth.failures.total")
        .tag("reason", reason)
        .description("Total number of authentication/authorization failures")
        .register(registry)
        .increment();
  }

  /**
   * Records a rate limit hit.
   *
   * @param routeId the gateway route ID
   */
  public void recordRateLimitHit(final String routeId) {
    Counter.builder("gateway.rate_limit.hits.total")
        .tag("route_id", routeId != null ? routeId : "unknown")
        .description("Total number of requests rejected by rate limiting")
        .register(registry)
        .increment();
  }

  /**
   * Records a downstream service error.
   *
   * @param service the downstream service name
   * @param status  the HTTP status code returned by the service
   */
  public void recordDownstreamError(final String service, final int status) {
    Counter.builder("gateway.downstream.errors.total")
        .tag("service", service)
        .tag("status", String.valueOf(status))
        .description("Total number of errors from downstream services")
        .register(registry)
        .increment();
  }
}
