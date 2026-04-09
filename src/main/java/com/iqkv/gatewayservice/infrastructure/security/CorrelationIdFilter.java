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

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Generates or propagates a {@code X-Correlation-ID} header on every request.
 * Runs first in the filter chain (order {@code -200}).
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  public static final String CORRELATION_ID_ATTR = "correlationId";

  @Override
  public int getOrder() {
    return -200;
  }

  @Override
  public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
    final String correlationId = exchange.getRequest().getHeaders()
        .getFirst(CORRELATION_ID_HEADER);
    final String resolved = (correlationId != null && !correlationId.isBlank())
        ? correlationId : UUID.randomUUID().toString();

    exchange.getAttributes().put(CORRELATION_ID_ATTR, resolved);

    final ServerWebExchange mutated = exchange.mutate()
        .request(r -> r.header(CORRELATION_ID_HEADER, resolved))
        .build();

    return chain.filter(mutated);
  }
}
