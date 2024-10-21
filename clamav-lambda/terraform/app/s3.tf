resource "aws_s3_bucket" "main" {
  bucket = "${local.name_prefix}-main-s3-${local.name_suffix}"
  tags = {
    Name = "${local.name_prefix}-main-s3-${local.name_suffix}"
  }
  force_destroy = true
}

resource "aws_s3_bucket_notification" "s3_create_object" {
  bucket = aws_s3_bucket.main.id
  queue {
    events = ["s3:ObjectCreated:*"]
    queue_arn     = aws_sqs_queue.trigger.arn
    filter_prefix = "create_events/"
  }
}