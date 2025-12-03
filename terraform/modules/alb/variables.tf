variable "project_name" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Environment"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs"
  type        = list(string)
}

variable "grpc_port" {
  description = "gRPC server port"
  type        = number
}

variable "health_port" {
  description = "Health check port"
  type        = number
}

variable "certificate_arn" {
  description = "ACM certificate ARN"
  type        = string
}
