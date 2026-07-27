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

package com.iqkv.foundation.gatewayservice.infrastructure.security;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Strips all user/tenant context headers from incoming client requests to prevent
 * identity spoofing. Runs before JWT extraction (order {@code -190}).
 *
 * <p>{@code X-Tenant-ID} is excluded from stripping on unauthenticated auth endpoints
 * (signin, refresh) because those requests carry no JWT and the header is the only
 * mechanism available to supply tenant context.
 */
@Component
public class HeaderSanitizationFilter implements GlobalFilter, Ordered {

  /**
   * Headers that are always stripped — these must never be client-supplied.
   */
  private static final List<String> ALWAYS_STRIP = List.of(
      "X-User-ID",
      "X-Username",
      "X-User-Email",
      "X-User-Authorities",
      "X-User-Permissions",
      "X-Organization-ID",
      "X-Plan-Code",
      "X-Audit-IP",
      "X-Audit-UA",
      "X-Audit-Source"
  );

  /**
   * Auth endpoints that legitimately supply {@code X-Tenant-ID} without a JWT.
   * {@code X-Tenant-ID} is stripped on all other paths.
   * WebSocket paths are also included — browsers cannot set custom headers on
   * WS upgrades, but non-browser clients may supply the tenant via header.
   */
  private static final List<String> TENANT_HEADER_ALLOWED_PATHS = List.of(
      "/api/v1/iam/auth/signin",
      "/api/v1/iam/auth/refresh"
  );

  private static final String WS_PATH_PREFIX = "/api/v1/iam/ws";

  @Override
  public int getOrder() {
    return -190;
  }

  @Override
  public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
    final String path = exchange.getRequest().getURI().getPath();
    final boolean allowTenantHeader = TENANT_HEADER_ALLOWED_PATHS.contains(path)
                                      || path.startsWith(WS_PATH_PREFIX);

    final ServerHttpRequest sanitized = exchange.getRequest().mutate()
        .headers(headers -> {
          ALWAYS_STRIP.forEach(headers::remove);
          if (!allowTenantHeader) {
            headers.remove("X-Tenant-ID");
          }
        })
        .build();
    return chain.filter(exchange.mutate().request(sanitized).build());
  }
}
