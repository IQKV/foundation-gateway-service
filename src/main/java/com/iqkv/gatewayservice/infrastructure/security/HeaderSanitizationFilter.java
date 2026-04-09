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

package com.iqkv.gatewayservice.infrastructure.security;

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
 */
@Component
public class HeaderSanitizationFilter implements GlobalFilter, Ordered {

  private static final List<String> PROTECTED_HEADERS = List.of(
      "X-User-ID",
      "X-Username",
      "X-User-Email",
      "X-User-Authorities",
      "X-User-Permissions",
      "X-Tenant-ID",
      "X-Organization-ID"
  );

  @Override
  public int getOrder() {
    return -190;
  }

  @Override
  public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
    final ServerHttpRequest sanitized = exchange.getRequest().mutate()
        .headers(headers -> PROTECTED_HEADERS.forEach(headers::remove))
        .build();
    return chain.filter(exchange.mutate().request(sanitized).build());
  }
}
