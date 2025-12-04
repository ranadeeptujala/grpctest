# 🚀 Complete Deployment Guide

## Overview

This guide covers the complete CI/CD pipeline and infrastructure setup for the gRPC Server.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DEPLOYMENT ARCHITECTURE                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   GitHub                          AWS                                        │
│   ┌─────────────┐                ┌─────────────────────────────────────┐   │
│   │   Push to   │                │              VPC                     │   │
│   │    main     │                │  ┌─────────────────────────────────┐│   │
│   └──────┬──────┘                │  │        Private Subnets          ││   │
│          │                       │  │                                  ││   │
│          ▼                       │  │   ┌─────────┐    ┌─────────┐   ││   │
│   ┌─────────────┐               │  │   │   ALB   │───▶│   ECS   │   ││   │
│   │     CI      │               │  │   │ (gRPC)  │    │ Fargate │   ││   │
│   │ Build/Test  │               │  │   └─────────┘    └─────────┘   ││   │
│   └──────┬──────┘               │  │                       │         ││   │
│          │                       │  │                       ▼         ││   │
│          ▼                       │  │               ┌─────────────┐  ││   │
│   ┌─────────────┐               │  │               │    ECR      │  ││   │
│   │     CD      │───────────────┼──┼──────────────▶│   Images    │  ││   │
│   │ Push & Deploy│              │  │               └─────────────┘  ││   │
│   └─────────────┘               │  └─────────────────────────────────┘│   │
│                                  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
grpctest/
├── .github/workflows/
│   ├── ci.yml              # Continuous Integration
│   ├── cd.yml              # Continuous Deployment
│   ├── lint.yml            # Code Linting
│   └── terraform.yml       # Infrastructure Deployment
├── terraform/
│   ├── main.tf             # Main Terraform config
│   ├── variables.tf        # Input variables
│   ├── outputs.tf          # Output values
│   ├── terraform.tfvars    # Variable values
│   └── modules/
│       ├── vpc/            # VPC, Subnets, NAT
│       ├── ecr/            # Container Registry
│       ├── ecs/            # ECS Cluster & Service
│       ├── alb/            # Application Load Balancer
│       └── iam/            # IAM Roles & OIDC
├── src/                    # Java Source Code
├── Dockerfile              # Container Build
└── pom.xml                 # Maven Config
```

---

## 🔧 Initial Setup (One-Time)

### Step 1: Prerequisites

| Requirement | Description |
|-------------|-------------|
| AWS Account | With admin access |
| GitHub Repo | Source code repository |
| Domain (Optional) | For SSL certificate |
| Terraform | v1.0+ installed locally |

### Step 2: Configure Terraform Variables

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars`:

```hcl
aws_region   = "us-east-2"
project_name = "grpc-server"
environment  = "dev"

vpc_cidr = "10.0.0.0/16"

grpc_port   = 9090
health_port = 8081

# ACM Certificate (create in AWS Console first)
certificate_arn = "arn:aws:acm:us-east-2:YOUR_ACCOUNT:certificate/xxx"

# GitHub OIDC
github_org  = "your-github-username"
github_repo = "grpctest"

# ECS Configuration
ecs_cpu           = 512
ecs_memory        = 1024
ecs_desired_count = 2
```

### Step 3: Create ACM Certificate

1. Go to AWS Certificate Manager (ACM)
2. Request public certificate
3. Domain: `*.yourdomain.com`
4. Validate via DNS (add CNAME to Route53)
5. Wait for "Issued" status
6. Copy ARN to `terraform.tfvars`

### Step 4: Deploy Infrastructure

```bash
cd terraform

# Initialize
terraform init

# Preview changes
terraform plan

# Deploy
terraform apply
```

**Resources Created:**

| Resource | Name |
|----------|------|
| VPC | grpc-server-dev-vpc |
| Subnets | 2 public, 2 private |
| NAT Gateway | For outbound traffic |
| ECR Repository | grpc-server-dev |
| ECS Cluster | grpc-server-dev |
| ECS Service | grpc-server-dev |
| ALB (Internal) | grpc-server-dev-internal |
| IAM OIDC | GitHub Actions integration |

### Step 5: Note Terraform Outputs

```
alb_dns_name = "internal-grpc-server-dev-internal-xxx.us-east-2.elb.amazonaws.com"
ecr_repository_url = "123456789.dkr.ecr.us-east-2.amazonaws.com/grpc-server-dev"
ecs_cluster_name = "grpc-server-dev"
github_actions_role_arn = "arn:aws:iam::123456789:role/grpc-server-dev-github-actions"
```

