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

package com.iqkv.foundation.gatewayservice;

import java.util.TimeZone;

import com.iqkv.foundation.gatewayservice.infrastructure.config.GatewayConfigurationProperties;
import com.iqkv.foundation.gatewayservice.infrastructure.config.GatewayProperties;
import com.iqkv.foundation.gatewayservice.infrastructure.config.PlatformConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
    GatewayProperties.class,
    PlatformConfigurationProperties.class,
    GatewayConfigurationProperties.Tenancy.class,
    GatewayConfigurationProperties.Iam.class
})
public class GatewayServiceApplication {

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(GatewayServiceApplication.class, args);
  }
}
