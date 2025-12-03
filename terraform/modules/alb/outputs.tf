output "dns_name" {
  description = "ALB DNS name"
  value       = aws_lb.internal.dns_name
}

output "zone_id" {
  description = "ALB hosted zone ID"
  value       = aws_lb.internal.zone_id
}

output "arn" {
  description = "ALB ARN"
  value       = aws_lb.internal.arn
}

output "target_group_arn" {
  description = "Target group ARN"
  value       = aws_lb_target_group.grpc.arn
}

output "security_group_id" {
  description = "ALB security group ID"
  value       = aws_security_group.alb.id
}