---

## 🔄 CI Pipeline (ci.yml)

### Trigger
- Push to `main` or `develop`
- Pull requests to `main` or `develop`

### Jobs

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌──────────────┐
│  Build  │────▶│  Test   │────▶│ Package │────▶│ Docker Build │
└─────────┘     └─────────┘     └─────────┘     └──────────────┘
```

| Job | Description | Artifacts |
|-----|-------------|-----------|
| **Build** | Compile Java code | target/ |
| **Test** | Run unit tests | Test reports |
| **Package** | Create JAR | .jar file |
| **Docker Build** | Build image | Docker image |

### Workflow

```yaml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - run: mvn clean compile -B

  test:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - run: mvn test -B

  package:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - run: mvn package -DskipTests -B

  docker-build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: docker/build-push-action@v5
        with:
          push: false
          tags: grpc-server:${{ github.sha }}
```

---

## 🚀 CD Pipeline (cd.yml)

### Trigger
- Push to `main` (excludes docs, *.md, terraform/)
- Manual trigger (workflow_dispatch)

### Jobs

```
┌────────────────┐     ┌────────────────┐
│ Build & Push   │────▶│  Deploy Dev    │
│    to ECR      │     │   (ECS)        │
└────────────────┘     └────────────────┘
```

### Workflow

```yaml
name: CD

on:
  push:
    branches: [main]
    paths-ignore:
      - 'docs/**'
      - '*.md'
      - 'terraform/**'

env:
  AWS_REGION: us-east-2
  AWS_ROLE_ARN: arn:aws:iam::123456789:role/grpc-server-dev-github-actions
  ECR_REPOSITORY: grpc-server-dev
  ECS_CLUSTER: grpc-server-dev
  ECS_SERVICE: grpc-server-dev

permissions:
  id-token: write   # Required for OIDC
  contents: read

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      # 1. Configure AWS (OIDC - no secrets!)
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ env.AWS_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      # 2. Login to ECR
      - uses: aws-actions/amazon-ecr-login@v2

      # 3. Build & Push
      - run: |
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$GITHUB_SHA .
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$GITHUB_SHA
          docker tag ... :latest
          docker push ... :latest

  deploy-dev:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      # 1. Get current task definition
      - run: |
          aws ecs describe-task-definition \
            --task-definition grpc-server-dev \
            > task-definition.json

      # 2. Update image in task definition
      - uses: aws-actions/amazon-ecs-render-task-definition@v1
        with:
          task-definition: task-definition.json
          container-name: grpc-server
          image: ${{ needs.build-and-push.outputs.image }}

      # 3. Deploy to ECS
      - uses: aws-actions/amazon-ecs-deploy-task-definition@v1
        with:
          task-definition: ${{ steps.task-def.outputs.task-definition }}
          service: grpc-server-dev
          cluster: grpc-server-dev
          wait-for-service-stability: true
```

---

## 📦 Deployment Flow

### What Happens on `git push main`

```
1. Developer pushes code to main
                │
                ▼
2. CI Pipeline Runs
   ├── Build Java code
   ├── Run tests
   ├── Package JAR
   └── Build Docker image (test only)
                │
                ▼
3. CD Pipeline Runs
   ├── Build Docker image
   ├── Push to ECR with tags:
   │   ├── {commit-sha}
   │   └── latest
   └── Deploy to ECS
                │
                ▼
4. ECS Deployment
   ├── Create new task definition (revision N+1)
   ├── Update service to use new task def
   ├── Launch new tasks
   ├── Wait for health checks
   ├── Drain old tasks
   └── Complete!
                │
                ▼
5. App Running ✅
   └── Available at ALB endpoint
```

### Timeline

| Step | Duration |
|------|----------|
| CI (Build/Test) | ~2-3 min |
| CD (Push to ECR) | ~2-3 min |
| ECS Deployment | ~3-5 min |
| **Total** | **~7-11 min** |

---

## 🔍 Monitoring & Verification

### Check Deployment Status

```bash
# ECS Service Status
aws ecs describe-services \
  --cluster grpc-server-dev \
  --services grpc-server-dev \
  --query 'services[0].{status:status,running:runningCount,desired:desiredCount}'

