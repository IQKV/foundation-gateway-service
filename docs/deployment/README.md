## 📜 Deployment Guide

### Overview

The Foundation Gateway Service is deployed using Helm charts and automated CI/CD pipelines. The service provides API gateway functionality with routing, JWT authentication, rate limiting, CORS, and multi-service orchestration using Spring Cloud Gateway (reactive WebFlux).

### Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- Deployed backend services (foundation-iam-service, foundation-billing-service)

### Environments

| Environment | Namespace      | Purpose                     |
| ----------- | -------------- | --------------------------- |
| Test        | `iqkv-sit-env` | Feature branch testing      |
| Staging     | `iqkv-uat-env` | Pre-production validation   |
| Production  | `iqkv-prd-env` | Live production environment |

### Automated Deployment (CI/CD)

#### Drone Pipeline Overview

<details>
<summary>📋 Pipeline Stages</summary>

The service uses Drone CI/CD pipeline with 10 stages:

1. **VerifyCode** - Code quality, tests, static analysis
2. **PublishArtifacts** - Maven artifacts to Nexus
3. **PublishDockerImage** - Container images to registry
4. **DeployWorkInProgress** - WIP branch auto-deployment
5. **RollbackWorkInProgress** - WIP rollback
6. **PromoteFeatureDeployment** - Feature branch promotion to SIT
7. **RollbackFeatureDeployment** - Feature rollback
8. **PromoteDeployment** - Release promotion to UAT/PRD
9. **RollbackDeployment** - Release rollback
10. **ReleasePackage** - Automated version management

</details>

<details>
<summary>🔐 Required Drone Secrets</summary>

| Secret Name                       | Purpose                           | Used In                                    |
| --------------------------------- | --------------------------------- | ------------------------------------------ |
| `NEXUS_DEPLOYER_USERNAME`         | Nexus repository authentication   | Artifact publishing, dependency resolution |
| `NEXUS_DEPLOYER_PASSWORD`         | Nexus repository authentication   | Artifact publishing, dependency resolution |
| `SONAR_HOST`                      | SonarQube server URL              | Static code analysis                       |
| `SONAR_TOKEN`                     | SonarQube authentication token    | Static code analysis                       |
| `SLACK_WEBHOOK`                   | Slack notifications webhook URL   | Build status notifications                 |
| `GITHUB_API_ACCESS_TOKEN`         | GitHub API access for releases    | Release creation, changelog generation     |
| `SVC_CONTAINER_REGISTRY_USERNAME` | Container registry authentication | Docker image publishing                    |
| `SVC_CONTAINER_REGISTRY_PASSWORD` | Container registry authentication | Docker image publishing                    |
| `HELM_CHARTS_REPOSITORY`          | Helm charts repository URL        | Kubernetes deployments                     |

> **Note**: The gateway has no infrastructure secrets (no database, message broker, or SMTP). All backend service URLs are configured via Helm values files.

</details>

#### Branch Deployment Strategy

| Branch Type | Auto Deploy | Manual Promote | Target Environment |
| ----------- | ----------- | -------------- | ------------------ |
| `wip`       | ✅ SIT      | -              | SIT                |
| `feature/*` | -           | ✅ SIT         | SIT                |
| Tags        | -           | ✅ UAT / PRD   | UAT / Production   |

#### Deployment Commands

The pipeline uses these Helm commands for deployment:

<details>
<summary>Helm Commands</summary>

```bash
# WIP / Feature branches (SIT)
helm upgrade --install --rollback-on-failure --wait --timeout 5m foundation-gateway-service ./ \
  --values ./values.yaml \
  --values ./values-sit.yaml \
  --set image.tag=${DRONE_BRANCH} \
  --namespace iqkv-sit-env \
  --create-namespace

# Production (tagged releases)
helm upgrade --install --rollback-on-failure --wait --timeout 5m foundation-gateway-service ./ \
  --values ./values.yaml \
  --values ./values-prd.yaml \
  --set image.tag=${DRONE_TAG} \
  --namespace iqkv-prd-env \
  --create-namespace
```

</details>

### Manual Deployment

#### Quick Start

```bash
# Clone Helm charts
git clone <HELM_CHARTS_REPOSITORY> charts
cd charts/IQKV/foundation-gateway-service

# Deploy to SIT
helm upgrade --install foundation-gateway-service ./ \
  --values values-sit.yaml \
  --namespace iqkv-sit-env \
  --create-namespace
```

#### Secret Configuration Examples

```bash
# CI/CD secrets only (no runtime application secrets)
drone secret add --repository IQKV/foundation-gateway-service --name NEXUS_DEPLOYER_USERNAME --data "your-nexus-username"
drone secret add --repository IQKV/foundation-gateway-service --name NEXUS_DEPLOYER_PASSWORD --data "your-nexus-password"
```

#### Backend Service Dependencies

The gateway routes requests to these backend services:

- **foundation-iam-service**: Authentication, user management, JWT validation
- **foundation-billing-service**: Billing, subscriptions, Stripe webhooks

Service URLs are configured per environment in values files:

