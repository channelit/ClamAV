resource "aws_vpc_endpoint" "kms" {
  vpc_id              = aws_vpc.main.id
  service_name        = "com.amazonaws.${var.aws_region}.kms"
  vpc_endpoint_type   = "Interface"
  private_dns_enabled = true

  tags = {
    Name = "${local.name_prefix}-kms-${local.name_suffix}"
  }
}

resource "aws_vpc_endpoint_subnet_association" "kms" {
  vpc_endpoint_id = aws_vpc_endpoint.kms.id
  subnet_id       = aws_subnet.private_subnets[0].id
}

resource "aws_vpc_endpoint_security_group_association" "kms" {
  vpc_endpoint_id   = aws_vpc_endpoint.kms.id
  security_group_id = aws_security_group.kms.id
}

resource "aws_vpc_endpoint_policy" "main" {
  vpc_endpoint_id = aws_vpc_endpoint.kms.id

  policy = jsonencode({
    Statement = [
      {
        Action   = ["kms:*"]
        Effect   = "Allow"
        Resource = aws_kms_key.client_secret.arn
        Principal = {
          AWS = aws_iam_role.lambda.arn
        }
      }
    ]
  })
}

resource "aws_security_group" "kms" {
  name        = "${local.name_prefix}-kms-sg-${local.name_suffix}"
  description = "Allow AWS Service connectivity via Interface Endpoints"
  vpc_id      = aws_vpc.main.id
  tags = {
    Name = "${local.name_prefix}-kms-sg-${local.name_suffix}"
  }
}

resource "aws_security_group_rule" "kms-ingress" {
  type              = "ingress"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = [aws_vpc.main.cidr_block]
  security_group_id = aws_security_group.kms.id
}

resource "aws_security_group_rule" "kms-egress" {
  type              = "egress"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.kms.id
}