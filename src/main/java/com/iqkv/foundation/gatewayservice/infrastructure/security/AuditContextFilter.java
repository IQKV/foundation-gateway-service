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

import java.net.InetSocketAddress;

import com.iqkv.foundation.gatewayservice.infrastructure.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extracts audit-related technical context (IP, User-Agent) and propagates them
 * as downstream headers. Runs after header sanitization (order {@code -180}).
 *
 * <p>Headers added:
 * <ul>
 *   <li>{@code X-Audit-IP} — Client IP address</li>
 *   <li>{@code X-Audit-UA} — Client User-Agent</li>
 *   <li>{@code X-Audit-Source} — Gateway source identifier</li>
 * </ul>
 */
@Component
public class AuditContextFilter implements GlobalFilter, Ordered {

  private final GatewayProperties properties;

  public AuditContextFilter(final GatewayProperties properties) {
    this.properties = properties;
  }

  @Override
  public int getOrder() {
    return -180;
  }

  @Override
  public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
    final HttpHeaders headers = exchange.getRequest().getHeaders();

    final String clientIp = extractIp(exchange);
    final String userAgent = headers.getFirst(HttpHeaders.USER_AGENT);

    final ServerWebExchange mutated = exchange.mutate()
        .request(r -> {
          if (clientIp != null) {
            r.header("X-Audit-IP", clientIp);
          }
          if (userAgent != null) {
            r.header("X-Audit-UA", userAgent);
          }
          r.header("X-Audit-Source", properties.auditSource());
        })
        .build();

    return chain.filter(mutated);
  }

  private String extractIp(final ServerWebExchange exchange) {
    final String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    final InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
    return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : null;
  }
}
