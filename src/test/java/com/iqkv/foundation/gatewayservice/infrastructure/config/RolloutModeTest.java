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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RolloutMode Enum Tests")
class RolloutModeTest {

  @Test
  @DisplayName("Should have MULTI_TENANT value")
  void shouldHaveMultiTenantValue() {
    // Act
    final var mode = RolloutMode.MULTI_TENANT;

    // Assert
    assertThat(mode).isNotNull();
    assertThat(mode.name()).isEqualTo("MULTI_TENANT");
  }

  @Test
  @DisplayName("Should have SINGLE_TENANT value")
  void shouldHaveSingleTenantValue() {
    // Act
    final var mode = RolloutMode.SINGLE_TENANT;

    // Assert
    assertThat(mode).isNotNull();
    assertThat(mode.name()).isEqualTo("SINGLE_TENANT");
  }

  @Test
  @DisplayName("Should have exactly two values")
  void shouldHaveExactlyTwoValues() {
    // Act
    final var values = RolloutMode.values();

    // Assert
    assertThat(values).hasSize(2);
    assertThat(values).contains(RolloutMode.MULTI_TENANT, RolloutMode.SINGLE_TENANT);
  }

  @Test
  @DisplayName("Should parse from string")
  void shouldParseFromString() {
    // Act
    final var multiTenant = RolloutMode.valueOf("MULTI_TENANT");
    final var singleTenant = RolloutMode.valueOf("SINGLE_TENANT");

    // Assert
    assertThat(multiTenant).isEqualTo(RolloutMode.MULTI_TENANT);
    assertThat(singleTenant).isEqualTo(RolloutMode.SINGLE_TENANT);
  }

  @Test
  @DisplayName("Should support equality comparison")
  void shouldSupportEqualityComparison() {
    // Arrange
    final var mode1 = RolloutMode.MULTI_TENANT;
    final var mode2 = RolloutMode.MULTI_TENANT;
    final var mode3 = RolloutMode.SINGLE_TENANT;

    // Assert
    assertThat(mode1).isEqualTo(mode2);
    assertThat(mode1).isNotEqualTo(mode3);
  }
}
