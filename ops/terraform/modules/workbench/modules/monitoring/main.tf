
# All notification channels for monitoring/alerting
resource "google_monitoring_notification_channel" "channel" {
  for_each     = var.notification_channel_info
  type         = each.value.type
  display_name = each.value.display_name
  labels       = each.value.labels
}
