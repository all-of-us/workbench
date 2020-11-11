# All notification channels for Stackdriver alerts (all subsystems).
resource "google_monitoring_notification_channel" "channel" {
  for_each = var.notification_channel_info

  description  = each.value.description
  display_name = each.value.display_name
  labels       = each.value.labels
  project      = each.value.project_id
  type         = each.value.type
}