# ALB Target Health
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancing:...
```

### View Logs

```bash
# CloudWatch Logs
aws logs tail /ecs/grpc-server-dev --follow
```

### GitHub Actions

- **CI:** https://github.com/{user}/grpctest/actions/workflows/ci.yml
- **CD:** https://github.com/{user}/grpctest/actions/workflows/cd.yml

---

## 🌐 Accessing the Service

### From Within VPC (Other Services)

```java
// Java gRPC Client
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("internal-grpc-server-dev-internal-xxx.us-east-2.elb.amazonaws.com", 443)
    .useTransportSecurity()
    .build();

GreetingServiceGrpc.GreetingServiceBlockingStub stub = 
    GreetingServiceGrpc.newBlockingStub(channel);
```

### DNS Setup (Optional)

Add Route53 alias record:
```
grpc.yourdomain.com → ALB DNS name
```

Then use:
```java
.forAddress("grpc.yourdomain.com", 443)
```

---

## 🔧 Common Operations

### Manual Deployment

```bash
# From GitHub Actions UI
# Go to Actions → CD → Run workflow → Select branch → Run
```

### Rollback

```bash
# Deploy previous task definition revision
aws ecs update-service \
  --cluster grpc-server-dev \
  --service grpc-server-dev \
  --task-definition grpc-server-dev:PREVIOUS_REVISION
```

### Scale Service

```bash
aws ecs update-service \
  --cluster grpc-server-dev \
  --service grpc-server-dev \
  --desired-count 4
```

### View Running Tasks

```bash
aws ecs list-tasks --cluster grpc-server-dev --service-name grpc-server-dev
```

---

## 🛡️ Security

### OIDC Authentication (No Secrets!)

```yaml
# GitHub Actions authenticates to AWS without storing credentials
permissions:
  id-token: write  # Request OIDC token

- uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::xxx:role/grpc-server-dev-github-actions
```

### IAM Role Trust Policy

```json
{
  "Effect": "Allow",
  "Principal": {
    "Federated": "arn:aws:iam::xxx:oidc-provider/token.actions.githubusercontent.com"
  },
  "Action": "sts:AssumeRoleWithWebIdentity",
  "Condition": {
    "StringLike": {
      "token.actions.githubusercontent.com:sub": "repo:your-org/grpctest:*"
    }
  }
}
```

### Network Security

| Component | Access |
|-----------|--------|
| ALB | Internal only (VPC) |
| ECS Tasks | Private subnets |
| ECR | VPC endpoint (optional) |
| NAT Gateway | Outbound only |

---

## 📊 Architecture Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                           VPC (10.0.0.0/16)                      │
│                                                                  │
│  ┌────────────────────────┐  ┌────────────────────────────────┐ │
│  │    Public Subnets      │  │       Private Subnets          │ │
│  │                        │  │                                 │ │
│  │  ┌──────────────────┐  │  │  ┌─────────────────────────┐  │ │
│  │  │   NAT Gateway    │  │  │  │     Internal ALB        │  │ │
│  │  └────────┬─────────┘  │  │  │    (HTTPS:443)          │  │ │
│  │           │            │  │  └───────────┬─────────────┘  │ │
│  └───────────┼────────────┘  │              │                 │ │
│              │               │              ▼                 │ │
│              │               │  ┌─────────────────────────┐  │ │
│              │               │  │      ECS Fargate        │  │ │
│              │               │  │  ┌─────┐    ┌─────┐     │  │ │
│              │               │  │  │Task1│    │Task2│     │  │ │
│              └───────────────┼──│  │:9090│    │:9090│     │  │ │
│                              │  │  └─────┘    └─────┘     │  │ │
│                              │  └─────────────────────────┘  │ │
│                              └────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## ✅ Checklist

### Initial Setup
- [ ] Create AWS Account
- [ ] Create ACM Certificate
- [ ] Configure terraform.tfvars
- [ ] Run `terraform apply`
- [ ] Verify ECS service running

### Per Deployment (Automatic)
- [x] Push to main
- [x] CI runs (build/test)
- [x] CD runs (push/deploy)
- [x] ECS updates
- [x] Health checks pass

---

## 🆘 Troubleshooting

| Issue | Solution |
|-------|----------|
| Health check failing | Check container logs, verify port 9090 |
| OIDC auth failing | Verify IAM role trust policy |
| Image push failing | Check ECR permissions |
| Deployment stuck | Check ECS events, task logs |

```bash
# View ECS task logs
aws logs tail /ecs/grpc-server-dev --follow

# View ECS service events
aws ecs describe-services --cluster grpc-server-dev --services grpc-server-dev \
  --query 'services[0].events[:5]'
```

