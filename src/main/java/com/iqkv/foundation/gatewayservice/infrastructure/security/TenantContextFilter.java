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

package com.iqkv.foundation.gatewayservice.infrastructure.security;

import java.util.Optional;

import com.iqkv.foundation.gatewayservice.infrastructure.config.GatewayConfigurationProperties;
import com.iqkv.foundation.gatewayservice.infrastructure.config.PlatformConfigurationProperties;
import com.iqkv.foundation.gatewayservice.infrastructure.config.RolloutMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Resolves and injects the {@code X-Tenant-ID} header for downstream services.
 *
 * <p>This filter runs at order {@code -50}, after {@link JwtContextPropagationFilter}
 * ({@code -100}) has already applied the JWT {@code tenant_id} claim as
 * {@code X-Tenant-ID}, and after {@link HeaderSanitizationFilter} ({@code -190}) has
 * stripped any client-supplied tenant headers.
 *
 * <h2>MULTI_TENANT mode</h2>
 * <p>The filter reads the {@code X-Tenant-ID} header that was set by
 * {@link JwtContextPropagationFilter}. If it is already present the request is
 * forwarded unchanged. If it is absent the request is forwarded without a tenant
 * header — downstream services are responsible for rejecting unauthenticated or
 * tenant-less requests.
 *
 * <h2>SINGLE_TENANT mode</h2>
 * <p>The filter checks whether {@code X-Tenant-ID} is already present (set from the
 * JWT claim). If it is, the request is forwarded unchanged. If it is absent — which
 * covers both unauthenticated requests (e.g. {@code POST /api/v1/iam/auth/signin})
 * and authenticated requests whose JWT does not yet carry a {@code tenant_id} claim —
 * the configured {@code iqkv.tenancy.default-tenant-key} is injected as
 * {@code X-Tenant-ID}.
 *
 * <p>If no default tenant key is configured in single-tenant mode a warning is logged
 * and the request is forwarded without the header.
 */
@Component
public class TenantContextFilter implements GlobalFilter, Ordered, TenantContextResolutionPolicy {

  private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

  /**
   * Header name used to carry the tenant key to downstream services.
   */
  static final String X_TENANT_ID = "X-Tenant-ID";

  /**
   * Sign-in endpoint path that must receive the default tenant key even when the
   * request is unauthenticated (no JWT present).
   */
  private static final String SIGNIN_PATH = "/api/v1/iam/auth/signin";

  /**
   * Runs after JWT propagation ({@code -100}) so the JWT-derived {@code X-Tenant-ID}
   * is already present when this filter executes.
   */
  private static final int ORDER = -50;

  private final PlatformConfigurationProperties platformConfig;
  private final GatewayConfigurationProperties.Tenancy tenancyProperties;

  public TenantContextFilter(
      final PlatformConfigurationProperties platformConfig,
      final GatewayConfigurationProperties.Tenancy tenancyProperties) {
    this.platformConfig = platformConfig;
    this.tenancyProperties = tenancyProperties;
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  // ---------------------------------------------------------------------------
  // GlobalFilter
  // ---------------------------------------------------------------------------

  @Override
  public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
    final Optional<String> resolved = resolveTenantContext(exchange);

    if (resolved.isEmpty()) {
      // Nothing to inject — forward as-is.
      return chain.filter(exchange);
    }

    final String tenantKey = resolved.get();
    final String existingHeader = exchange.getRequest().getHeaders().getFirst(X_TENANT_ID);

    if (tenantKey.equals(existingHeader)) {
      // Header already carries the correct value — no mutation needed.
      return chain.filter(exchange);
    }

    log.debug(
        "event=tenant_context_injected service=gateway-service "
        + "tenant_key={} path={} mode={}",
        tenantKey,
        exchange.getRequest().getPath(),
        platformConfig.rolloutMode());

    final ServerHttpRequest mutated = exchange.getRequest().mutate()
        .header(X_TENANT_ID, tenantKey)
        .build();
    return chain.filter(exchange.mutate().request(mutated).build());
  }

  // ---------------------------------------------------------------------------
  // TenantContextResolutionPolicy
  // ---------------------------------------------------------------------------

  /**
   * {@inheritDoc}
   *
   * <p>Resolution logic:
   * <ol>
   *   <li>If {@code X-Tenant-ID} is already present in the request (set by
   *       {@link JwtContextPropagationFilter}), return it as-is for both modes.</li>
   *   <li>In {@code MULTI_TENANT} mode with no existing header, return empty — the
   *       downstream service will reject the request.</li>
   *   <li>In {@code SINGLE_TENANT} mode with no existing header, return the configured
   *       default tenant key (or empty with a warning if it is not configured).</li>
   * </ol>
   */
  @Override
  public Optional<String> resolveTenantContext(final ServerWebExchange exchange) {
    final String existingTenantId = exchange.getRequest().getHeaders().getFirst(X_TENANT_ID);

    // If a tenant context is already present (from JWT claim), honour it in both modes.
    if (existingTenantId != null && !existingTenantId.isBlank()) {
      return Optional.of(existingTenantId);
    }

    if (platformConfig.rolloutMode() == RolloutMode.MULTI_TENANT) {
      // Multi-tenant: no auto-injection — tenant must come from the JWT.
      return Optional.empty();
    }

    // Single-tenant: auto-inject the default tenant key.
    return resolveDefaultTenantKey(exchange);
  }

  // ---------------------------------------------------------------------------
  // Internal helpers
  // ---------------------------------------------------------------------------

  private Optional<String> resolveDefaultTenantKey(final ServerWebExchange exchange) {
    final String defaultKey = tenancyProperties.getDefaultTenantKey();

    if (defaultKey == null || defaultKey.isBlank()) {
      log.warn(
          "event=tenant_context_missing_default service=gateway-service "
          + "path={} message=\"Single-tenant mode active but iqkv.tenancy.default-tenant-key "
          + "is not configured; forwarding request without X-Tenant-ID\"",
          exchange.getRequest().getPath());
      return Optional.empty();
    }

    return Optional.of(defaultKey);
  }
}
