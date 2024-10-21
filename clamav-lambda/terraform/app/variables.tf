variable "env_name" {
  description = "environment name"
}
variable "log_retention_in_days" {
  default = 5
}
variable "stage_name" {
  description = "api gateway stage name"
}
variable "public_subnet_cidrs" {
  type = list(string)
  description = "Public Subnet CIDR values"
  default = ["132.0.1.0/24", "132.0.2.0/24", "132.0.3.0/24"]
}
variable "private_subnet_cidrs" {
  type = list(string)
  description = "Private Subnet CIDR values"
  default = ["132.0.4.0/24", "132.0.5.0/24", "132.0.6.0/24"]
}
variable "azs" {
  type = list(string)
  description = "Availability Zones"
  default = ["us-east-1a", "us-east-1b", "us-east-1d"]
}
variable "token_url" {
  type        = string
  description = "token_url"
}
variable "aws_region" {
  description = "aws_region"
}
variable "log_level" {
  description = "log_level"
}
variable "client_secrets" {
  description = "all client secrets as one json"
}
variable "client_secret_name" {
  description = "Client secret name in secrets manager"
}
locals {
  name_prefix  = "scan"
  env = lower(var.env_name)
  name_suffix = lower(var.env_name)
  lambda_image = "122936777114.dkr.ecr.us-east-1.amazonaws.com/clamav:latest"
}