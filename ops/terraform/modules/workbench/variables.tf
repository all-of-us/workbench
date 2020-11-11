#
# Environment Variables
#
variable aou_env {
  description = "Short name (all lowercase) of All of Us Workbench deployed environments, e.g. local, test, staging, prod."
  type        = string
}

#
# Provider Variables
#

variable project_id {
  description = "GCP Project"
  type        = string
}

variable "region" {
  description = "GCP region"
  type        = string
  default     = "us-central1"
}

variable "zone" {
  description = "GCP zone"
  type        = string
  default     = "us-central1-c"
}

#
# Reporting
#
variable reporting_dataset_id {
  description = "BigQuery dataset for workbench reporting data."
  type        = string
}

# TODO(jaycarlton) codegen this top-level variables as the union
#   of all modules' variables files.
# List of objects whose values correspond to the google_monitoring_notification_channel
# structure
variable "notification_channels" {
  description = "Email address and Friendly Descriptions for Email Notification Channels"
  default = [{
    display_name = "Anonymous  Notification Channel"
    type         = "" # email or
    labels = {
      email = ""
    }
  }]
}

#
# Monitoring
#

variable "notification_channel_info" {
  description = <<EOF
I want to use an  array  of objects,  but  as of v0.13, only
sets of strings or  single objects are supported.l Hence I'm
passing dummy keys as  the outermost entries, named _0, _1, etc.
TODO(jaycarlton) move to collection of objects wen that's an  actual thing.
{
    _0 = {
      display_name = "An email channel"
      type         = "email" # email or
      labels = {
        email = ""
      }
    },
    _1 = {
      display_name = "Slack Random Channel"
      type         = "slack"
      labels = {
        channel = "#random"
      }
    }
EOF

  type = map(any)
}

#
# Action Audit
#
variable "action_audit_dataset_id" {
  description = "Name of BigQuery dataset for action audit information"
  type        = string
}
