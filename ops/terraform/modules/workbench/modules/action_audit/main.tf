module "main" {
  source     = "terraform-google-modules/bigquery/google"
  version    = "~> 4.3"
  dataset_id = var.action_audit_dataset_id
  project_id = var.project_id
  location   = "US"

  # Note: friendly_name is discovered in plan and apply steps, but can't be
  # entered here. Maybe they're just not exposed by the dataset module but the resources are looking
  # for them?
  dataset_name = "Workbench ${title(var.aou_env)} Environment Reporting Data" # exposed as friendly_name in plan
  description  = "Daily output of relational tables and time series views for analysis. Views are provided for general ad-hoc analysis."

  tables = local.tables

  # Note that, when creating this module fom the ground up, it's common to see an error like
  # `Error: googleapi: Error 404: Not found: Table my-project:my_dataset.my_table, notFound`. It seems
  # to be a momentary issue due to the dataset's existence not yet being observable to the table/view
  # create API. So far, it's always worked on a re-run.
  # TODO(jaycarlton) see if there's a way to put a retry on this. I'm not convinced that will work
  #   outside of a resource context (and inside a third-party module).
  views = local.views

  dataset_labels = {
    subsystem         = "reporting"
    terraform_managed = "true"
    aou_env           = var.aou_env
  }

}
