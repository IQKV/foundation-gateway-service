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

import java.net.InetSocketAddress;
import java.util.List;

import com.iqkv.foundation.gatewayservice.infrastructure.config.GatewayProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuditContextFilter Tests")
class AuditContextFilterTest {

  @Test
  @DisplayName("Should extract IP from X-Forwarded-For and User-Agent from headers")
  void shouldExtractContextFromHeaders() {
    // Arrange
    final var properties = new GatewayProperties(List.of(), "test-gateway");
    final var filter = new AuditContextFilter(properties);
    final var chain = mock(GatewayFilterChain.class);
    when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

    final var request = MockServerHttpRequest.get("/api/test")
        .header("X-Forwarded-For", "192.168.1.1, 10.0.0.1")
        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
        .build();
    final var exchange = MockServerWebExchange.from(request);

    // Act
    filter.filter(exchange, chain).block();

    // Assert
    final var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
    verify(chain).filter(captor.capture());
    final var mutatedHeaders = captor.getValue().getRequest().getHeaders();

    assertThat(mutatedHeaders.getFirst("X-Audit-IP")).isEqualTo("192.168.1.1");
    assertThat(mutatedHeaders.getFirst("X-Audit-UA")).isEqualTo("Mozilla/5.0");
    assertThat(mutatedHeaders.getFirst("X-Audit-Source")).isEqualTo("test-gateway");
  }

  @Test
  @DisplayName("Should use remote address when X-Forwarded-For is missing")
  void shouldFallbackToRemoteAddress() {
    // Arrange
    final var properties = new GatewayProperties(List.of(), "web-gateway");
    final var filter = new AuditContextFilter(properties);
    final var chain = mock(GatewayFilterChain.class);
    when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

    final var request = MockServerHttpRequest.get("/api/test")
        .remoteAddress(new InetSocketAddress("1.2.3.4", 80))
        .build();
    final var exchange = MockServerWebExchange.from(request);

    // Act
    filter.filter(exchange, chain).block();

    // Assert
    final var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
    verify(chain).filter(captor.capture());
    final var mutatedHeaders = captor.getValue().getRequest().getHeaders();

    assertThat(mutatedHeaders.getFirst("X-Audit-IP")).isEqualTo("1.2.3.4");
    assertThat(mutatedHeaders.getFirst("X-Audit-Source")).isEqualTo("web-gateway");
  }
}
