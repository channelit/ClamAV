module "us_east_1" {
  source = "../../app"
  providers = {
    aws = aws.use1
  }
  env_name           = "dev"
  stage_name         = "scan"
  aws_region         = "us-east-1"
  client_secret_name = "client_secrets_scan"
  client_secrets = {
    "1" : {
      "www.google.com" : {
        "auth_type" : "client_credentials",
        "token_url" : "https://google.com/auth",
        "client_id" : "one",
        "client_secret" : "secret_one"
      },
      "ipv4.download..com" : {
        "auth_type" : "client_credentials",
        "token_url" : "https://google.com/auth",
        "client_id" : "one",
        "client_secret" : "secret_one"
      }
    },
    "CLI2" : {
      "www.google.com" : {
        "auth_type" : "client_credentials",
        "token_url" : "https://google.com/auth",
        "client_id" : "two",
        "client_secret" : "secret_two"
      },
      "www.amazon.com" : {
        "auth_type" : "client_credentials",
        "token_url" : "https://google.com/auth",
        "client_id" : "two",
        "client_secret" : "secret_two"
      }
    }
  }
  log_level = ""
  token_url = ""
}