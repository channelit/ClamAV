resource "aws_sqs_queue" "report" {
  name                      = "${local.name_prefix}-report-${local.name_suffix}"
  delay_seconds             = 90
  max_message_size          = 2048
  message_retention_seconds = 86400
  receive_wait_time_seconds = 10
}

resource "aws_sqs_queue" "trigger" {
  name                       = "${local.name_prefix}-trigger-${local.name_suffix}"
  delay_seconds              = 90
  max_message_size           = 2048
  message_retention_seconds  = 86400
  receive_wait_time_seconds  = 10
  visibility_timeout_seconds = 900
}

data "aws_iam_policy_document" "s3_create_object_events" {
  statement {
    sid    = "First"
    effect = "Allow"

    principals {
      type = "*"
      identifiers = ["*"]
    }

    actions = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.trigger.arn]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [aws_s3_bucket.main.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "s3_create_object_events" {
  queue_url = aws_sqs_queue.trigger.id
  policy    = data.aws_iam_policy_document.s3_create_object_events.json
}