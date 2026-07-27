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

@DisplayName("DownstreamServiceException Tests")
class DownstreamServiceExceptionTest {

  @Test
  @DisplayName("Should create exception with service name")
  void shouldCreateExceptionWithServiceName() {
    // Arrange
    final var serviceName = "iam-service";

    // Act
    final var exception = new DownstreamServiceException(serviceName);

    // Assert
    assertThat(exception.getMessage()).isEqualTo("Downstream service unavailable: iam-service");
    assertThat(exception).isInstanceOf(GatewayServiceException.class);
  }

  @Test
  @DisplayName("Should create exception with message and cause")
  void shouldCreateExceptionWithMessageAndCause() {
    // Arrange
    final var message = "Service connection failed";
    final var cause = new RuntimeException("Connection timeout");

    // Act
    final var exception = new DownstreamServiceException(message, cause);

    // Assert
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
  }

  @Test
  @DisplayName("Should format message with service name correctly")
  void shouldFormatMessageWithServiceNameCorrectly() {
    // Act
    final var exception = new DownstreamServiceException("billing-service");

    // Assert
    assertThat(exception.getMessage()).contains("billing-service");
    assertThat(exception.getMessage()).startsWith("Downstream service unavailable:");
  }
}
