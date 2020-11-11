# All notification channels for Stackdriver alerts (all subsystems). Since
# most of these are deployment-dependent, all the info is passed via a varriable
# rather than stored in json files.
resource "google_monitoring_notification_channel" "channel" {
  provider = google
  for_each = var.notification_channel_info

  description  = each.value.description
  display_name = each.value.display_name
  labels       = each.value.labels
  project      = each.value.project_id
  type         = each.value.type
}
