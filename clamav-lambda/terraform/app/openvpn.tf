#resource "aws_vpc" "vpn" {
#  cidr_block = "135.0.0.0/16"
#  tags       = {
#    Name = "${local.name_prefix}-vpn-vpc-${local.name_suffix}"
#  }
#}
#
#resource "aws_subnet" "vpn" {
#  vpc_id            = aws_vpc.vpn.id
#  cidr_block        = aws_vpc.vpn.cidr_block
#  availability_zone = "us-east-1a"
#  tags              = {
#    Name = "${local.name_prefix}-subnet-vpn-${local.name_suffix}"
#  }
#}
#
#resource "aws_security_group" "vpn_access_server" {
#  name        = "${local.name_prefix}-sg-vpn-${local.name_suffix}"
#  description = "Security group for VPN access server"
#  vpc_id      = aws_vpc.vpn.id
#  tags        = {
#    Name = "${local.name_prefix}-sg-vpn-${local.name_suffix}"
#  }
#
#  ingress {
#    protocol    = "tcp"
#    from_port   = 22
#    to_port     = 22
#    cidr_blocks = ["0.0.0.0/0"]
#  }
#
#  ingress {
#    protocol    = "tcp"
#    from_port   = 943
#    to_port     = 943
#    cidr_blocks = ["0.0.0.0/0"]
#  }
#
#  ingress {
#    protocol    = "tcp"
#    from_port   = 443
#    to_port     = 443
#    cidr_blocks = ["0.0.0.0/0"]
#  }
#
#  ingress {
#    protocol    = "udp"
#    from_port   = 1194
#    to_port     = 1194
#    cidr_blocks = ["0.0.0.0/0"]
#  }
#
#  egress {
#    protocol    = "-1"
#    from_port   = 0
#    to_port     = 0
#    cidr_blocks = ["0.0.0.0/0"]
#  }
#}
#
#resource "aws_internet_gateway" "vpn" {
#  vpc_id = aws_vpc.vpn.id
#  tags   = {
#    Name = "${local.name_prefix}-igw-vpn-${local.name_suffix}"
#  }
#}
#
#resource "aws_instance" "vpn_access_server" {
#  ami                         = "ami-06e5a963b2dadea6f"
#  instance_type               = "t2.nano"
#  vpc_security_group_ids      = [aws_security_group.vpn_access_server.id]
#  associate_public_ip_address = true
#  subnet_id                   = aws_subnet.vpn.id
#  key_name                    = "air"
#
#  tags = {
#    Name = "${local.name_prefix}-vpn-${local.name_suffix}"
#  }
#}