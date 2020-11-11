locals {
  policy_dir   = "${path.module}/assets/alert_policiess"
  policy_paths = [for policy_file in fileset("${local.policy_dir}/", "*.json") : pathexpand(policy_file)]
  policy_names = [for policy_path in local.policy_paths : replace(basename(policy_path), ".json", "")]

  policy_tuple = [for policy_path in local.policy_paths :
    jsondecode(templatefile("${local.policy_dir}/${policy_path}", {
      project_id = var.project_id
      namespace  = var.aou_env
    }))
  ]
  # The map-valued for-expression syntax is flaky. A workaround is to make a list of keys and 0
  # a list of values and just zip them. https://github.com/hashicorp/terraform/issues/20230#issuecomment-461783910
  name_to_alert_policy = zipmap(local.policy_names, local.policy_tuple)
}

resource "google_monitoring_alert_policy" "policy" {
  for_each = local.name_to_alert_policy

  combiner     = each.value.combiner
  display_name = each.value.displayName

  dynamic "conditions" {
    for_each = each.value.conditions
    content {
      display_name = lookup(conditions, "displayName")
      condition_absent {
        duration = lookup(conditions.conditionAbsent, "duration")
        filter   = lookup(conditions.conditionAbsent, "filter")
        trigger {
          percent = lookup(lookup(conditions.conditionAbsent, "trigger"), "percent")
        }
        aggregations {
          alignment_period     = lookup(lookup(conditions.conditionAbsent, "aggregations"), "alignmentPeriod")
          per_series_aligner   = lookup(lookup(conditions.conditionAbsent, "aggregations"), "perSeriesAligner")
          cross_series_reducer = lookup(lookup(conditions.conditionAbsent, "aggregations"), "crossSeriesReducer")
        }
      }
      condition_threshold {
        filter     = "metric.type=\"compute.googleapis.com/instance/disk/write_bytes_count\" AND resource.type=\"gce_instance\""
        duration   = "60s"
        comparison = "COMPARISON_GT"
        aggregations {
          alignment_period   = "60s"
          per_series_aligner = "ALIGN_RATE"
        }
      }
    }
  }
}

