# resource "aws_secretsmanager_secret" "client_id" {
#   name = "${local.name_suffix}/${local.name_prefix}/client_id_1"
# }
#
# resource "aws_secretsmanager_secret_version" "client_id" {
#   secret_id     = aws_secretsmanager_secret.client_id.id
#   secret_string = var.client_id
# }
#
# resource "aws_secretsmanager_secret" "client_secret" {
#   name = "${local.name_suffix}/${local.name_prefix}/client_secret_1"
# }
#
# resource "aws_secretsmanager_secret_version" "client_secret" {
#   secret_id     = aws_secretsmanager_secret.client_secret.id
#   secret_string = var.client_secret
# }
#
# resource "aws_secretsmanager_secret" "token_url" {
#   name = "${local.name_suffix}/${local.name_prefix}/token_url_1"
# }
#
# resource "aws_secretsmanager_secret_version" "token_url" {
#   secret_id     = aws_secretsmanager_secret.token_url.id
#   secret_string = var.token_url
# }
#
# resource "aws_secretsmanager_secret" "client_1" {
#   name = "${local.name_suffix}/${local.name_prefix}/client_1"
# }
#
# resource "aws_secretsmanager_secret_version" "client_1" {
#   secret_id = aws_secretsmanager_secret.client_1.id
#   secret_string = jsonencode(var.client_1)
# }

resource "aws_secretsmanager_secret" "client_secrets" {
  name = "${local.name_suffix}/${local.name_prefix}/${var.client_secret_name}"
}

resource "aws_secretsmanager_secret_version" "client_secrets" {
  secret_id = aws_secretsmanager_secret.client_secrets.id
  secret_string = jsonencode(var.client_secrets)
}