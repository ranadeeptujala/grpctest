# AWS ALB Configuration for gRPC + REST

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AWS VPC                                         │
│                                                                              │
│  ┌─────────────────┐                                                        │
│  │  Public Subnet  │                                                        │
│  │                 │                                                        │
│  │ ┌─────────────┐ │         ┌──────────────────────────────────────────┐  │
│  │ │ Public ALB  │ │         │            Private Subnet                 │  │
│  │ │ (internet-  │ │         │                                          │  │
│  │ │  facing)    │ │  HTTP   │  ┌────────────────────────────────────┐  │  │
│  │ │             │─┼────────►│  │     ECS/EKS/EC2 Instance          │  │  │
│  │ │ HTTPS:443   │ │  :8080  │  │  ┌─────────────────────────────┐  │  │  │
│  │ └─────────────┘ │         │  │  │  Spring Boot Application    │  │  │  │
│  │                 │         │  │  │                             │  │  │  │
│  └─────────────────┘         │  │  │  REST API ──► Port 8080     │  │  │  │
│                              │  │  │  gRPC     ──► Port 9090     │  │  │  │
│  ┌─────────────────┐         │  │  │  Health   ──► Port 8081     │  │  │  │
│  │  Private Subnet │         │  │  │                             │  │  │  │
│  │                 │         │  │  └─────────────────────────────┘  │  │  │
│  │ ┌─────────────┐ │  gRPC   │  └────────────────────────────────────┘  │  │
│  │ │Internal ALB │─┼────────►│                                          │  │
│  │ │             │ │  :9090  │  Other microservices can call            │  │
│  │ │ HTTP/2      │ │         │  gRPC via Internal ALB                   │  │
│  │ └─────────────┘ │         │                                          │  │
│  └─────────────────┘         └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Port Summary

| Port | Protocol | Purpose | Access | ALB Type |
|------|----------|---------|--------|----------|
| 8080 | HTTP | REST API | Public | Internet-facing |
| 9090 | HTTP/2 (gRPC) | gRPC Service | VPC Internal | Internal |
| 8081 | HTTP | Health Checks | Internal | N/A (direct) |

## 1. Public ALB Configuration (REST API - Port 8080)

### Target Group Settings

```yaml
Target Group:
  Name: grpc-rest-tg
  Target Type: IP (for ECS/Fargate) or Instance (for EC2)
  Protocol: HTTP
  Port: 8080
  VPC: your-vpc
  
  Health Check:
    Protocol: HTTP
    Path: /health
    Port: traffic-port  # or 8081 for separate health port
    Healthy threshold: 2
    Unhealthy threshold: 3
    Timeout: 5 seconds
    Interval: 30 seconds
    Success codes: 200
```

### Listener Configuration

```yaml
Listener:
  Protocol: HTTPS
  Port: 443
  SSL Certificate: your-acm-certificate
  Default Action: Forward to grpc-rest-tg
  
  # Optional: HTTP to HTTPS redirect
  HTTP Listener (Port 80):
    Action: Redirect to HTTPS
```

### Security Group (Public ALB)

```yaml
Inbound Rules:
  - Type: HTTPS
    Port: 443
    Source: 0.0.0.0/0
  - Type: HTTP
    Port: 80
    Source: 0.0.0.0/0  # For redirect

Outbound Rules:
  - Type: Custom TCP
    Port: 8080
    Destination: Application Security Group
```

---

## 2. Internal ALB Configuration (gRPC - Port 9090)

### Target Group Settings (gRPC)

```yaml
Target Group:
  Name: grpc-service-tg
  Target Type: IP or Instance
  Protocol: HTTP  # ALB uses HTTP but with HTTP/2
  Protocol Version: gRPC  # ⚠️ IMPORTANT: Select gRPC!
  Port: 9090
  VPC: your-vpc
  
  Health Check:
    Protocol: HTTP
    Path: /grpc.health.v1.Health/Check  # gRPC health check
    Port: traffic-port
    Healthy threshold: 2
    Unhealthy threshold: 2
    Timeout: 5 seconds
    Interval: 10 seconds
    Success codes: 0-99  # gRPC status codes (0 = OK)
```

### Listener Configuration

