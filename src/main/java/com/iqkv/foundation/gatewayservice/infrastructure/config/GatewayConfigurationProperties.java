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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Aggregated gateway configuration properties covering tenancy and IAM integration.
 *
 * <p>Bound from:
 * <pre>
 * iqkv:
 *   tenancy:
 *     default-tenant-key: ${DEFAULT_TENANT_KEY:}
 *   iam:
 *     service-url: ${IAM_SERVICE_URL:http://foundation-iam-service:8080}
 * </pre>
 */
@Validated
public class GatewayConfigurationProperties {

  @NestedConfigurationProperty
  private Tenancy tenancy = new Tenancy();

  @NestedConfigurationProperty
  private Iam iam = new Iam();

  @NestedConfigurationProperty
  private Billing billing = new Billing();

  public Tenancy getTenancy() {
    return tenancy;
  }

  public void setTenancy(final Tenancy tenancy) {
    this.tenancy = tenancy;
  }

  public Iam getIam() {
    return iam;
  }

  public void setIam(final Iam iam) {
    this.iam = iam;
  }

  public Billing getBilling() {
    return billing;
  }

  public void setBilling(final Billing billing) {
    this.billing = billing;
  }

  /**
   * Tenancy-related configuration properties bound from {@code iqkv.tenancy.*}.
   */
  @ConfigurationProperties(prefix = "iqkv.tenancy")
  public static class Tenancy {

    /**
     * The default tenant key used in single-tenant mode.
     * Must match the value configured in the IAM service ({@code iqkv.tenancy.default-tenant-key}).
     * Optional — if absent, the gateway will not auto-inject a tenant key.
     */
    private String defaultTenantKey;

    public String getDefaultTenantKey() {
      return defaultTenantKey;
    }

    public void setDefaultTenantKey(final String defaultTenantKey) {
      this.defaultTenantKey = defaultTenantKey;
    }
  }

  /**
   * IAM service integration properties bound from {@code iqkv.iam.*}.
   */
  @ConfigurationProperties(prefix = "iqkv.iam")
  public static class Iam {

    /**
     * Base URL of the IAM service used for canonical mode verification.
     * Defaults to the internal Kubernetes service address.
     */
    private String serviceUrl = "http://foundation-iam-service:8080";

    public String getServiceUrl() {
      return serviceUrl;
    }

    public void setServiceUrl(final String serviceUrl) {
      this.serviceUrl = serviceUrl;
    }
  }

  /**
   * Billing service integration properties bound from {@code iqkv.billing.*}.
   */
  @ConfigurationProperties(prefix = "iqkv.billing")
  public static class Billing {

    /**
     * Base URL of the billing service used by {@code PlanResolver}.
     */
    private String serviceUrl = "http://foundation-billing-service";

    /**
     * How often the plan data is refreshed.
     * ISO-8601 duration string, e.g. {@code PT10M}.
     */
    private String planRefreshInterval = "PT10M";

    public String getServiceUrl() {
      return serviceUrl;
    }

    public void setServiceUrl(final String serviceUrl) {
      this.serviceUrl = serviceUrl;
    }

    public String getPlanRefreshInterval() {
      return planRefreshInterval;
    }

    public void setPlanRefreshInterval(final String planRefreshInterval) {
      this.planRefreshInterval = planRefreshInterval;
    }
  }
}
