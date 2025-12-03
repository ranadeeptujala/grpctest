# Terraform Infrastructure

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    AWS VPC                                               │
│                                                                                          │
│  ┌───────────────────────────────────────────────────────────────────────────────────┐  │
│  │                              Private Subnets                                       │  │
│  │                                                                                    │  │
│  │  ┌─────────────────────────────────────────────────────────────────────────────┐  │  │
│  │  │                        Internal ALB (gRPC - HTTP/2)                          │  │  │
│  │  │                              Port 443                                        │  │  │
│  │  └─────────────────────────────────┬───────────────────────────────────────────┘  │  │
│  │                                    │                                              │  │
│  │                                    ▼                                              │  │
│  │  ┌─────────────────────────────────────────────────────────────────────────────┐  │  │
│  │  │                           ECS Fargate                                        │  │  │
│  │  │  ┌─────────────────────┐  ┌─────────────────────┐                           │  │  │
│  │  │  │  Task 1             │  │  Task 2             │                           │  │  │
│  │  │  │  gRPC: 9090         │  │  gRPC: 9090         │                           │  │  │
│  │  │  │  Health: 8081       │  │  Health: 8081       │                           │  │  │
│  │  │  └─────────────────────┘  └─────────────────────┘                           │  │  │
│  │  └─────────────────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                                    │  │
│  └────────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              GitHub Actions CI/CD                                        │
│                                                                                          │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐          │
│   │   Push to   │────►│   Build &   │────►│   Push to   │────►│  Deploy to  │          │
│   │    main     │     │    Test     │     │     ECR     │     │     ECS     │          │
│   └─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘          │
│                                                                                          │
│                         Authentication: GitHub OIDC → AWS IAM                           │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

## Prerequisites

1. **AWS Account** with appropriate permissions
2. **ACM Certificate** for the internal ALB (for HTTPS/gRPC)
3. **Terraform** >= 1.0

## Quick Start

### 1. Configure Variables

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars`:

```hcl
aws_region      = "us-east-1"
project_name    = "grpc-server"
environment     = "dev"
github_org      = "your-github-username"
github_repo     = "grpctest"
certificate_arn = "arn:aws:acm:..."
```

### 2. Initialize and Apply

```bash
# Initialize Terraform
terraform init

# Plan
terraform plan

# Apply
terraform apply
```

### 3. Configure GitHub Secrets

After applying, get the IAM role ARN:

```bash
terraform output github_actions_role_arn
```

Add to GitHub repository secrets:
- `AWS_ROLE_ARN` = (output from above)

## Modules

| Module | Description |
|--------|-------------|
| `vpc` | VPC with public/private subnets, NAT Gateway |
| `ecr` | ECR repository for Docker images |
| `iam` | GitHub OIDC provider and IAM role |
| `alb` | Internal ALB for gRPC (HTTP/2) |
| `ecs` | ECS Fargate cluster and service |

## GitHub OIDC Authentication

This setup uses **OpenID Connect (OIDC)** for GitHub Actions to authenticate with AWS:

```
GitHub Actions ──► GitHub OIDC Provider ──► AWS IAM Role ──► AWS Resources
```

**Benefits:**
- No long-lived AWS credentials stored in GitHub
- Fine-grained access control
- Automatic credential rotation

## Outputs

| Output | Description |
|--------|-------------|
| `ecr_repository_url` | ECR repository URL |
| `alb_dns_name` | Internal ALB DNS name |
| `grpc_endpoint` | gRPC endpoint for clients |
| `github_actions_role_arn` | IAM role ARN for GitHub |

## CI/CD Workflows

### `ci-cd.yml`
- Triggered on push to `main`
- Builds Docker image
- Pushes to ECR
- Deploys to ECS

### `terraform.yml`
- Triggered on changes to `terraform/`
- Runs `terraform plan` on PRs
- Runs `terraform apply` on merge to `main`

## Cleanup

```bash
terraform destroy
```

## Security Considerations

1. **Internal ALB** - Not exposed to internet
2. **Private Subnets** - ECS tasks in private subnets
3. **OIDC** - No static credentials
4. **Least Privilege** - IAM roles with minimal permissions
5. **Encryption** - TLS for ALB, encrypted ECR

