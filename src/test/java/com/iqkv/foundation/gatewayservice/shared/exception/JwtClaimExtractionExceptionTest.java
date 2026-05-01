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

package com.iqkv.foundation.gatewayservice.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtClaimExtractionException Tests")
class JwtClaimExtractionExceptionTest {

  @Test
  @DisplayName("Should create exception with claim name")
  void shouldCreateExceptionWithClaimName() {
    // Arrange
    final var claimName = "tenant_id";

    // Act
    final var exception = new JwtClaimExtractionException(claimName);

    // Assert
    assertThat(exception.getMessage()).isEqualTo("Required JWT claim missing or invalid: tenant_id");
    assertThat(exception).isInstanceOf(GatewayServiceException.class);
  }

  @Test
  @DisplayName("Should create exception with message and cause")
  void shouldCreateExceptionWithMessageAndCause() {
    // Arrange
    final var message = "JWT claim extraction failed";
    final var cause = new RuntimeException("Invalid token format");

    // Act
    final var exception = new JwtClaimExtractionException(message, cause);

    // Assert
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
  }

  @Test
  @DisplayName("Should format message with claim name correctly")
  void shouldFormatMessageWithClaimNameCorrectly() {
    // Act
    final var exception = new JwtClaimExtractionException("userId");

    // Assert
    assertThat(exception.getMessage()).contains("userId");
    assertThat(exception.getMessage()).startsWith("Required JWT claim missing or invalid:");
  }
}
