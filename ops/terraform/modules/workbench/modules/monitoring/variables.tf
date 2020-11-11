

variable "notification_channel_info" {
  description = <<EOF
I want to use an  array  of objects,  but  as of v0.13, only
sets of strings or  single objects are supported. Next best thing is
a map where the keys are unique, short names for the instances. It's
possible to name them anonymously like _0, _1, etc, but I found it helpful
to use recognizable tokens.
{
    email_channel_1 = {
      display_name = "An email channel"
      type         = "email" # email or
      project_id   = "my-project-id" # belt and suspenders
      labels = {
        email_address = "a@b.co"
      }
    },
    slack_random = {
      display_name = "Slack Random Channel"
      type         = "slack"
      project_id   = "my-project-id" # belt and suspenders
      labels = {
        channel_name = "#random"
        auth_token = "my_token"
      }
    }
EOF

  type = map(
    object({
      display_name = string
      type         = string
      labels       = map(string)
      description  = string
      project_id   = string
  }))
}

variable project_id {
  description = "GCP Project"
  type        = string
}

variable aou_env {
  description = "Short name (all lowercase) of All of Us Workbench deployed environments, e.g. local, test, staging, prod."
  type        = string
}
