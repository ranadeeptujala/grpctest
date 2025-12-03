# Internal ALB Security Group
resource "aws_security_group" "alb" {
  name        = "${var.project_name}-${var.environment}-alb-sg"
  description = "Security group for internal gRPC ALB"
  vpc_id      = var.vpc_id

  # gRPC over HTTPS (HTTP/2)
  ingress {
    description = "HTTPS from VPC"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.selected.cidr_block]
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-alb-sg"
  }
}

data "aws_vpc" "selected" {
  id = var.vpc_id
}

# Internal Application Load Balancer
resource "aws_lb" "internal" {
  name               = "${var.project_name}-${var.environment}-internal"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = var.private_subnet_ids

  enable_deletion_protection = false
  enable_http2               = true

  tags = {
    Name = "${var.project_name}-${var.environment}-internal-alb"
  }
}

# Target Group for gRPC (HTTP/2)
resource "aws_lb_target_group" "grpc" {
  name             = "${var.project_name}-${var.environment}-grpc"
  port             = var.grpc_port
  protocol         = "HTTP"
  protocol_version = "GRPC"
  vpc_id           = var.vpc_id
  target_type      = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 10
    path                = "/grpc.health.v1.Health/Check"
    port                = "traffic-port"
    protocol            = "HTTP"
    matcher             = "0-99"
  }

  tags = {
    Name = "${var.project_name}-${var.environment}-grpc-tg"
  }
}

# HTTPS Listener (required for HTTP/2/gRPC)
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

  tags = {
    Name = "${var.project_name}-${var.environment}-https-listener"
  }
}
