resource "aws_vpc_endpoint" "secretsmanager" {
  vpc_id              = aws_vpc.main.id
  service_name        = "com.amazonaws.${var.aws_region}.secretsmanager"
  vpc_endpoint_type   = "Interface"
  private_dns_enabled = true

  tags = {
    Name = "${local.name_prefix}-secretsmanager-${local.name_suffix}"
  }
}

resource "aws_vpc_endpoint_subnet_association" "secretsmanager" {
  vpc_endpoint_id = aws_vpc_endpoint.secretsmanager.id
  subnet_id       = aws_subnet.private_subnets[0].id
}

resource "aws_vpc_endpoint_security_group_association" "secretsmanager" {
  vpc_endpoint_id   = aws_vpc_endpoint.secretsmanager.id
  security_group_id = aws_security_group.secretsmanager.id
}

resource "aws_vpc_endpoint_policy" "secretsmanager" {
  vpc_endpoint_id = aws_vpc_endpoint.secretsmanager.id
  policy = jsonencode({
    Statement = [
      {
        Action   = ["secretsmanager:*"]
        Effect   = "Allow"
        Resource = [
          aws_secretsmanager_secret.client_secrets.arn
#           aws_secretsmanager_secret.token_url.arn,
#           aws_secretsmanager_secret.client_id.arn,
#           aws_secretsmanager_secret.client_1.arn
        ]
        Principal = {
          AWS = aws_iam_role.lambda.arn
        }
      },
      {
        Action   = ["kms:Decrypt"]
        Effect   = "Allow"
        Resource = aws_kms_key.client_secret.arn
        Principal = {
          AWS = aws_iam_role.lambda.arn
        }
      }
    ]
  })
}

resource "aws_security_group" "secretsmanager" {
  name        = "${local.name_prefix}-secretsmanager-sg-${local.name_suffix}"
  description = "Allow AWS Service connectivity via Interface Endpoints"
  vpc_id      = aws_vpc.main.id
  tags = {
    Name = "${local.name_prefix}-secretsmanager-sg-${local.name_suffix}"
  }
}

resource "aws_security_group_rule" "secretsmanager-ingress" {
  type              = "ingress"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = [aws_vpc.main.cidr_block]
  security_group_id = aws_security_group.secretsmanager.id
}

resource "aws_security_group_rule" "secretsmanager-egress" {
  type              = "egress"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.secretsmanager.id
}