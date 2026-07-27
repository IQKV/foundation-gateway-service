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

/**
 * Thrown when no gateway route matches the incoming request path.
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class RouteNotFoundException extends GatewayServiceException {

  public RouteNotFoundException(final String path) {
    super("No route found for path: " + path);
  }

  public RouteNotFoundException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
