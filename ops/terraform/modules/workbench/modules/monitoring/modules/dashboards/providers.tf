# Only the beta provider currently supports google_monitoring_dashboard
provider "google-beta" {
  # No credentials file.
  project = var.project_id
  region  = var.region
  zone    = var.zone
}