```yaml
Listener:
  Protocol: HTTPS  # Required for HTTP/2/gRPC
  Port: 443
  SSL Certificate: your-internal-acm-certificate
  Default Action: Forward to grpc-service-tg
```

### Security Group (Internal ALB)

```yaml
Inbound Rules:
  - Type: HTTPS
    Port: 443
    Source: VPC CIDR (e.g., 10.0.0.0/16)

Outbound Rules:
  - Type: Custom TCP
    Port: 9090
    Destination: Application Security Group
```

---

## 3. Application Security Group

```yaml
Inbound Rules:
  # From Public ALB (REST)
  - Type: Custom TCP
    Port: 8080
    Source: Public ALB Security Group
  
  # From Internal ALB (gRPC)
  - Type: Custom TCP
    Port: 9090
    Source: Internal ALB Security Group
  
  # Health checks (if using separate port)
  - Type: Custom TCP
    Port: 8081
    Source: VPC CIDR

Outbound Rules:
  - Type: All traffic
    Destination: 0.0.0.0/0
```

---

## 4. Terraform Example

```hcl
# Public ALB for REST API
resource "aws_lb" "public" {
  name               = "grpc-public-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.public_alb.id]
  subnets            = var.public_subnet_ids
}

resource "aws_lb_target_group" "rest" {
  name        = "grpc-rest-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = "/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 5
    unhealthy_threshold = 3
  }
}

# Internal ALB for gRPC
resource "aws_lb" "internal" {
  name               = "grpc-internal-alb"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.internal_alb.id]
  subnets            = var.private_subnet_ids
}

resource "aws_lb_target_group" "grpc" {
  name             = "grpc-service-tg"
  port             = 9090
  protocol         = "HTTP"
  protocol_version = "GRPC"  # ⚠️ Important for gRPC
  vpc_id           = var.vpc_id
  target_type      = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 10
    matcher             = "0-99"  # gRPC status codes
    path                = "/grpc.health.v1.Health/Check"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 5
    unhealthy_threshold = 2
  }
}

resource "aws_lb_listener" "grpc" {
  load_balancer_arn = aws_lb.internal.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.internal_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.grpc.arn
  }
}
```

---

## 5. ECS Task Definition Example

```json
{
  "family": "grpc-virtual-threads",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "containerDefinitions": [
    {
      "name": "app",
      "image": "your-ecr-repo/grpc-virtual-threads:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp",
          "name": "rest"
        },
        {
          "containerPort": 9090,
          "protocol": "tcp",
          "name": "grpc"
        },
        {
          "containerPort": 8081,
          "protocol": "tcp",
          "name": "health"
        }
      ],
      "environment": [
        {
          "name": "GRPC_SERVER_ADDRESS",
          "value": "static://grpc-internal-alb.your-domain.internal:443"
        }
      ],
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8081/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      },
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/grpc-virtual-threads",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

---

## 6. gRPC Client Configuration for Internal ALB

When other services need to call your gRPC service via the internal ALB:

```yaml
# In calling service's application.yml
grpc:
  client:
    greeting-service:
      address: dns:///grpc-internal-alb.your-domain.internal:443
      negotiation-type: tls
      enable-keep-alive: true
      keep-alive-time: 30s
```

---

## 7. Key Points

### ⚠️ ALB gRPC Requirements

1. **Protocol Version**: Must select "gRPC" when creating target group
2. **HTTPS Required**: ALB requires HTTPS listener for HTTP/2 (gRPC)
3. **Health Check Path**: Use gRPC health check protocol: `/grpc.health.v1.Health/Check`
4. **Success Codes**: Use `0-99` for gRPC status codes (0 = OK)

### 🔒 Security Best Practices

1. **Public ALB**: Only expose REST API (8080)
2. **Internal ALB**: gRPC (9090) accessible only within VPC
3. **Health Port**: Consider separate port (8081) for health checks
4. **TLS Everywhere**: Use TLS for both ALBs

### 🚀 Performance Tips

1. **Connection Pooling**: gRPC clients should maintain persistent connections
2. **Keep-Alive**: Configure keep-alive settings to prevent connection drops
3. **Target Deregistration Delay**: Set appropriately for graceful shutdown

