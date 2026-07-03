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

package com.iqkv.foundation.gatewayservice.plan;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.iqkv.foundation.gatewayservice.infrastructure.config.GatewayConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Local in-memory cache of the billing plan catalog for the gateway.
 *
 * <p>Fetches {@code GET /api/v1/billing/internal/plans} from the billing service at startup
 * and refreshes on a configurable schedule (default: every 10 minutes).
 *
 * <p>Falls back to the last known state when billing is temporarily unreachable — the cache
 * is only reset if the gateway restarts while billing is unavailable.
 *
 * <p>Used by {@link RequiresPlanFeatureFilterFactory} to resolve plan features from the
 * {@code X-Plan-Code} header without a synchronous call to billing on the hot path.
 */
@Component
public class PlanCatalogCache {

  private static final Logger log = LoggerFactory.getLogger(PlanCatalogCache.class);
  private static final String INTERNAL_PLANS_PATH = "/api/v1/billing/internal/plans";

  /**
   * Local DTO for deserializing the billing internal plans response.
   */
  record PlanCatalogEntry(String planCode, PlanEntitlement planEntitlement) {
  }

  private volatile Map<String, PlanEntitlement> cache = Map.of();

  private final WebClient billingClient;

  public PlanCatalogCache(final GatewayConfigurationProperties.Billing billingProps,
                          final WebClient.Builder webClientBuilder) {
    this.billingClient = webClientBuilder
        .baseUrl(billingProps.getServiceUrl())
        .build();
  }

  @PostConstruct
  public void loadOnStartup() {
    refresh();
  }

  /**
   * Refreshes the plan catalog from the billing service.
   * Runs on a fixed delay configured by {@code iqkv.billing.plan-catalog-refresh-interval}.
   * Falls back to the last known cache on failure.
   */
  @Scheduled(fixedDelayString = "${iqkv.billing.plan-catalog-refresh-interval:PT10M}")
  public void refresh() {
    try {
      final List<PlanCatalogEntry> plans = billingClient.get()
          .uri(INTERNAL_PLANS_PATH)
          .retrieve()
          .bodyToFlux(PlanCatalogEntry.class)
          .collectList()
          .block(Duration.ofSeconds(5));

      if (plans != null && !plans.isEmpty()) {
        cache = plans.stream()
            .filter(e -> e.planCode() != null && e.features() != null)
            .collect(Collectors.toUnmodifiableMap(
                PlanCatalogEntry::planCode,
                PlanCatalogEntry::features
            ));
        log.info("Plan catalog refreshed: {} plans loaded", cache.size());
      } else {
        log.warn("Plan catalog refresh returned empty response — keeping last known state");
      }
    } catch (final Exception e) {
      log.warn("Failed to refresh plan catalog from billing service, using last known state: {}",
          e.getMessage());
    }
  }

  /**
   * Returns the {@link PlanEntitlement} for the given plan code.
   * Falls back to {@link PlanEntitlement#NONE} when the plan code is unknown or the cache is empty.
   *
   * @param planCode the plan code (e.g. {@code "pro-monthly"})
   * @return the plan's features, never {@code null}
   */
  public PlanEntitlement resolveEntitlement(final String planCode) {
    if (planCode == null || planCode.isBlank()) {
      return PlanEntitlement.NONE;
    }
    return cache.getOrDefault(planCode, PlanEntitlement.NONE);
  }
}
