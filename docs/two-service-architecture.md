# Two Service Architecture: REST API + gRPC Consumer

## Overview

Deploy two separate services:
1. **REST API Service** - Public-facing, exposed via ALB (Port 8080)
2. **gRPC Service** - Internal only, consumed within VPC (Port 9090)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    AWS VPC                                               │
│                                                                                          │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │                              Public Subnet                                       │   │
│   │                                                                                  │   │
│   │                         ┌──────────────────────┐                                │   │
│   │        Internet ──────► │    Public ALB        │                                │   │
│   │                         │    (HTTPS:443)       │                                │   │
│   │                         └──────────┬───────────┘                                │   │
│   │                                    │                                            │   │
│   └────────────────────────────────────┼────────────────────────────────────────────┘   │
│                                        │                                                 │
│   ┌────────────────────────────────────┼────────────────────────────────────────────┐   │
│   │                              Private Subnet                                      │   │
│   │                                    │                                            │   │
│   │                                    ▼                                            │   │
│   │   ┌────────────────────────────────────────────────────────────────────────┐   │   │
│   │   │                    SERVICE 1: REST API                                  │   │   │
│   │   │                    ────────────────────                                 │   │   │
│   │   │                                                                         │   │   │
│   │   │   ┌─────────────────────────────────────────────────────────────────┐  │   │   │
│   │   │   │  ECS Fargate / EKS Pod                                          │  │   │   │
│   │   │   │                                                                  │  │   │   │
│   │   │   │  ┌──────────────────────────────────────────────────────────┐   │  │   │   │
│   │   │   │  │  Spring Boot REST Application                            │   │  │   │   │
│   │   │   │  │                                                          │   │  │   │   │
│   │   │   │  │  • Port 8080 (REST endpoints)                           │   │  │   │   │
│   │   │   │  │  • /api/v1/*                                            │   │  │   │   │
│   │   │   │  │  • Contains gRPC Client stub                            │   │  │   │   │
│   │   │   │  │  • Calls gRPC Service internally                        │   │  │   │   │
│   │   │   │  │                                                          │   │  │   │   │
│   │   │   │  └──────────────────────────────────────────────────────────┘   │  │   │   │
│   │   │   │                              │                                   │  │   │   │
│   │   │   └──────────────────────────────┼───────────────────────────────────┘  │   │   │
│   │   │                                  │                                      │   │   │
│   │   └──────────────────────────────────┼──────────────────────────────────────┘   │   │
│   │                                      │                                          │   │
│   │                                      │ gRPC call (HTTP/2)                       │   │
│   │                                      │                                          │   │
│   │                         ┌────────────▼───────────┐                              │   │
│   │                         │    Internal ALB        │                              │   │
│   │                         │    (gRPC - HTTPS:443)  │                              │   │
│   │                         └────────────┬───────────┘                              │   │
│   │                                      │                                          │   │
│   │                                      ▼                                          │   │
│   │   ┌────────────────────────────────────────────────────────────────────────┐   │   │
│   │   │                    SERVICE 2: gRPC SERVICE                              │   │   │
│   │   │                    ───────────────────────                              │   │   │
│   │   │                                                                         │   │   │
│   │   │   ┌─────────────────────────────────────────────────────────────────┐  │   │   │
│   │   │   │  ECS Fargate / EKS Pod                                          │  │   │   │
│   │   │   │                                                                  │  │   │   │
│   │   │   │  ┌──────────────────────────────────────────────────────────┐   │  │   │   │
│   │   │   │  │  Spring Boot gRPC Application                            │   │  │   │   │
│   │   │   │  │                                                          │   │  │   │   │
│   │   │   │  │  • Port 9090 (gRPC server)                              │   │  │   │   │
│   │   │   │  │  • GreetingService implementation                       │   │  │   │   │
│   │   │   │  │  • Business logic, DB access                            │   │  │   │   │
│   │   │   │  │  • Internal only (no public access)                     │   │  │   │   │
│   │   │   │  │                                                          │   │  │   │   │
│   │   │   │  └──────────────────────────────────────────────────────────┘   │  │   │   │
│   │   │   │                                                                  │  │   │   │
│   │   │   └──────────────────────────────────────────────────────────────────┘  │   │   │
│   │   │                                                                         │   │   │
│   │   └─────────────────────────────────────────────────────────────────────────┘   │   │
│   │                                                                                  │   │
│   └──────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Service Breakdown

### Service 1: REST API (Public)

| Property | Value |
|----------|-------|
| **Port** | 8080 |
| **Protocol** | HTTP/REST |
| **Access** | Public (via ALB) |
| **Role** | API Gateway / BFF |
| **Contains** | gRPC Client Stub |

### Service 2: gRPC Service (Internal)

| Property | Value |
|----------|-------|
| **Port** | 9090 |
| **Protocol** | gRPC (HTTP/2) |
| **Access** | VPC Internal only |
| **Role** | Backend Service |
| **Contains** | Business Logic, DB |

---

## Project Structure (Two Separate Projects)

```
workspace/
│
├── rest-api-service/                    # SERVICE 1: REST API
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/example/api/
│   │       ├── RestApiApplication.java
│   │       ├── controller/
│   │       │   └── GreetingController.java
│   │       └── client/
│   │           └── GreetingGrpcClient.java   # gRPC client
│   └── src/main/resources/
│       └── application.yml
│
└── grpc-service/                        # SERVICE 2: gRPC Service  
    ├── pom.xml
    ├── src/main/proto/
    │   └── greeting.proto
    ├── src/main/java/
    │   └── com/example/grpc/
    │       ├── GrpcServiceApplication.java
    │       └── service/
    │           └── GreetingGrpcService.java  # gRPC server
    └── src/main/resources/
        └── application.yml
```

---

## Configuration

### Service 1: REST API - `application.yml`

```yaml
spring:
  application:
    name: rest-api-service
  threads:
    virtual:
      enabled: true

server:
  port: 8080

# gRPC Client - connects to internal gRPC service
grpc:
  client:
    greeting-service:
      # Use Internal ALB DNS name
      address: dns:///grpc-internal-alb.your-domain.internal:443
      negotiation-type: tls
      enable-keep-alive: true
      keep-alive-time: 30s

management:
  endpoints:
    web:
      exposure:
        include: health
```

### Service 2: gRPC Service - `application.yml`

```yaml
spring:
  application:
    name: grpc-service
  threads:
    virtual:
      enabled: true

# No REST server needed (gRPC only)
spring.main.web-application-type: none

# gRPC Server
grpc:
  server:
    port: 9090
    health-service-enabled: true
    reflection-service-enabled: true

management:
  server:
    port: 8081  # Health check port for ALB
```

---

## AWS Resources

### Target Groups

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Target Groups                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  rest-api-tg                                                        │    │
│  │  ────────────                                                       │    │
│  │  Protocol: HTTP                                                     │    │
│  │  Port: 8080                                                         │    │
│  │  Target Type: IP (Fargate)                                          │    │
│  │  Health Check: /health                                              │    │
│  │  Targets: REST API Service instances                                │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  grpc-service-tg                                                    │    │
│  │  ───────────────                                                    │    │
│  │  Protocol: HTTP                                                     │    │
│  │  Protocol Version: gRPC  ← Important!                               │    │
│  │  Port: 9090                                                         │    │
│  │  Target Type: IP (Fargate)                                          │    │
│  │  Health Check: /grpc.health.v1.Health/Check                         │    │
│  │  Success Codes: 0-99                                                │    │
│  │  Targets: gRPC Service instances                                    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Load Balancers

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Load Balancers                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  public-alb (Internet-facing)                                       │    │
│  │  ──────────────────────────────                                     │    │
│  │                                                                      │    │
│  │  Listener: HTTPS:443                                                │    │
│  │    └── Forward to: rest-api-tg                                      │    │
│  │                                                                      │    │
│  │  DNS: api.yourdomain.com                                            │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  internal-alb (Internal)                                            │    │
│  │  ───────────────────────────                                        │    │
│  │                                                                      │    │
│  │  Listener: HTTPS:443 (HTTP/2 for gRPC)                              │    │
│  │    └── Forward to: grpc-service-tg                                  │    │
│  │                                                                      │    │
│  │  DNS: grpc-internal-alb.your-domain.internal (Private Zone)         │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Request Flow

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              Request Flow                                                │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  1. User Request                                                                        │
│     ─────────────                                                                       │
│     GET https://api.yourdomain.com/api/greetings/hello?name=John                        │
│                                                                                          │
│  2. Public ALB                                                                          │
│     ──────────────                                                                      │
│     • Terminates TLS                                                                    │
│     • Routes to REST API Service (port 8080)                                            │
│                                                                                          │
│  3. REST API Service                                                                    │
│     ──────────────────                                                                  │
│     • Receives HTTP request                                                             │
│     • Controller calls gRPC Client                                                      │
│     • gRPC Client connects to Internal ALB                                              │
│                                                                                          │
│  4. Internal ALB                                                                        │
│     ──────────────                                                                      │
│     • Routes gRPC request to gRPC Service (port 9090)                                   │
│     • Maintains HTTP/2 connection                                                       │
│                                                                                          │
│  5. gRPC Service                                                                        │
│     ─────────────                                                                       │
│     • Processes gRPC request                                                            │
│     • Executes business logic                                                           │
│     • Returns gRPC response                                                             │
│                                                                                          │
│  6. Response flows back                                                                 │
│     ─────────────────────                                                               │
│     gRPC Service → Internal ALB → REST API → Public ALB → User                          │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘

Sequence:

┌──────┐      ┌───────────┐      ┌─────────────┐      ┌────────────┐      ┌─────────────┐
│ User │      │Public ALB │      │REST API Svc │      │Internal ALB│      │ gRPC Service│
└──┬───┘      └─────┬─────┘      └──────┬──────┘      └─────┬──────┘      └──────┬──────┘
   │                │                   │                   │                    │
   │  HTTPS :443    │                   │                   │                    │
   │───────────────>│                   │                   │                    │
   │                │   HTTP :8080      │                   │                    │
   │                │──────────────────>│                   │                    │
   │                │                   │                   │                    │
   │                │                   │   gRPC :443       │                    │
   │                │                   │──────────────────>│                    │
   │                │                   │                   │   gRPC :9090       │
   │                │                   │                   │───────────────────>│
   │                │                   │                   │                    │
   │                │                   │                   │   gRPC Response    │
   │                │                   │                   │<───────────────────│
   │                │                   │   gRPC Response   │                    │
   │                │                   │<──────────────────│                    │
   │                │   HTTP Response   │                   │                    │
   │                │<──────────────────│                   │                    │
   │  HTTPS Response│                   │                   │                    │
   │<───────────────│                   │                   │                    │
   │                │                   │                   │                    │
```

---

## Terraform Example

```hcl
# ECS Services
resource "aws_ecs_service" "rest_api" {
  name            = "rest-api-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.rest_api.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.rest_api.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.rest_api.arn
    container_name   = "rest-api"
    container_port   = 8080
  }
}

resource "aws_ecs_service" "grpc_service" {
  name            = "grpc-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.grpc_service.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.grpc_service.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.grpc_service.arn
    container_name   = "grpc-service"
    container_port   = 9090
  }
}

# Security Groups
resource "aws_security_group" "rest_api" {
  name   = "rest-api-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.public_alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "grpc_service" {
  name   = "grpc-service-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port       = 9090
    to_port         = 9090
    protocol        = "tcp"
    security_groups = [aws_security_group.internal_alb.id]
  }

  ingress {
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]  # Health check
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
```

---

## Benefits of This Architecture

| Benefit | Description |
|---------|-------------|
| **Security** | gRPC service not exposed to internet |
| **Scalability** | Scale each service independently |
| **Separation** | Clear separation of concerns |
| **Performance** | gRPC for internal (fast), REST for public (compatible) |
| **Flexibility** | Can add more gRPC consumers later |

---

## Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                    Deployment Summary                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  SERVICE 1: REST API                                            │
│  ─────────────────────                                          │
│  • Public ALB → Port 8080                                       │
│  • Contains gRPC client                                         │
│  • Exposed to internet                                          │
│                                                                  │
│  SERVICE 2: gRPC Service                                        │
│  ─────────────────────────                                      │
│  • Internal ALB → Port 9090                                     │
│  • Contains gRPC server                                         │
│  • VPC internal only                                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

