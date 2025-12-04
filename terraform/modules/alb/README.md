# ALB Module

Internal Application Load Balancer for gRPC traffic.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                              VPC                                     │
│                                                                      │
│   Client (within VPC)                                               │
│         │                                                            │
│         │ HTTPS:443                                                  │
│         ▼                                                            │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │              Security Group (grpc-server-dev-alb-sg)         │   │
│   │                                                              │   │
│   │   Ingress: Port 443 from VPC CIDR only                      │   │
│   │   Egress:  All traffic allowed                               │   │
│   │                                                              │   │
│   │   ┌─────────────────────────────────────────────────────┐   │   │
│   │   │        Internal ALB (grpc-server-dev-internal)       │   │   │
│   │   │                                                      │   │   │
│   │   │   ┌────────────────┐                                │   │   │
│   │   │   │ HTTPS Listener │  SSL Policy: TLS 1.3           │   │   │
│   │   │   │   Port: 443    │  Certificate: ACM              │   │   │
│   │   │   └───────┬────────┘                                │   │   │
│   │   │           │                                          │   │   │
│   │   │           ▼                                          │   │   │
│   │   │   ┌────────────────┐                                │   │   │
│   │   │   │  Target Group  │  Protocol: GRPC                │   │   │
│   │   │   │   Port: 9090   │  Health: /grpc.health.v1...    │   │   │
│   │   │   └───────┬────────┘                                │   │   │
│   │   │           │                                          │   │   │
│   │   └───────────┼──────────────────────────────────────────┘   │   │
│   └───────────────┼──────────────────────────────────────────────┘   │
│                   │                                                   │
│                   ▼                                                   │
│           ┌─────────────┐                                            │
│           │ ECS Fargate │                                            │
│           │  Tasks:9090 │                                            │
│           └─────────────┘                                            │
└─────────────────────────────────────────────────────────────────────┘
```

## Resources Created

| Resource | Type | Description |
|----------|------|-------------|
| `aws_security_group.alb` | Security Group | Firewall rules for ALB |
| `aws_lb.internal` | Application Load Balancer | Internal gRPC load balancer |
| `aws_lb_target_group.grpc` | Target Group | Routes traffic to ECS tasks |
| `aws_lb_listener.https` | HTTPS Listener | Entry point with SSL |

## Usage

```hcl
module "alb" {
  source = "./modules/alb"

  project_name       = "grpc-server"
  environment        = "dev"
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  grpc_port          = 9090
  health_port        = 8081
  certificate_arn    = "arn:aws:acm:us-east-2:123456789:certificate/xxx"
}
```

## Input Variables

| Name | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `project_name` | string | Yes | - | Project name for resource naming |
| `environment` | string | Yes | - | Environment (dev, staging, prod) |
| `vpc_id` | string | Yes | - | VPC ID where ALB will be created |
| `private_subnet_ids` | list(string) | Yes | - | List of private subnet IDs |
| `grpc_port` | number | Yes | - | gRPC server port (e.g., 9090) |
| `health_port` | number | Yes | - | Health check port (e.g., 8081) |
| `certificate_arn` | string | Yes | - | ACM certificate ARN for HTTPS |

## Outputs

| Name | Description | Example |
|------|-------------|---------|
| `dns_name` | ALB DNS name | `internal-grpc-server-dev-xxx.elb.amazonaws.com` |
| `zone_id` | ALB hosted zone ID | `Z3AADJGX6KTTL2` |
| `arn` | ALB ARN | `arn:aws:elasticloadbalancing:...` |
| `target_group_arn` | Target group ARN | `arn:aws:elasticloadbalancing:...` |
| `security_group_id` | ALB security group ID | `sg-0fb9e6c647799caa1` |

## Resource Details

### Security Group

```hcl
resource "aws_security_group" "alb" {
  name        = "${var.project_name}-${var.environment}-alb-sg"
  description = "Security group for internal gRPC ALB"
  vpc_id      = var.vpc_id

  # Inbound: HTTPS from VPC only
  ingress {
    description = "HTTPS from VPC"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.selected.cidr_block]
  }

  # Outbound: All traffic
  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
