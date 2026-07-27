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

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GatewayProperties Tests")
class GatewayPropertiesTest {

  @Test
  @DisplayName("Should create properties with public paths and audit source")
  void shouldCreatePropertiesWithPublicPaths() {
    // Arrange
    final var publicPaths = List.of("/api/v1/auth/signin", "/api/v1/auth/signup");

    // Act
    final var properties = new GatewayProperties(publicPaths, "test-gateway");

    // Assert
    assertThat(properties.publicPaths()).hasSize(2);
    assertThat(properties.publicPaths()).contains("/api/v1/auth/signin", "/api/v1/auth/signup");
    assertThat(properties.auditSource()).isEqualTo("test-gateway");
  }

  @Test
  @DisplayName("Should create properties with empty list and default audit source when null provided")
  void shouldCreatePropertiesWithEmptyListWhenNullProvided() {
    // Act
    final var properties = new GatewayProperties(null, null);

    // Assert
    assertThat(properties.publicPaths()).isNotNull();
    assertThat(properties.publicPaths()).isEmpty();
    assertThat(properties.auditSource()).isEqualTo("web-gateway");
  }

  @Test
  @DisplayName("Should create properties with empty list and default audit source")
  void shouldCreatePropertiesWithEmptyList() {
    // Act
    final var properties = new GatewayProperties(List.of(), "");

    // Assert
    assertThat(properties.publicPaths()).isEmpty();
    assertThat(properties.auditSource()).isEqualTo("web-gateway");
  }

  @Test
  @DisplayName("Should support equality comparison")
  void shouldSupportEqualityComparison() {
    // Arrange
    final var properties1 = new GatewayProperties(List.of("/api/v1/public"), "source1");
    final var properties2 = new GatewayProperties(List.of("/api/v1/public"), "source1");
    final var properties3 = new GatewayProperties(List.of("/api/v1/private"), "source1");
    final var properties4 = new GatewayProperties(List.of("/api/v1/public"), "source2");

    // Assert
    assertThat(properties1).isEqualTo(properties2);
    assertThat(properties1).isNotEqualTo(properties3);
    assertThat(properties1).isNotEqualTo(properties4);
  }

  @Test
  @DisplayName("Should generate consistent hashCode")
  void shouldGenerateConsistentHashCode() {
    // Arrange
    final var properties1 = new GatewayProperties(List.of("/api/v1/health"), "source1");
    final var properties2 = new GatewayProperties(List.of("/api/v1/health"), "source1");

    // Assert
    assertThat(properties1.hashCode()).isEqualTo(properties2.hashCode());
  }

  @Test
  @DisplayName("Should generate meaningful toString")
  void shouldGenerateMeaningfulToString() {
    // Arrange
    final var properties = new GatewayProperties(List.of("/api/v1/actuator"), "source1");

    // Act
    final var toString = properties.toString();

    // Assert
    assertThat(toString).contains("GatewayProperties");
    assertThat(toString).contains("/api/v1/actuator");
    assertThat(toString).contains("source1");
  }
}
