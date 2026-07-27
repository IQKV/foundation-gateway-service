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

package com.iqkv.foundation.gatewayservice.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GatewayConfigurationProperties Tests")
class GatewayConfigurationPropertiesTest {

  @Test
  @DisplayName("Should create properties with default values")
  void shouldCreatePropertiesWithDefaultValues() {
    // Act
    final var properties = new GatewayConfigurationProperties();

    // Assert
    assertThat(properties.getTenancy()).isNotNull();
    assertThat(properties.getIam()).isNotNull();
  }

  @Test
  @DisplayName("Should set and get tenancy properties")
  void shouldSetAndGetTenancyProperties() {
    // Arrange
    final var properties = new GatewayConfigurationProperties();
    final var tenancy = new GatewayConfigurationProperties.Tenancy();
    tenancy.setDefaultTenantKey("default-tenant");

    // Act
    properties.setTenancy(tenancy);

    // Assert
    assertThat(properties.getTenancy()).isEqualTo(tenancy);
    assertThat(properties.getTenancy().getDefaultTenantKey()).isEqualTo("default-tenant");
  }

  @Test
  @DisplayName("Should set and get IAM properties")
  void shouldSetAndGetIamProperties() {
    // Arrange
    final var properties = new GatewayConfigurationProperties();
    final var iam = new GatewayConfigurationProperties.Iam();
    iam.setServiceUrl("http://iam-service:8080");

    // Act
    properties.setIam(iam);

    // Assert
    assertThat(properties.getIam()).isEqualTo(iam);
    assertThat(properties.getIam().getServiceUrl()).isEqualTo("http://iam-service:8080");
  }

  @DisplayName("Tenancy Properties Tests")
  static class TenancyTest {

    @Test
    @DisplayName("Should set and get default tenant key")
    void shouldSetAndGetDefaultTenantKey() {
      // Arrange
      final var tenancy = new GatewayConfigurationProperties.Tenancy();

      // Act
      tenancy.setDefaultTenantKey("my-tenant");

      // Assert
      assertThat(tenancy.getDefaultTenantKey()).isEqualTo("my-tenant");
    }

    @Test
    @DisplayName("Should have null default tenant key initially")
    void shouldHaveNullDefaultTenantKeyInitially() {
      // Act
      final var tenancy = new GatewayConfigurationProperties.Tenancy();

      // Assert
      assertThat(tenancy.getDefaultTenantKey()).isNull();
    }
  }

  @DisplayName("IAM Properties Tests")
  static class IamTest {

    @Test
    @DisplayName("Should set and get service URL")
    void shouldSetAndGetServiceUrl() {
      // Arrange
      final var iam = new GatewayConfigurationProperties.Iam();

      // Act
      iam.setServiceUrl("http://custom-iam:9090");

      // Assert
      assertThat(iam.getServiceUrl()).isEqualTo("http://custom-iam:9090");
    }

    @Test
    @DisplayName("Should have default service URL")
    void shouldHaveDefaultServiceUrl() {
      // Act
      final var iam = new GatewayConfigurationProperties.Iam();

      // Assert
      assertThat(iam.getServiceUrl()).isEqualTo("http://foundation-iam-service:8080");
    }
  }
}
