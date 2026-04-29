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

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extracts user and tenant context from the validated JWT and propagates them
 * as downstream headers. Runs after security validation (order {@code -100}).
 *
 * <p>Headers added:
 * <ul>
 *   <li>{@code X-User-ID} — userId claim</li>
 *   <li>{@code X-Username} — username claim</li>
 *   <li>{@code X-User-Email} — email claim</li>
 *   <li>{@code X-User-Authorities} — comma-separated authorities</li>
 *   <li>{@code X-Tenant-ID} — tenant_id claim</li>
 * </ul>
 */
@Component
public class JwtContextPropagationFilter implements GlobalFilter, Ordered {

  @Override
  public int getOrder() {
    return -100;
  }

  @Override
  public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .filter(auth -> auth instanceof JwtAuthenticationToken)
        .cast(JwtAuthenticationToken.class)
        .map(JwtAuthenticationToken::getToken)
        .flatMap(jwt -> chain.filter(enrichExchange(exchange, jwt)))
        .switchIfEmpty(chain.filter(exchange));
  }

  private ServerWebExchange enrichExchange(final ServerWebExchange exchange, final Jwt jwt) {
    final ServerHttpRequest enriched = exchange.getRequest().mutate()
        .headers(headers -> {
          setIfPresent(headers, "X-User-ID", jwt.getClaimAsString("userId"));
          setIfPresent(headers, "X-Username", jwt.getClaimAsString("username"));
          setIfPresent(headers, "X-User-Email", jwt.getClaimAsString("email"));
          setIfPresent(headers, "X-Tenant-ID", jwt.getClaimAsString("tenant_id"));

          final List<String> authorities = jwt.getClaimAsStringList("authorities");
          if (authorities != null && !authorities.isEmpty()) {
            headers.set("X-User-Authorities", String.join(",", authorities));
          }
        })
        .build();
    return exchange.mutate().request(enriched).build();
  }

  private void setIfPresent(final org.springframework.http.HttpHeaders headers,
                            final String name, final String value) {
    if (value != null && !value.isBlank()) {
      headers.set(name, value);
    }
  }
}
