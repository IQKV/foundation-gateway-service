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

package com.iqkv.foundation.gatewayservice.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GatewayProperties Tests")
class GatewayPropertiesTest {

  @Test
  @DisplayName("Should create properties with public paths")
  void shouldCreatePropertiesWithPublicPaths() {
    // Arrange
    final var publicPaths = List.of("/api/v1/auth/signin", "/api/v1/auth/signup");

    // Act
    final var properties = new GatewayProperties(publicPaths);

    // Assert
    assertThat(properties.publicPaths()).hasSize(2);
    assertThat(properties.publicPaths()).contains("/api/v1/auth/signin", "/api/v1/auth/signup");
  }

  @Test
  @DisplayName("Should create properties with empty list when null provided")
  void shouldCreatePropertiesWithEmptyListWhenNullProvided() {
    // Act
    final var properties = new GatewayProperties(null);

    // Assert
    assertThat(properties.publicPaths()).isNotNull();
    assertThat(properties.publicPaths()).isEmpty();
  }

  @Test
  @DisplayName("Should create properties with empty list")
  void shouldCreatePropertiesWithEmptyList() {
    // Act
    final var properties = new GatewayProperties(List.of());

    // Assert
    assertThat(properties.publicPaths()).isEmpty();
  }

  @Test
  @DisplayName("Should support equality comparison")
  void shouldSupportEqualityComparison() {
    // Arrange
    final var properties1 = new GatewayProperties(List.of("/api/v1/public"));
    final var properties2 = new GatewayProperties(List.of("/api/v1/public"));
    final var properties3 = new GatewayProperties(List.of("/api/v1/private"));

    // Assert
    assertThat(properties1).isEqualTo(properties2);
    assertThat(properties1).isNotEqualTo(properties3);
  }

  @Test
  @DisplayName("Should generate consistent hashCode")
  void shouldGenerateConsistentHashCode() {
    // Arrange
    final var properties1 = new GatewayProperties(List.of("/api/v1/health"));
    final var properties2 = new GatewayProperties(List.of("/api/v1/health"));

    // Assert
    assertThat(properties1.hashCode()).isEqualTo(properties2.hashCode());
  }

  @Test
  @DisplayName("Should generate meaningful toString")
  void shouldGenerateMeaningfulToString() {
    // Arrange
    final var properties = new GatewayProperties(List.of("/api/v1/actuator"));

    // Act
    final var toString = properties.toString();

    // Assert
    assertThat(toString).contains("GatewayProperties");
    assertThat(toString).contains("/api/v1/actuator");
  }
}
