
// Module for creating an instance of the scratch AoU RW Environment
module "reporting" {
  source = "./modules/reporting"

  # reporting
  aou_env              = var.aou_env
  reporting_dataset_id = var.reporting_dataset_id

  # provider
  project_id = var.project_id
}

# Stackdriver Alerting
module "monitoring" {
  source                    = "./modules/monitoring"
  project_id                = var.project_id
  notification_channel_info = var.notification_channel_info
}
