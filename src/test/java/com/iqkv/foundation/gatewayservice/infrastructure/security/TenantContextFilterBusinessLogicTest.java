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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iqkv.foundation.gatewayservice.infrastructure.config.GatewayConfigurationProperties;
import com.iqkv.foundation.gatewayservice.infrastructure.config.PlatformConfigurationProperties;
import com.iqkv.foundation.gatewayservice.infrastructure.config.RolloutMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

@DisplayName("TenantContextFilter Business Logic Tests")
class TenantContextFilterBusinessLogicTest {

  @Test
  @DisplayName("Should resolve existing tenant ID in multi-tenant mode")
  void shouldResolveExistingTenantIdInMultiTenantMode() {
    // Arrange
    final var platformConfig = new PlatformConfigurationProperties(RolloutMode.MULTI_TENANT);
    final var tenancyProperties = mock(GatewayConfigurationProperties.Tenancy.class);
    final var filter = new TenantContextFilter(platformConfig, tenancyProperties);

    final var exchange = createExchangeWithTenantId("tenant-123");

    // Act
    final var resolved = filter.resolveTenantContext(exchange);

    // Assert
    assertThat(resolved).isPresent();
    assertThat(resolved.get()).isEqualTo("tenant-123");
  }

  @Test
  @DisplayName("Should return empty in multi-tenant mode when no tenant ID present")
  void shouldReturnEmptyInMultiTenantModeWhenNoTenantId() {
    // Arrange
    final var platformConfig = new PlatformConfigurationProperties(RolloutMode.MULTI_TENANT);
    final var tenancyProperties = mock(GatewayConfigurationProperties.Tenancy.class);
    final var filter = new TenantContextFilter(platformConfig, tenancyProperties);

    final var exchange = createExchangeWithoutTenantId();

    // Act
    final var resolved = filter.resolveTenantContext(exchange);

    // Assert
    assertThat(resolved).isEmpty();
  }

  @Test
  @DisplayName("Should resolve default tenant key in single-tenant mode when no tenant ID present")
  void shouldResolveDefaultTenantKeyInSingleTenantMode() {
    // Arrange
    final var platformConfig = new PlatformConfigurationProperties(RolloutMode.SINGLE_TENANT);
    final var tenancyProperties = mock(GatewayConfigurationProperties.Tenancy.class);
    when(tenancyProperties.getDefaultTenantKey()).thenReturn("default-tenant");
    final var filter = new TenantContextFilter(platformConfig, tenancyProperties);

    final var exchange = createExchangeWithoutTenantId();

    // Act
    final var resolved = filter.resolveTenantContext(exchange);

    // Assert
    assertThat(resolved).isPresent();
    assertThat(resolved.get()).isEqualTo("default-tenant");
  }

  @Test
  @DisplayName("Should preserve existing tenant ID in single-tenant mode")
  void shouldPreserveExistingTenantIdInSingleTenantMode() {
    // Arrange
    final var platformConfig = new PlatformConfigurationProperties(RolloutMode.SINGLE_TENANT);
    final var tenancyProperties = mock(GatewayConfigurationProperties.Tenancy.class);
    when(tenancyProperties.getDefaultTenantKey()).thenReturn("default-tenant");
    final var filter = new TenantContextFilter(platformConfig, tenancyProperties);

    final var exchange = createExchangeWithTenantId("user-tenant-456");

    // Act
    final var resolved = filter.resolveTenantContext(exchange);

    // Assert
    assertThat(resolved).isPresent();
    assertThat(resolved.get()).isEqualTo("user-tenant-456");
  }

  @Test
  @DisplayName("Should return empty when default tenant key is not configured in single-tenant mode")
  void shouldReturnEmptyWhenDefaultTenantKeyNotConfigured() {
    // Arrange
    final var platformConfig = new PlatformConfigurationProperties(RolloutMode.SINGLE_TENANT);
    final var tenancyProperties = mock(GatewayConfigurationProperties.Tenancy.class);
    when(tenancyProperties.getDefaultTenantKey()).thenReturn(null);
    final var filter = new TenantContextFilter(platformConfig, tenancyProperties);

    final var exchange = createExchangeWithoutTenantId();

    // Act
    final var resolved = filter.resolveTenantContext(exchange);

    // Assert
    assertThat(resolved).isEmpty();
  }

  @Test
  @DisplayName("Should return empty when default tenant key is blank in single-tenant mode")
  void shouldReturnEmptyWhenDefaultTenantKeyIsBlank() {
    // Arrange
    final var platformConfig = new PlatformConfigurationProperties(RolloutMode.SINGLE_TENANT);
    final var tenancyProperties = mock(GatewayConfigurationProperties.Tenancy.class);
    when(tenancyProperties.getDefaultTenantKey()).thenReturn("   ");
    final var filter = new TenantContextFilter(platformConfig, tenancyProperties);

    final var exchange = createExchangeWithoutTenantId();

    // Act
    final var resolved = filter.resolveTenantContext(exchange);

    // Assert
    assertThat(resolved).isEmpty();
  }

  @Test
  @DisplayName("Should have correct filter order")
  void shouldHaveCorrectFilterOrder() {
    // Arrange
    final var platformConfig = new PlatformConfigurationProperties(RolloutMode.MULTI_TENANT);
    final var tenancyProperties = mock(GatewayConfigurationProperties.Tenancy.class);
    final var filter = new TenantContextFilter(platformConfig, tenancyProperties);

    // Act
    final var order = filter.getOrder();

    // Assert
    assertThat(order).isEqualTo(-50);
  }

  private ServerWebExchange createExchangeWithTenantId(final String tenantId) {
    final var exchange = mock(ServerWebExchange.class);
    final var request = mock(ServerHttpRequest.class);
    final var headers = new HttpHeaders();
    headers.set("X-Tenant-ID", tenantId);

    when(exchange.getRequest()).thenReturn(request);
    when(request.getHeaders()).thenReturn(headers);

    return exchange;
  }

  private ServerWebExchange createExchangeWithoutTenantId() {
    final var exchange = mock(ServerWebExchange.class);
    final var request = mock(ServerHttpRequest.class);
    final var headers = new HttpHeaders();

    when(exchange.getRequest()).thenReturn(request);
    when(request.getHeaders()).thenReturn(headers);
    when(request.getPath()).thenReturn(mock(org.springframework.http.server.RequestPath.class));

    return exchange;
  }
}
