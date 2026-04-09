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

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adds security response headers and echoes the correlation ID back to the client.
 * Runs last in the post-processing phase (order {@code Integer.MIN_VALUE + 1}).
 */
@Component
public class ResponseTransformationFilter implements GlobalFilter, Ordered {

  @Override
  public int getOrder() {
    return Integer.MIN_VALUE + 1;
  }

  @Override
  public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
    return chain.filter(exchange).then(Mono.fromRunnable(() -> {
      final HttpHeaders headers = exchange.getResponse().getHeaders();
      headers.set("X-Content-Type-Options", "nosniff");
      headers.set("X-Frame-Options", "DENY");
      headers.set("X-XSS-Protection", "1; mode=block");
      headers.set("Referrer-Policy", "strict-origin-when-cross-origin");

      final String correlationId = (String) exchange.getAttributes()
          .get(CorrelationIdFilter.CORRELATION_ID_ATTR);
      if (correlationId != null) {
        headers.set(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
      }
    }));
  }
}
