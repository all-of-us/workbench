
module "notification_channels" {
  source                    = "./modules/notification_channels"
  notification_channel_info = var.notification_channel_info
  project_id                = var.project_id
}
