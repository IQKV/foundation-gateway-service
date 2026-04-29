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

import java.util.Optional;

import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves the tenant context for an incoming request in a mode-aware way.
 *
 * <p>In {@code MULTI_TENANT} mode the tenant key must already be present in the
 * {@code X-Tenant-ID} header (set by {@link JwtContextPropagationFilter} from the
 * JWT {@code tenant_id} claim). If it is absent the policy returns empty and the
 * downstream service is responsible for rejecting the request.
 *
 * <p>In {@code SINGLE_TENANT} mode the policy auto-injects the configured default
 * tenant key whenever no tenant context is already present, including for
 * unauthenticated sign-in requests.
 *
 * <p>Requirements: 8.1, 8.2, 8.3, 16.6
 */
public interface TenantContextResolutionPolicy {

  /**
   * Resolves the tenant key that should be forwarded to downstream services.
   *
   * @param exchange the current server web exchange (headers already sanitized and
   *                 JWT-propagated headers already applied)
   * @return the resolved tenant key, or {@link Optional#empty()} when no tenant
   * context is available and none should be injected (multi-tenant mode
   * without an existing header)
   */
  Optional<String> resolveTenantContext(ServerWebExchange exchange);
}
