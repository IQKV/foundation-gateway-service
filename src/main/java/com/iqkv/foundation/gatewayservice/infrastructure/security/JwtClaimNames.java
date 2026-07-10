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

package com.iqkv.foundation.gatewayservice.infrastructure.security;

/**
 * JWT claim name constants used by the gateway service.
 *
 * <p>These claims are issued by the IAM service ({@code foundation-iam-service}) and consumed
 * here to populate downstream request headers via {@link JwtContextPropagationFilter}.
 *
 * <p>Only the claims actually read by this service are declared here.
 * The authoritative full list lives in {@code JwtClaimNames} inside {@code foundation-iam-service}.
 */
public final class JwtClaimNames {

  /**
   * Unique user identifier (UUID string).
   * Propagated as the {@code X-User-ID} downstream header.
   */
  public static final String USER_ID = "user_id";

  /**
   * User's email address.
   * Propagated as the {@code X-User-Email} downstream header.
   */
  public static final String EMAIL = "email";

  /**
   * Tenant key (8-character NanoID).
   * Propagated as the {@code X-Tenant-ID} downstream header.
   * Absent on platform-admin tokens.
   */
  public static final String TENANT_ID = "tenant_id";

  /**
   * Active subscription plan code.
   * Propagated as the {@code X-Plan-Code} downstream header.
   * Absent when the user has no active subscription.
   */
  public static final String PLAN_CODE = "plan_code";

  /**
   * Granted authority strings, e.g. {@code ["ROLE_USER", "TENANT_OWNER"]}.
   * Propagated as the comma-separated {@code X-User-Authorities} downstream header
   * and mapped to Spring Security {@code GrantedAuthority} instances.
   */
  public static final String AUTHORITIES = "authorities";

  private JwtClaimNames() {
  }
}
