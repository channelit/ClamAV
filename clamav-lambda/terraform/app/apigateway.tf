# resource "aws_api_gateway_rest_api" "app_lambda" {
#   name = "${local.name_prefix}-apigw-${local.name_suffix}"
# }
#
# resource "aws_api_gateway_deployment" "app_lambda" {
#   rest_api_id = aws_api_gateway_rest_api.app_lambda.id
#   triggers = {
#     redeployment = sha1(jsonencode([
#       aws_api_gateway_resource.app_lambda.id,
#       aws_api_gateway_method.app_lambda.id,
#       aws_api_gateway_integration.app_lambda.id
#     ]))
#   }
#   lifecycle {
#     create_before_destroy = true
#   }
#   depends_on = [
#     aws_api_gateway_method.app_lambda,
#     aws_api_gateway_resource.app_lambda,
#     aws_api_gateway_integration.app_lambda
#   ]
# }
#
# resource "aws_api_gateway_stage" "app_lambda" {
#   deployment_id = aws_api_gateway_deployment.app_lambda.id
#   rest_api_id   = aws_api_gateway_rest_api.app_lambda.id
#   stage_name    = var.stage_name
# }
#
# resource "aws_api_gateway_method" "app_lambda" {
#   authorization    = "NONE"
#   http_method      = "GET"
#   resource_id      = aws_api_gateway_resource.app_lambda.id
#   rest_api_id      = aws_api_gateway_rest_api.app_lambda.id
#   api_key_required = false
# }
#
#
# resource "aws_api_gateway_integration" "app_lambda" {
#   rest_api_id             = aws_api_gateway_rest_api.app_lambda.id
#   resource_id             = aws_api_gateway_resource.app_lambda.id
#   http_method             = aws_api_gateway_method.app_lambda.http_method
#   integration_http_method = "POST"
#   type                    = "AWS_PROXY"
#   uri                     = aws_lambda_alias.app_lambda_function_alias.invoke_arn
# }
#
# resource "aws_api_gateway_method_settings" "app_lambda" {
#   method_path = "*/*"
#   rest_api_id = aws_api_gateway_rest_api.app_lambda.id
#   stage_name  = aws_api_gateway_stage.app_lambda.stage_name
#   settings {
#     logging_level = "INFO"
#   }
# }
#
# resource "aws_api_gateway_resource" "app_lambda" {
#   rest_api_id = aws_api_gateway_rest_api.app_lambda.id
#   parent_id   = aws_api_gateway_rest_api.app_lambda.root_resource_id
#   path_part   = "{proxy+}"
# }
#
# data "aws_iam_policy_document" "app_apigw_assume_role" {
#   statement {
#     effect = "Allow"
#     principals {
#       identifiers = ["apigateway.amazonaws.com"]
#       type        = "Service"
#     }
#     actions = ["sts:AssumeRole"]
#   }
# }
#
# resource "aws_iam_role" "app_lambda_cloudwatch" {
#   assume_role_policy = data.aws_iam_policy_document.app_apigw_assume_role.json
#   name               = "${local.name_prefix}-app-lambda-cloudwatch-role-${local.name_suffix}"
# }
#
# data "aws_iam_policy_document" "app_lambda_cloudwatch" {
#   statement {
#     effect  = "Allow"
#     actions = [
#       "logs:CreateLogGroup",
#       "logs:CreateLogStream",
#       "logs:DescribeLogGroups",
#       "logs:DescribeLogStreams",
#       "logs:PutLogEvents",
#       "logs:GetLogEvents",
#       "logs:FilterLogEvents"
#     ]
#     resources = ["*"]
#   }
# }
#
# # resource "aws_iam_role_policy" "app_lambda_cloudwatch" {
# #   name   = "${local.name_prefix}-cw-policy-${local.name_suffix}"
# #   policy = data.aws_iam_policy_document.app_lambda_cloudwatch.json
# #   role   = aws_iam_role.app_lambda_cloudwatch.id
# # }
#
# resource "aws_lambda_permission" "app_lambda_apigw" {
#   statement_id  = "${local.name_prefix}-apigw-permission-${local.name_suffix}"
#   action        = "lambda:InvokeFunction"
#   function_name = aws_lambda_function.app_lambda_function.function_name
#   principal     = "apigateway.amazonaws.com"
#   source_arn    = "${aws_api_gateway_rest_api.app_lambda.execution_arn}/*/*/*"
#   qualifier     = aws_lambda_alias.app_lambda_function_alias.name
# }
#
# resource "aws_api_gateway_account" "app_lambda" {
#   cloudwatch_role_arn = aws_iam_role.app_lambda_cloudwatch.arn
# }
#
#
# resource "aws_cloudwatch_log_group" "app_apigw_log_group" {
#   name              = "API-Gateway-Execution-Logs_${aws_api_gateway_rest_api.app_lambda.id}/${var.stage_name}"
#   retention_in_days = var.log_retention_in_days
# }