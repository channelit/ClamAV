resource "aws_efs_file_system" "main" {
  creation_token   = "${local.name_prefix}-efs-${local.name_suffix}"
  performance_mode = "generalPurpose"
  throughput_mode  = "bursting"
  encrypted        = "true"
  tags = {
    Name = "${local.name_prefix}-efs-${local.name_suffix}"
  }
}

resource "aws_efs_mount_target" "main" {
  count           = length(var.azs)
  file_system_id  = aws_efs_file_system.main.id
  subnet_id       = aws_subnet.private_subnets[count.index].id
  security_groups = [aws_security_group.lambda.id]
}

resource "aws_efs_access_point" "efs_access_point" {
  file_system_id = aws_efs_file_system.main.id

  posix_user {
    gid = 1000
    uid = 1000
  }
  root_directory {
    path = "/access"
    creation_info {
      owner_gid   = 1000
      owner_uid   = 1000
      permissions = "777"
    }
  }
  tags = {
    Name = "${local.name_prefix}-efs-access-point-${local.name_suffix}"
  }
}


resource "aws_efs_file_system_policy" "main" {
  file_system_id = aws_efs_file_system.main.id

  bypass_policy_lockout_safety_check = true

  policy = <<POLICY
            {
                "Version": "2012-10-17",
                "Id": "${local.name_prefix}-efs-policy-${local.name_suffix}",
                "Statement": [
                    {
                        "Sid": "${local.name_prefix}-efs-policy-stmt-${local.name_suffix}",
                        "Effect": "Allow",
                        "Principal": {
                            "AWS": "*"
                        },
                        "Resource": "${aws_efs_file_system.main.arn}",
                        "Action": [
                            "elasticfilesystem:ClientMount",
                            "elasticfilesystem:ClientWrite",
                            "elasticfilesystem:ClientRootAccess"
                        ],
                        "Condition": {
                            "Bool": {
                                "aws:SecureTransport": "true"
                            }
                        }
                    }
                ]
            }
            POLICY
}