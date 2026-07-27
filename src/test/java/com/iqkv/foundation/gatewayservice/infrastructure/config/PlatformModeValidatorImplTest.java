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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iqkv.foundation.gatewayservice.shared.exception.InvalidPlatformModeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

@DisplayName("PlatformModeValidator Unit Tests")
class PlatformModeValidatorImplTest {

  @Test
  @DisplayName("Should validate successfully when rollout mode is MULTI_TENANT")
  void shouldValidateMultiTenantMode() {
    // Arrange
    final var platformConfig = new PlatformConfigurationProperties(RolloutMode.MULTI_TENANT);
    final var applicationContext = mock(ApplicationContext.class);
    final var validator = new PlatformModeValidatorImpl(platformConfig, applicationContext);

    // Act
    validator.validate();

    // Assert
    assertThat(validator.getMode()).isEqualTo(RolloutMode.MULTI_TENANT);
  }

  @Test
  @DisplayName("Should validate successfully when rollout mode is SINGLE_TENANT")
  void shouldValidateSingleTenantMode() {
    // Arrange
    final var platformConfig = new PlatformConfigurationProperties(RolloutMode.SINGLE_TENANT);
    final var applicationContext = mock(ApplicationContext.class);
    final var validator = new PlatformModeValidatorImpl(platformConfig, applicationContext);

    // Act
    validator.validate();

    // Assert
    assertThat(validator.getMode()).isEqualTo(RolloutMode.SINGLE_TENANT);
  }

  @Test
  @DisplayName("Should throw exception when platform config is null")
  void shouldThrowExceptionWhenConfigIsNull() {
    // Arrange
    final var applicationContext = mock(ApplicationContext.class);
    final var validator = new PlatformModeValidatorImpl(null, applicationContext);

    // Act & Assert
    assertThatThrownBy(validator::validate)
        .isInstanceOf(InvalidPlatformModeException.class)
        .hasMessageContaining("Platform rollout mode is not configured");
  }

  @Test
  @DisplayName("Should throw exception when rollout mode is null")
  void shouldThrowExceptionWhenRolloutModeIsNull() {
    // Arrange
    final var platformConfig = new PlatformConfigurationProperties(null);
    final var applicationContext = mock(ApplicationContext.class);
    final var validator = new PlatformModeValidatorImpl(platformConfig, applicationContext);

    // Act & Assert
    assertThatThrownBy(validator::validate)
        .isInstanceOf(InvalidPlatformModeException.class)
        .hasMessageContaining("Platform rollout mode is not configured");
  }

  @Test
  @DisplayName("Should throw exception when getMode is called before validation")
  void shouldThrowExceptionWhenGetModeCalledBeforeValidation() {
    // Arrange
    final var platformConfig = new PlatformConfigurationProperties(RolloutMode.MULTI_TENANT);
    final var applicationContext = mock(ApplicationContext.class);
    final var validator = new PlatformModeValidatorImpl(platformConfig, applicationContext);

    // Act & Assert
    assertThatThrownBy(validator::getMode)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Platform mode has not been validated yet");
  }

  @Test
  @DisplayName("Should return validated mode after successful validation")
  void shouldReturnValidatedModeAfterValidation() {
    // Arrange
    final var platformConfig = new PlatformConfigurationProperties(RolloutMode.SINGLE_TENANT);
    final var applicationContext = mock(ApplicationContext.class);
    final var validator = new PlatformModeValidatorImpl(platformConfig, applicationContext);

    // Act
    validator.validate();
    final var mode = validator.getMode();

    // Assert
    assertThat(mode).isEqualTo(RolloutMode.SINGLE_TENANT);
  }
}
