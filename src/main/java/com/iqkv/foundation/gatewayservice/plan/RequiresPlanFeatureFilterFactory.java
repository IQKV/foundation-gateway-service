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

package com.iqkv.foundation.gatewayservice.plan;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Gateway filter factory that enforces plan-based feature access at the route level.
 *
 * <p>Reads the {@code X-Plan-Code} header (stamped by {@code JwtContextPropagationFilter}
 * from the JWT {@code plan_code} claim), looks up the plan's features in the local
 * {@link PlanResolver}, and rejects the request with {@code 402 Payment Required}
 * if the required feature is not included in the plan.
 *
 * <p>Route configuration example:
 * <pre>
 * filters:
 *   - RequiresPlanFeature=priority_support
 * </pre>
 *
 * <p>If the plan data is empty (e.g. billing is unreachable on startup),
 * all feature checks will use {@link PlanEntitlement#NONE} and deny access to gated routes.
 * This is intentional — fail-safe behaviour prevents unauthorized access during degraded state.
 */
@Component
public class RequiresPlanFeatureFilterFactory
    extends AbstractGatewayFilterFactory<RequiresPlanFeatureFilterFactory.Config> {

  static final String X_PLAN_CODE_HEADER = "X-Plan-Code";

  private final PlanResolver planResolver;

  public RequiresPlanFeatureFilterFactory(final PlanResolver planResolver) {
    super(Config.class);
    this.planResolver = planResolver;
  }

  @Override
  public List<String> shortcutFieldOrder() {
    return List.of("feature");
  }

  @Override
  public GatewayFilter apply(final Config config) {
    return (exchange, chain) -> {
      final String planCode = exchange.getRequest().getHeaders().getFirst(X_PLAN_CODE_HEADER);
      final PlanEntitlement planEntitlement = planResolver.resolveEntitlement(planCode);

      if (!planEntitlement.has(config.getFeature())) {
        exchange.getResponse().setStatusCode(HttpStatus.PAYMENT_REQUIRED);
        return exchange.getResponse().setComplete();
      }

      return chain.filter(exchange);
    };
  }

  /**
   * Configuration for {@link RequiresPlanFeatureFilterFactory}.
   */
  public static class Config {

    /**
     * The feature key that must be present in the plan (e.g. {@code "priority_support"}).
     */
    private String feature;

    public String getFeature() {
      return feature;
    }

    public void setFeature(final String feature) {
      this.feature = feature;
    }
  }
}
