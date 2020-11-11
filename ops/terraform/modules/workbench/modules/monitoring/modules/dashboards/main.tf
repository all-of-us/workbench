locals {
  dashboard_files = fileset("${path.module}/assets/dashboards_json", "*.json")
  # String indices for dashboard resource set
  dashboard_names = [for path in local.dashboard_files : replace(basename(path), ".json", "")]

  # Actual JSON strings for each dashboard.
  dashboards = [for full_path in local.dashboard_files :
    templatefile(pathexpand("${path.module}/assets/dashboards_json/${full_path}"), {
      namespace                           = var.aou_env
      metric__labels__buffer_entry_status = join("", ["$", "{metric.labels.BufferEntryStatus}"])
      metric__labels__data_access_level   = join("", ["$", "{metric.labels.DataAccessLevel}"])
      metric__labels__gsuite_domain       = join("", ["$", "{metric.labels.gsuite_domain}"])
    })
  ]

  # Build a map for use with for_each below
  name_to_dashboard = zipmap(local.dashboard_names, local.dashboards)
}

resource "google_monitoring_dashboard" "dashboard" {
  provider       = google-beta
  for_each       = local.name_to_dashboard
  dashboard_json = each.value
}

