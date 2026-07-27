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

@DisplayName("RateLimitExceededException Tests")
class RateLimitExceededExceptionTest {

  @Test
  @DisplayName("Should create exception with client ID")
  void shouldCreateExceptionWithClientId() {
    // Arrange
    final var clientId = "client-123";

    // Act
    final var exception = new RateLimitExceededException(clientId);

    // Assert
    assertThat(exception.getMessage()).isEqualTo("Rate limit exceeded for client: client-123");
    assertThat(exception).isInstanceOf(GatewayServiceException.class);
  }

  @Test
  @DisplayName("Should create exception with message and cause")
  void shouldCreateExceptionWithMessageAndCause() {
    // Arrange
    final var message = "Rate limit check failed";
    final var cause = new RuntimeException("Redis connection error");

    // Act
    final var exception = new RateLimitExceededException(message, cause);

    // Assert
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
  }

  @Test
  @DisplayName("Should format message with client ID correctly")
  void shouldFormatMessageWithClientIdCorrectly() {
    // Act
    final var exception = new RateLimitExceededException("user-456");

    // Assert
    assertThat(exception.getMessage()).contains("user-456");
    assertThat(exception.getMessage()).startsWith("Rate limit exceeded for client:");
  }
}
