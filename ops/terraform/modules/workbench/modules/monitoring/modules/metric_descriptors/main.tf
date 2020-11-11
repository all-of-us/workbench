
locals {
  monitoring_metric_descriptor_paths = [for metric_file in fileset("${path.module}/assets/monitoring_metric_descriptors/", "*.json") : pathexpand(metric_file)]
  monitoring_metric_descriptor_names = [for metric_path in local.monitoring_metric_descriptor_paths : replace(basename(metric_path), ".json", "")]

  metric_tuple = [for metric_path in local.monitoring_metric_descriptor_paths :
    jsondecode(templatefile("${path.module}/assets/monitoring_metric_descriptors/${metric_path}", {
      project_id = var.project_id
      namespace  = var.aou_env
    }))
  ]
  # The map-valued for-expression syntax is flaky. A workaround is to make a list of keys and 0
  # a list of values and just zip them. https://github.com/hashicorp/terraform/issues/20230#issuecomment-461783910
  name_to_monitoring_metric_descriptor = zipmap(local.monitoring_metric_descriptor_names, local.metric_tuple)
}

resource "google_monitoring_metric_descriptor" "basic" {
  provider = google-beta
  for_each = local.name_to_monitoring_metric_descriptor

  description  = each.value.description
  display_name = each.value.displayName
  metric_kind  = each.value.metricKind
  launch_stage = lookup(each.value, "launch_stage", "LAUNCH_STAGE_UNSPECIFIED")
  project      = var.project_id
  type         = each.value.type
  unit         = each.value.unit
  value_type   = each.value.valueType

  # Build a list of label objects for each outer iteration
  dynamic "labels" {
    for_each = each.value.labels
    content {
      key         = lookup(labels.value, "key", null)
      value_type  = lookup(labels.value, "value_type", null)
      description = lookup(labels.value, "description", null)
    }
  }
  metadata {
    sample_period = "60s"
    ingest_delay  = null # each.value.metadata.ingest_delay
  }
}


//resource "google_logging_metric" "basic" {
//
//  filter = ""
//  name = ""
//  metric_descriptor {}
//}
