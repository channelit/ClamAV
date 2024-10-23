resource "aws_lambda_function" "app_lambda_function" {
  function_name = "${local.name_prefix}-lambda-${local.name_suffix}"
  role          = aws_iam_role.lambda.arn
  image_uri     = local.lambda_image
  package_type  = "Image"
  memory_size   = "10240"
  timeout       = "900"
  environment {
    variables = {
      S3_BUCKET_NAME          = aws_s3_bucket.main.bucket
      EFS_PATH                = "/mnt/lambda"
      folder                  = "/mnt/lambda"
      REPORT_SQS_URL          = aws_sqs_queue.report.url
      KMS_ENDPOINT            = "https://${aws_vpc_endpoint.kms.arn}"
      #       KEY_CLIENT_ID           = aws_secretsmanager_secret.client_id.name
      #       KEY_TOKEN_URL           = aws_secretsmanager_secret.token_url.name
      #       KEY_CLIENT_SECRET       = aws_secretsmanager_secret.client_secrets.name
      CLIENT_SECRET_NAME      = aws_secretsmanager_secret.client_secrets.name
      SECRET_MANAGER_ENDPOINT = "https://secretsmanager.us-east-1.amazonaws.com"
      POWERTOOLS_SERVICE_NAME = "${local.name_prefix}-lambda-${local.name_suffix}"
      POWERTOOLS_LOG_LEVEL    = var.log_level
    }
  }
  vpc_config {
    security_group_ids = [aws_security_group.lambda.id,]
    subnet_ids = flatten([[aws_subnet.private_subnets.*.id]])
  }
  ephemeral_storage {
    size = 10240
  }
  file_system_config {
    arn              = aws_efs_access_point.efs_access_point.arn
    local_mount_path = "/mnt/lambda"
  }
  tracing_config {
    mode = "Active"
  }
  source_code_hash = null_resource.update_image.triggers.dir_sha
}

# resource "aws_lambda_alias" "app_lambda_function_alias" {
#   name             = "${local.name_prefix}-lambda-alias-${local.name_suffix}"
#   function_name    = aws_lambda_function.app_lambda_function.function_name
#   function_version = aws_lambda_function.app_lambda_function.version
# }

resource "null_resource" "update_image" {
  triggers = {
    dir_sha = sha1(join("", [for f in fileset("../../src", "*") : filesha1("../../src/${f}")]))
  }
}

resource "aws_iam_role" "lambda" {
  name               = "${local.name_prefix}-lambda-iam-${local.name_suffix}"
  path               = "/system/"
  assume_role_policy = data.aws_iam_policy_document.app_lambda_assume_role.json
}

data "aws_iam_policy_document" "app_lambda_assume_role" {
  statement {
    actions = [
      "sts:AssumeRole"
    ]
    principals {
      identifiers = [
        "lambda.amazonaws.com"
      ]
      type = "Service"
    }
  }
}

data "aws_iam_policy_document" "app_lambda_policy" {
  statement {
    actions = [
      "es:ESHttp*"
    ]
    effect = "Allow"
    resources = ["*"]
  }
  statement {
    actions = [
      "ec2:DescribeNetworkInterfaces",
      "ec2:CreateNetworkInterface",
      "ec2:DeleteNetworkInterface",
      "ec2:DescribeInstances",
      "ec2:AttachNetworkInterface"
    ]
    effect = "Allow"
    resources = [
      "*"
    ]
  }
  statement {
    actions = [
      "s3:**"
    ]
    effect = "Allow"
    resources = ["arn:aws:s3:::*"]
  }
  statement {
    actions = [
      "sqs:SendMessage",
      "sqs:GetQueueUrl"
    ]
    effect = "Allow"
    resources = [aws_sqs_queue.report.arn]
  }
  statement {
    actions = [
      "sqs:ReceiveMessage",
      "sqs:GetQueueUrl",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes"
    ]
    effect = "Allow"
    resources = [aws_sqs_queue.trigger.arn]
  }
  statement {
    actions = [
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret"
    ]
    effect = "Allow"
    resources = ["*"]
  }
}

resource "aws_iam_policy" "app_lambda_policy" {
  name   = "${local.name_prefix}-lambda-policy-${local.name_suffix}"
  path   = "/"
  policy = data.aws_iam_policy_document.app_lambda_policy.json
}

resource "aws_iam_role_policy_attachment" "app_lambda_xray_access" {
  role       = aws_iam_role.lambda.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXrayFullAccess"
}

resource "aws_iam_role_policy_attachment" "app_lambda_basic_exec_role" {
  role       = aws_iam_role.lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "app_lambda_role_permissions" {
  role       = aws_iam_role.lambda.name
  policy_arn = aws_iam_policy.app_lambda_policy.arn
}
resource "aws_iam_role_policy_attachment" "app_lambda_role_kms_permissions" {
  role       = aws_iam_role.lambda.name
  policy_arn = aws_iam_policy.browse_kms.arn
}
resource "aws_cloudwatch_log_group" "app_lambda_log_group" {
  name              = "/aws/lambda/${aws_lambda_function.app_lambda_function.function_name}"
  retention_in_days = var.log_retention_in_days
}

resource "aws_security_group" "lambda" {
  name   = "${local.name_prefix}-lambda-sg-${local.name_suffix}"
  vpc_id = aws_vpc.main.id
}

resource "aws_vpc_security_group_ingress_rule" "lambda" {
  count = length(var.private_subnet_cidrs)
  security_group_id = aws_security_group.lambda.id
  from_port         = 443
  to_port           = 443
  cidr_ipv4 = element(var.private_subnet_cidrs, count.index)
  ip_protocol       = "tcp"
  description       = "Allow subnet tcp ingress"
}

resource "aws_vpc_security_group_ingress_rule" "efs_access" {
  count = length(var.private_subnet_cidrs)
  security_group_id = aws_security_group.lambda.id
  from_port         = 2049
  to_port           = 2049
  cidr_ipv4 = element(var.private_subnet_cidrs, count.index)
  ip_protocol       = "tcp"
  description       = "Allow efs ingress"
}

resource "aws_vpc_security_group_egress_rule" "lambda" {
  security_group_id = aws_security_group.lambda.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
  description       = "Allow all egress"
}

data "aws_iam_policy" "lambda_execute" {
  arn = "arn:aws:iam::aws:policy/AWSLambdaExecute"
}

data "aws_iam_policy" "lambda_vpc" {
  arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"
}

data "aws_iam_policy" "efs_full_access" {
  arn = "arn:aws:iam::aws:policy/AmazonElasticFileSystemFullAccess"
}

resource "aws_iam_role" "lambda_efs" {
  name               = "${local.name_prefix}-lambda-efs-role-${local.name_suffix}"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "lambda_execute_policy" {
  role       = aws_iam_role.lambda_efs.name
  policy_arn = data.aws_iam_policy.lambda_execute.arn
}

resource "aws_iam_role_policy_attachment" "lambda_vpc_policy" {
  role       = aws_iam_role.lambda_efs.name
  policy_arn = data.aws_iam_policy.lambda_vpc.arn
}

resource "aws_iam_role_policy_attachment" "lambda_efs_full_policy" {
  role       = aws_iam_role.lambda_efs.name
  policy_arn = data.aws_iam_policy.efs_full_access.arn
}

resource "aws_lambda_event_source_mapping" "lambda" {
  event_source_arn = aws_sqs_queue.trigger.arn
  function_name    = aws_lambda_function.app_lambda_function.arn
}