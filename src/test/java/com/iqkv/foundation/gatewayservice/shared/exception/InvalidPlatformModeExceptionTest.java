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

package com.iqkv.foundation.gatewayservice.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidPlatformModeException Tests")
class InvalidPlatformModeExceptionTest {

  @Test
  @DisplayName("Should create exception with message")
  void shouldCreateExceptionWithMessage() {
    // Arrange
    final var message = "Platform mode is invalid";

    // Act
    final var exception = new InvalidPlatformModeException(message);

    // Assert
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("Should create exception with message and cause")
  void shouldCreateExceptionWithMessageAndCause() {
    // Arrange
    final var message = "Platform mode configuration error";
    final var cause = new IllegalArgumentException("Invalid mode value");

    // Act
    final var exception = new InvalidPlatformModeException(message, cause);

    // Assert
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
  }

  @Test
  @DisplayName("Should preserve cause chain")
  void shouldPreserveCauseChain() {
    // Arrange
    final var rootCause = new RuntimeException("Root cause");
    final var exception = new InvalidPlatformModeException("Error", rootCause);

    // Assert
    assertThat(exception.getCause()).isEqualTo(rootCause);
    assertThat(exception.getCause().getMessage()).isEqualTo("Root cause");
  }
}
