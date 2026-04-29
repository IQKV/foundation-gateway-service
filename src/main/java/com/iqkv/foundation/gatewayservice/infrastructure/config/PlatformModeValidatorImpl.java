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

import com.iqkv.foundation.gatewayservice.shared.exception.InvalidPlatformModeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates the platform rollout mode configuration at startup for the Gateway service.
 * Runs with highest precedence to ensure validation completes before any request is served.
 *
 * <p>On validation failure the readiness probe is set to {@code REFUSING_TRAFFIC} (DOWN)
 * and a structured error is logged so that the container orchestrator will not route
 * traffic to this instance.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PlatformModeValidatorImpl implements PlatformModeValidator, ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(PlatformModeValidatorImpl.class);

  private final PlatformConfigurationProperties platformConfig;
  private final ApplicationContext applicationContext;
  private RolloutMode validatedMode;

  public PlatformModeValidatorImpl(
      final PlatformConfigurationProperties platformConfig,
      final ApplicationContext applicationContext) {
    this.platformConfig = platformConfig;
    this.applicationContext = applicationContext;
  }

  @Override
  public void run(final ApplicationArguments args) {
    try {
      validate();
    } catch (InvalidPlatformModeException ex) {
      log.error(
          "event=platform_mode_validation_failed service=gateway-service "
          + "configured_mode={} error=\"{}\"",
          platformConfig != null && platformConfig.rolloutMode() != null
              ? platformConfig.rolloutMode()
              : "MISSING",
          ex.getMessage());
      AvailabilityChangeEvent.publish(applicationContext, ReadinessState.REFUSING_TRAFFIC);
      throw ex;
    }
  }

  @Override
  public void validate() {
    if (platformConfig == null || platformConfig.rolloutMode() == null) {
      final String message = "Platform rollout mode is not configured. "
                             + "Please set 'iqkv.platform.rollout-mode' to either 'MULTI_TENANT' or 'SINGLE_TENANT'.";
      log.error("Platform mode validation failed: {}", message);
      throw new InvalidPlatformModeException(message);
    }

    validatedMode = platformConfig.rolloutMode();
    log.info("Platform rollout mode validated successfully: mode={}", validatedMode);
  }

  @Override
  public RolloutMode getMode() {
    if (validatedMode == null) {
      throw new IllegalStateException(
          "Platform mode has not been validated yet. Ensure validate() is called during startup.");
    }
    return validatedMode;
  }
}
