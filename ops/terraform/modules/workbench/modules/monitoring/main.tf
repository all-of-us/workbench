
module "notification_channels" {
  source                    = "./modules/notification_channels"
  notification_channel_info = var.notification_channel_info
  project_id                = var.project_id
}

module "dashboards" {
  source     = "./modules/dashboards"
  aou_env    = var.aou_env
  project_id = var.project_id
}

module "metric_descriptors" {
  source     = "./modules/metric_descriptors"
  aou_env    = var.aou_env
  project_id = var.project_id
}

module "alert_policies" {
  source = "./modules/alert_policies"

  project_id = var.project_id
  aou_env    = var.aou_env
}
