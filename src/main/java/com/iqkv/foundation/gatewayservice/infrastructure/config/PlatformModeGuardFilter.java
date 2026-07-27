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

package com.iqkv.foundation.gatewayservice.infrastructure.config;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Guards all gateway traffic by verifying that the locally configured rollout mode
 * matches the canonical mode published by the IAM service.
 *
 * <p>On startup (via {@link PostConstruct}) and every 60 seconds thereafter, this filter
 * queries the IAM service {@code /actuator/info} endpoint and compares the reported
 * {@code rollout-mode} value against the local {@code iqkv.platform.rollout-mode}.
 *
 * <p>If a mismatch is detected:
 * <ul>
 *   <li>The service readiness is set to {@code REFUSING_TRAFFIC} (DOWN).</li>
 *   <li>A structured error is logged with both the local and canonical mode values.</li>
 *   <li>All business traffic receives {@code 503 Service Unavailable}.</li>
 * </ul>
 *
 * <p>If the IAM service is unreachable, a warning is logged and traffic is allowed to
 * proceed (fail-open) to avoid cascading failures during transient network issues.
 * The next scheduled re-validation will retry the check.
 *
 * <p>Runs with {@link Ordered#HIGHEST_PRECEDENCE} to execute before all other filters.
 */
@Component
public class PlatformModeGuardFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(PlatformModeGuardFilter.class);

  /**
   * JSON path within the IAM {@code /actuator/info} response that carries the rollout mode.
   * Expected structure: {@code { "platform": { "rollout-mode": "MULTI_TENANT" } }}
   */
  private static final String INFO_PLATFORM_KEY = "platform";
  private static final String INFO_ROLLOUT_MODE_KEY = "rollout-mode";

  /**
   * Timeout for each IAM health-check request.
   */
  private static final Duration IAM_REQUEST_TIMEOUT = Duration.ofSeconds(5);

  private final PlatformConfigurationProperties platformConfig;
  private final GatewayConfigurationProperties.Iam iamProperties;
  private final ApplicationContext applicationContext;
  private final WebClient webClient;

  /**
   * {@code true} when the last mode-consistency check detected a mismatch.
   * Volatile + AtomicBoolean ensures safe cross-thread visibility.
   */
  private final AtomicBoolean modeMismatchDetected = new AtomicBoolean(false);

  public PlatformModeGuardFilter(
      final PlatformConfigurationProperties platformConfig,
      final GatewayConfigurationProperties.Iam iamProperties,
      final ApplicationContext applicationContext,
      final WebClient.Builder webClientBuilder) {
    this.platformConfig = platformConfig;
    this.iamProperties = iamProperties;
    this.applicationContext = applicationContext;
    this.webClient = webClientBuilder.build();
  }

  // ---------------------------------------------------------------------------
  // GlobalFilter
  // ---------------------------------------------------------------------------

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  /**
   * Blocks all traffic when a mode mismatch has been detected.
   * Returns {@code 503 Service Unavailable} with a descriptive message.
   */
  @Override
  public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
    if (modeMismatchDetected.get()) {
      log.warn(
          "event=traffic_blocked_mode_mismatch service=gateway-service "
          + "local_mode={} path={}",
          platformConfig.rolloutMode(),
          exchange.getRequest().getPath());
      exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
      exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
      final byte[] body = ("{\"error\":\"Service unavailable: platform mode mismatch detected. "
                           + "Check service configuration.\"}").getBytes();
      final var buffer = exchange.getResponse().bufferFactory().wrap(body);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    }
    return chain.filter(exchange);
  }

  // ---------------------------------------------------------------------------
  // Startup validation
  // ---------------------------------------------------------------------------

  /**
   * Performs the initial mode-consistency check on startup.
   * Runs after dependency injection is complete.
   */
  @PostConstruct
  public void validateOnStartup() {
    log.info(
        "event=platform_mode_guard_startup service=gateway-service "
        + "local_mode={} iam_url={}",
        platformConfig.rolloutMode(),
        iamProperties.getServiceUrl());
    performModeCheck();
  }

  // ---------------------------------------------------------------------------
  // Periodic re-validation
  // ---------------------------------------------------------------------------

  /**
   * Re-validates mode consistency every 60 seconds to detect runtime drift.
   */
  @Scheduled(fixedDelay = 60_000)
  public void revalidatePeriodically() {
    log.debug("event=platform_mode_guard_revalidation service=gateway-service");
    performModeCheck();
  }

  // ---------------------------------------------------------------------------
  // Internal helpers
  // ---------------------------------------------------------------------------

  /**
   * Queries the IAM {@code /actuator/info} endpoint and compares the canonical mode
   * with the locally configured mode. Updates {@link #modeMismatchDetected} and
   * the application readiness state accordingly.
   */
  void performModeCheck() {
    final String iamInfoUrl = iamProperties.getServiceUrl() + "/actuator/info";

    webClient.get()
        .uri(iamInfoUrl)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
        })
        .timeout(IAM_REQUEST_TIMEOUT)
        .subscribe(
            body -> handleIamInfoResponse(body),
            error -> handleIamUnreachable(error)
        );
  }

  @SuppressWarnings("unchecked")
  private void handleIamInfoResponse(final Map<String, Object> body) {
    final String canonicalMode = extractCanonicalMode(body);

    if (canonicalMode == null) {
      log.warn(
          "event=platform_mode_canonical_missing service=gateway-service "
          + "iam_url={} message=\"IAM /actuator/info did not contain platform.rollout-mode; "
          + "skipping consistency check\"",
          iamProperties.getServiceUrl());
      return;
    }

    final String localMode = platformConfig.rolloutMode().name();

    if (!localMode.equalsIgnoreCase(canonicalMode)) {
      log.error(
          "event=platform_mode_mismatch service=gateway-service "
          + "local_mode={} canonical_mode={} "
          + "message=\"Rollout mode mismatch detected; setting readiness to DOWN and blocking traffic\"",
          localMode,
          canonicalMode);
      modeMismatchDetected.set(true);
      AvailabilityChangeEvent.publish(applicationContext, ReadinessState.REFUSING_TRAFFIC);
    } else {
      if (modeMismatchDetected.compareAndSet(true, false)) {
        // Mismatch was previously detected but is now resolved — restore readiness.
        log.info(
            "event=platform_mode_mismatch_resolved service=gateway-service "
            + "local_mode={} canonical_mode={} "
            + "message=\"Mode mismatch resolved; restoring readiness to ACCEPTING_TRAFFIC\"",
            localMode,
            canonicalMode);
        AvailabilityChangeEvent.publish(applicationContext, ReadinessState.ACCEPTING_TRAFFIC);
      } else {
        log.debug(
            "event=platform_mode_consistent service=gateway-service "
            + "local_mode={} canonical_mode={}",
            localMode,
            canonicalMode);
      }
    }
  }

  private void handleIamUnreachable(final Throwable error) {
    log.warn(
        "event=platform_mode_iam_unreachable service=gateway-service "
        + "iam_url={} error=\"{}\" "
        + "message=\"IAM service unreachable during mode check; allowing traffic (fail-open)\"",
        iamProperties.getServiceUrl(),
        error.getMessage());
    // Fail-open: do not block traffic on transient IAM unavailability.
    // The next scheduled re-validation will retry.
  }

  /**
   * Extracts the rollout mode string from the IAM {@code /actuator/info} response.
   *
   * <p>Expected JSON structure:
   * <pre>
   * {
   *   "platform": {
   *     "rollout-mode": "MULTI_TENANT"
   *   }
   * }
   * </pre>
   *
   * @param body the parsed response body
   * @return the canonical mode string, or {@code null} if not present
   */
  @SuppressWarnings("unchecked")
  private String extractCanonicalMode(final Map<String, Object> body) {
    if (body == null) {
      return null;
    }
    final Object platformSection = body.get(INFO_PLATFORM_KEY);
    if (platformSection instanceof Map<?, ?> platformMap) {
      final Object modeValue = platformMap.get(INFO_ROLLOUT_MODE_KEY);
      if (modeValue instanceof String modeStr) {
        return modeStr;
      }
    }
    return null;
  }
}
