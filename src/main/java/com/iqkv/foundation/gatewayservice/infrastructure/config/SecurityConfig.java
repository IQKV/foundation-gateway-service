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

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import reactor.core.publisher.Flux;

/**
 * Reactive security configuration for the API gateway.
 *
 * <p>JWT validation is delegated to Spring Security OAuth2 Resource Server using the
 * JWKS endpoint exposed by the IAM service. Public paths are permitted without a token;
 * all other routes require a valid RS256 JWT.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  private final GatewayProperties gatewayProperties;

  public SecurityConfig(final GatewayProperties gatewayProperties) {
    this.gatewayProperties = gatewayProperties;
  }

  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(final ServerHttpSecurity http) {
    final String[] publicPaths = gatewayProperties.publicPaths().toArray(new String[0]);

    http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
            .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
        )
        .authorizeExchange(auth -> {
          if (publicPaths.length > 0) {
            auth.pathMatchers(publicPaths).permitAll();
          }
          auth.anyExchange().authenticated();
        })
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );

    return http.build();
  }

  /**
   * Extracts the {@code authorities} claim from the IAM-issued JWT and maps each
   * entry to a {@link SimpleGrantedAuthority}.
   */
  private ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
    final var converter = new ReactiveJwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
      final List<String> authorities = jwt.getClaimAsStringList("authorities");
      if (authorities == null || authorities.isEmpty()) {
        return Flux.empty();
      }
      return Flux.fromIterable(authorities)
          .map(SimpleGrantedAuthority::new)
          .map(a -> (GrantedAuthority) a);
    });
    return converter;
  }
}