```

**Security Rules:**

| Direction | Port | Protocol | Source/Dest | Purpose |
|-----------|------|----------|-------------|---------|
| Ingress | 443 | TCP | VPC CIDR | HTTPS/gRPC traffic |
| Egress | All | All | 0.0.0.0/0 | Health checks & responses |

### Application Load Balancer

```hcl
resource "aws_lb" "internal" {
  name               = "${var.project_name}-${var.environment}-internal"
  internal           = true                    # Not internet-facing
  load_balancer_type = "application"           # Layer 7
  security_groups    = [aws_security_group.alb.id]
  subnets            = var.private_subnet_ids

  enable_deletion_protection = false
  enable_http2               = true            # Required for gRPC
}
```

**Key Settings:**

| Setting | Value | Reason |
|---------|-------|--------|
| `internal` | `true` | Private ALB, not exposed to internet |
| `load_balancer_type` | `application` | Layer 7, supports HTTP/2 |
| `enable_http2` | `true` | **gRPC requires HTTP/2** |
| `subnets` | Private subnets | Additional security |

### Target Group (gRPC)

```hcl
resource "aws_lb_target_group" "grpc" {
  name             = "${var.project_name}-${var.environment}-grpc"
  port             = var.grpc_port            # 9090
  protocol         = "HTTP"
  protocol_version = "GRPC"                   # Native gRPC support
  vpc_id           = var.vpc_id
  target_type      = "ip"                     # Required for Fargate

  health_check {
    enabled             = true
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 10
    path                = "/grpc.health.v1.Health/Check"
    port                = "traffic-port"
    protocol            = "HTTP"
    matcher             = "0-99"              # gRPC status codes
  }
}
```

**gRPC Health Check:**

| Setting | Value | Description |
|---------|-------|-------------|
| `protocol_version` | `GRPC` | Native AWS gRPC support |
| `path` | `/grpc.health.v1.Health/Check` | Standard gRPC health protocol |
| `matcher` | `0-99` | gRPC status codes (0 = OK) |
| `target_type` | `ip` | Required for ECS Fargate |

### HTTPS Listener

```hcl
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.internal.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.grpc.arn
  }
}
```

**SSL Configuration:**

| Setting | Value | Description |
|---------|-------|-------------|
| `port` | 443 | Standard HTTPS port |
| `protocol` | HTTPS | Required for HTTP/2/gRPC |
| `ssl_policy` | TLS 1.3 | Latest security standards |
| `certificate_arn` | ACM cert | Your domain certificate |

## Traffic Flow

```
1. gRPC Client (within VPC)
   │
   │ Request: grpc.ranadeepdev.com:443
   ▼
2. DNS Resolution
   │ → internal-grpc-server-dev-xxx.elb.amazonaws.com
   ▼
3. Security Group Check
   │ ✓ Source IP in VPC CIDR? Allow
   │ ✗ External IP? Deny
   ▼
4. HTTPS Listener (Port 443)
   │ - SSL/TLS termination
   │ - Certificate validation
   │ - HTTP/2 protocol
   ▼
5. Target Group Selection
   │ - Protocol: GRPC
   │ - Health check: Healthy targets only
   ▼
6. Load Balancing
   │ - Round robin to healthy ECS tasks
   │ - Sticky sessions (optional)
   ▼
7. ECS Fargate Task (Port 9090)
   │ - gRPC server processes request
   ▼
8. Response back through same path
```

## Why These Choices?

### Why Internal ALB?

```
✅ Internal ALB:
- Only accessible within VPC
- No public IP assigned
- Other microservices can access
- More secure for service-to-service

❌ Internet-facing ALB:
- Exposed to internet
- Requires additional security
- Not needed for internal gRPC
```

### Why HTTPS Required?

```
gRPC → HTTP/2 → TLS/HTTPS

gRPC protocol is built on HTTP/2
HTTP/2 in ALB requires HTTPS
Therefore: gRPC ALB needs HTTPS
```

### Why Protocol Version: GRPC?

```
AWS ALB has native gRPC support:
- Understands gRPC health checks
- Proper gRPC status code handling
- gRPC-specific load balancing
- No need for custom configurations
```

## Connecting from Other Services

### Java gRPC Client

```java
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("internal-grpc-server-dev-xxx.us-east-2.elb.amazonaws.com", 443)
    .useTransportSecurity()  // HTTPS
    .build();

GreetingServiceGrpc.GreetingServiceBlockingStub stub = 
    GreetingServiceGrpc.newBlockingStub(channel);

HelloResponse response = stub.sayHello(
    HelloRequest.newBuilder()
        .setName("World")
        .build()
);
```

### With Custom DNS (Route53)

```java
// After adding Route53 alias record
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("grpc.ranadeepdev.com", 443)
    .useTransportSecurity()
    .build();
```

## Troubleshooting

### Health Check Failing

```bash
# Check target health
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancing:...

# Common causes:
# 1. Container not started yet (wait 60s)
# 2. Security group blocking traffic
# 3. gRPC health service not enabled
# 4. Wrong port configuration
```

### Connection Refused

```bash
# Verify security group allows traffic
aws ec2 describe-security-groups --group-ids sg-xxx

# Check if ALB is in correct subnets
aws elbv2 describe-load-balancers --names grpc-server-dev-internal
```

### Certificate Issues

```bash
# Verify certificate is valid
aws acm describe-certificate --certificate-arn arn:aws:acm:...

# Certificate must be:
# - In same region as ALB
# - Status: ISSUED
# - Domain matches request
```

## Related Modules

| Module | Purpose |
|--------|---------|
| `vpc` | Creates VPC and subnets |
| `ecs` | Creates ECS cluster and service |
| `ecr` | Creates container registry |
| `iam` | Creates IAM roles |

## Cost Considerations

| Component | Pricing |
|-----------|---------|
| ALB | ~$16/month + data transfer |
| ACM Certificate | Free |
| Data Transfer | $0.008/GB (internal) |

## Security Best Practices

1. ✅ **Internal ALB** - Not internet-facing
2. ✅ **VPC-only access** - Security group restricts to VPC CIDR
3. ✅ **TLS 1.3** - Latest encryption standard
4. ✅ **Private subnets** - ALB in private subnets
5. ✅ **ACM certificate** - Managed SSL certificate