| Environment | IAM Service URI                 | Billing Service URI                 |
| ----------- | ------------------------------- | ----------------------------------- |
| SIT         | `http://foundation-iam-service` | `http://foundation-billing-service` |
| UAT         | `http://foundation-iam-service` | `http://foundation-billing-service` |
| Production  | `http://foundation-iam-service` | `http://foundation-billing-service` |

> All service-to-service communication uses in-cluster DNS (ClusterIP services).

#### Service Configuration

| Setting        | SIT      | UAT          | Production    |
| -------------- | -------- | ------------ | ------------- |
| Replicas       | 1        | 2            | 3             |
| CPU Request    | 500m     | 750m         | 1000m         |
| CPU Limit      | 1000m    | 1500m        | 2000m         |
| Memory Request | 512Mi    | 768Mi        | 1Gi           |
| Memory Limit   | 1Gi      | 1.5Gi        | 2Gi           |
| Autoscaling    | Disabled | 2–5 replicas | 3–10 replicas |
| Ingress        | Enabled  | Enabled      | Enabled       |
| Monitoring     | Enabled  | Enabled      | Enabled       |
| Network Policy | Disabled | Enabled      | Enabled       |

#### CORS Configuration

CORS allowed origins are environment-specific:

- **SIT**: `https://test-app.iqkv.site`, `http://localhost:3000`, `http://localhost:5173`
- **UAT**: `https://staging-app.iqkv.site`, `https://app.iqkv.site`, `https://auth.iqkv.site`
- **Production**: `https://app.iqkv.site`, `https://iqkv.site`, `https://www.iqkv.site`

#### Platform Rollout Mode

The `platform.rolloutMode` value controls multi-tenancy behavior. Valid values: `MULTI_TENANT` (default) | `SINGLE_TENANT`.

In `SINGLE_TENANT` mode, set `platform.defaultTenantKey` to match the IAM service configuration.

> **Important**: This value must be identical across `foundation-iam-service`, `foundation-billing-service`, and `foundation-gateway-service`. Mixed modes are a hard deployment error.

### Monitoring & Health Checks

#### Health Endpoints

- **Liveness**: `/actuator/health/liveness` (port 8081)
- **Readiness**: `/actuator/health/readiness` (port 8081)
- **Metrics**: `/actuator/prometheus` (port 8081)

#### Monitoring Stack

All environments include:

- Prometheus ServiceMonitor
- Alerting rules: service down (>1m), high memory (>80%), high latency (p95 >2s)
- Grafana dashboards

### Troubleshooting

#### Common Issues

1. **Backend Service Connection Failures**

    ```bash
    kubectl logs deployment/foundation-gateway-service -n iqkv-sit-env
    ```

2. **JWT Validation Errors**

    Check IAM service connectivity and JWKS endpoint:

    ```bash
    kubectl exec -it deployment/foundation-gateway-service -n iqkv-sit-env -- \
      curl http://foundation-iam-service-management-http/.well-known/jwks.json
    ```

3. **CORS Issues**

    Verify allowed origins in ConfigMap:

    ```bash
    kubectl describe configmap foundation-gateway-service-config -n iqkv-sit-env | grep CORS
    ```

4. **Routing Issues**

    Check route configuration and backend service availability:

    ```bash
    kubectl get svc -n iqkv-sit-env | grep foundation
    ```

5. **Check Configuration**

    ```bash
    kubectl describe configmap foundation-gateway-service-config -n iqkv-sit-env
    ```

6. **Test Health Endpoints**

    ```bash
    kubectl port-forward deployment/foundation-gateway-service 8081:8081 -n iqkv-sit-env
    curl http://localhost:8081/actuator/health
    ```

#### Missing Backend Services

If deployments fail due to missing backend services:

```bash
# Verify IAM service is running
kubectl get deployment foundation-iam-service -n iqkv-sit-env

# Verify Billing service is running
kubectl get deployment foundation-billing-service -n iqkv-sit-env

# Check service endpoints
kubectl get endpoints -n iqkv-sit-env | grep foundation
```

#### Rollback

```bash
# Rollback to previous Helm revision
helm rollback foundation-gateway-service -n iqkv-prd-env

# Or uninstall completely
helm uninstall foundation-gateway-service -n iqkv-prd-env
```

### Security

- No sensitive secrets stored (stateless gateway, no database or message broker)
- JWT validation via JWKS from IAM service (RS256 public key)
- TLS enabled via cert-manager in all environments (ingress)
- Network policies restrict pod communication in UAT/production
- Non-root container execution (UID 1001)
- Read-only root filesystem in production
- Rate limiting and request size limits enforced at ingress (10MB body size, 300s timeout)

### Ingress Configuration

The gateway is exposed via Nginx Ingress Controller:

| Environment | Host                    | TLS Issuer             | TLS Enabled |
| ----------- | ----------------------- | ---------------------- | ----------- |
| SIT         | `api.iqkv.site`         | letsencrypt-staging    | No          |
| UAT         | `staging-api.iqkv.site` | letsencrypt-production | No          |
| Production  | `api.iqkv.site`         | letsencrypt-production | No          |

> **Note**: `tls.enabled: false` in all environments — TLS termination is handled by the ingress controller with cert-manager annotations, not by explicit TLS configuration in the Ingress resource.
