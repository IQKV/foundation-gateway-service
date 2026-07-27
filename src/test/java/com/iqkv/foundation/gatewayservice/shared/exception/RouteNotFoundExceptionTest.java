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

@DisplayName("RouteNotFoundException Tests")
class RouteNotFoundExceptionTest {

  @Test
  @DisplayName("Should create exception with path")
  void shouldCreateExceptionWithPath() {
    // Arrange
    final var path = "/api/v1/unknown";

    // Act
    final var exception = new RouteNotFoundException(path);

    // Assert
    assertThat(exception.getMessage()).isEqualTo("No route found for path: /api/v1/unknown");
    assertThat(exception).isInstanceOf(GatewayServiceException.class);
  }

  @Test
  @DisplayName("Should create exception with message and cause")
  void shouldCreateExceptionWithMessageAndCause() {
    // Arrange
    final var message = "Route lookup failed";
    final var cause = new RuntimeException("Database error");

    // Act
    final var exception = new RouteNotFoundException(message, cause);

    // Assert
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
  }

  @Test
  @DisplayName("Should format message with path correctly")
  void shouldFormatMessageWithPathCorrectly() {
    // Act
    final var exception = new RouteNotFoundException("/api/v1/users/123");

    // Assert
    assertThat(exception.getMessage()).contains("/api/v1/users/123");
    assertThat(exception.getMessage()).startsWith("No route found for path:");
  }
}
