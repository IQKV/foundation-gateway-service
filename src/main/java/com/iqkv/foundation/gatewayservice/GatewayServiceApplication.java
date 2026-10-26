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

package com.iqkv.foundation.gatewayservice;

import java.util.List;
import java.util.TimeZone;

import com.iqkv.foundation.gatewayservice.infrastructure.config.GatewayConfigurationProperties;
import com.iqkv.foundation.gatewayservice.infrastructure.config.GatewayProperties;
import com.iqkv.foundation.gatewayservice.infrastructure.config.PlatformConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
    GatewayProperties.class,
    PlatformConfigurationProperties.class,
    GatewayConfigurationProperties.Tenancy.class,
    GatewayConfigurationProperties.Iam.class,
    GatewayConfigurationProperties.Billing.class
})
public class GatewayServiceApplication {

  private static final Logger log = LoggerFactory.getLogger(GatewayServiceApplication.class);

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    getStartupVersionMessages().forEach(log::info);
    SpringApplication.run(GatewayServiceApplication.class, args);
  }

  private static List<String> getStartupVersionMessages() {
    final String javaVersion = System.getProperty("java.version");
    final String javaVmName = System.getProperty("java.vm.name");
    final String javaVmVersion = System.getProperty("java.vm.version");
    final String javaVendor = System.getProperty("java.vendor");
    final String javaVendorVersion = System.getProperty("java.vendor.version");
    final String osName = System.getProperty("os.name");
    final String osVersion = System.getProperty("os.version");
    final String osArch = System.getProperty("os.arch");
    return List.of(
        String.format(
            "Running Java %s (%s %s; %s %s) on %s %s (%s)",
            javaVersion, javaVmName, javaVmVersion, javaVendor, javaVendorVersion, osName, osVersion, osArch),
        String.format("Starting %s on Spring Boot %s", applicationBuild(), SpringBootVersion.getVersion())
    );
  }

  /**
   * Reads the build identity from the jar manifest, falling back to the class name when the manifest carries no
   * implementation entries (for example when running from an IDE or from test classes).
   *
   * @return the application name with its version, or the simple class name
   */
  private static String applicationBuild() {
    final Package applicationPackage = GatewayServiceApplication.class.getPackage();
    final String title = applicationPackage.getImplementationTitle();
    final String version = applicationPackage.getImplementationVersion();
    if (title == null || version == null) {
      return GatewayServiceApplication.class.getSimpleName();
    }
    return title + " " + version;
  }
}
