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

package com.iqkv.foundation.gatewayservice.infrastructure.monitoring;

import io.micrometer.core.instrument.Timer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter for recording request metrics.
 */
@Component
public class MonitoringFilter implements GlobalFilter, Ordered {

  private final GatewayMetrics metrics;

  public MonitoringFilter(final GatewayMetrics metrics) {
    this.metrics = metrics;
  }

  @Override
  public int getOrder() {
    // Run very early to start the timer, but after routing if possible to get routeId.
    // However, global filters run after routing anyway if they have positive order,
    // but we want to measure the whole gateway overhead too.
    // Let's run at -201, just before CorrelationIdFilter.
    return -201;
  }

  @Override
  public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
    final Timer.Sample sample = metrics.startTimer();

    return chain.filter(exchange).doFinally(signalType -> {
      final Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
      final String routeId = route != null ? route.getId() : "unknown";
      final String method = exchange.getRequest().getMethod().name();
      final int status = exchange.getResponse().getStatusCode() != null
          ? exchange.getResponse().getStatusCode().value() : 500;
      final String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");

      metrics.stopTimer(sample, routeId, method, status);
      metrics.recordRequest(routeId, method, status, tenantId);
    });
  }
}
