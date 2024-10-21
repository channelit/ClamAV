resource "aws_kms_key" "client_secret" {
  description         = "${local.name_prefix}-pswd-${local.name_suffix}"
  enable_key_rotation = true
}

resource "aws_kms_alias" "client_secret" {
  name          = "alias/${local.name_prefix}-pswd-${local.name_suffix}"
  target_key_id = aws_kms_key.client_secret.id
}

data "aws_kms_ciphertext" "client_secret" {
  key_id = aws_kms_key.client_secret.id
  plaintext = jsonencode(var.client_secrets)
}

data "aws_iam_policy_document" "browse_kms" {
  statement {
    actions = [
      "kms:Decrypt",
      "kms:DescribeKey",
      "kms:ListAliases",
      "kms:ListKeys"
    ]
    resources = [
      aws_kms_key.client_secret.arn
    ]
    effect = "Allow"
  }
}

resource "aws_iam_policy" "browse_kms" {
  name   = "${local.name_prefix}-kms-policy-${local.name_suffix}"
  policy = data.aws_iam_policy_document.browse_kms.json
  path   = "/"
}