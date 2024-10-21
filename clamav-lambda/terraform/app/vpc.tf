resource "aws_vpc" "main" {
  cidr_block                       = "132.0.0.0/16"
  enable_dns_hostnames             = true
  assign_generated_ipv6_cidr_block = true
  instance_tenancy                 = "default"
  enable_dns_support               = true
  tags = {
    Name = "${local.name_prefix}-main-vpc-${local.name_suffix}"
  }
}

resource "aws_subnet" "public_subnets" {
  count = length(var.public_subnet_cidrs)
  vpc_id                  = aws_vpc.main.id
  cidr_block = element(var.public_subnet_cidrs, count.index)
  availability_zone = element(var.azs, count.index)
  map_public_ip_on_launch = true
  tags = {
    Name = "${local.name_prefix}-public-subnet-${local.name_suffix}-${count.index + 1}"
  }
}

resource "aws_subnet" "private_subnets" {
  count = length(var.private_subnet_cidrs)
  vpc_id                  = aws_vpc.main.id
  cidr_block = element(var.private_subnet_cidrs, count.index)
  availability_zone = element(var.azs, count.index)
  map_public_ip_on_launch = false
  tags = {
    Name = "${local.name_prefix}-private-subnet-${local.name_suffix}-${count.index + 1}"
  }
}

# should add aws_egress_only_internet_gateway for ip6
resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.main.id
  tags = {
    Name = "${local.name_prefix}-igw-${local.name_suffix}"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  tags = {
    Name = "${local.name_prefix}-public-rt-${local.name_suffix}"
  }
}
resource "aws_route" "public_route" {
  route_table_id         = aws_route_table.public.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.igw.id
}

resource "aws_eip" "nat_gateway" {
  tags = {
    Name = "${local.name_prefix}-eip-${local.name_suffix}"
  }
  depends_on = [aws_internet_gateway.igw]
}

resource "aws_nat_gateway" "main" {
  subnet_id = element(aws_subnet.public_subnets[*].id, 0)
  allocation_id = aws_eip.nat_gateway.id
  tags = {
    Name = "${local.name_prefix}-nat-gateway-${local.name_suffix}"
  }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id
  tags = {
    Name = "${local.name_prefix}-private-rt-${local.name_suffix}"
  }
}
resource "aws_route" "private" {
  route_table_id         = aws_route_table.private.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_nat_gateway.main.id
}

resource "aws_route_table_association" "public" {
  count = length(var.public_subnet_cidrs)
  subnet_id = element(aws_subnet.public_subnets[*].id, count.index)
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count = length(var.private_subnet_cidrs)
  subnet_id = element(aws_subnet.private_subnets[*].id, count.index)
  route_table_id = aws_route_table.private.id
}