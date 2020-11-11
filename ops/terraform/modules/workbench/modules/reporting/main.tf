locals {
  #
  # Tables
  #

  # Description attribute for each table. If absent, no description is set (null).
  table_to_description = {
    cohort      = "Workbench Cohorts, including Uncompressed JSON Criteria"
    user        = "All Workbench users at each snapshot, including disabled or incompletely registered accounts."
    institution = "Institutions represented by workbench users in an official affiliation"
    workspace   = "Workbench Workspaces (Active Only). Includes all user-supplied data about the research being conducted in each workspace."
  }

  # Values that don't ever change set for this dataset.
  TABLE_CONSTANTS = {
    time_partitioning = null
    expiration_time   = null
    clustering        = []
    labels = {
      terraform_managed = "true"
    }
  }

  TABLE_SCHEMA_SUFFIX = ".json"
  #  The module path is in a hidden directory under the running directory of the calling module, but
  # our view files and schemas for BigQuery are in whatever directory this file is located, and apparently
  # don't get loaded into that dir by default.
  table_schema_filenames = fileset(pathexpand("${path.module}/assets/schemas"), "*.json")
  // fileset() doesn't have an option to output full paths, so we need to re-expand them
  table_schema_paths = [for file_name in local.table_schema_filenames : pathexpand("${path.module}/assets/schemas/${file_name}")]

  # Build a vector of objects, one for each table
  table_inputs = [for full_path in local.table_schema_paths : {
    schema = full_path
    # TODO(jaycarlton) I do not yet see a way around doing the replacement twice, as it's not possible
    #   to refer to other values in the same object when defining it.
    table_id    = replace(basename(full_path), local.TABLE_SCHEMA_SUFFIX, "")
    description = lookup(local.table_to_description, replace(basename(full_path), local.TABLE_SCHEMA_SUFFIX, ""), null)
  }]

  # Merge calculated inputs with the ones we use every time.
  tables = [for table_input in local.table_inputs :
    merge(table_input, local.TABLE_CONSTANTS)
  ]

  #
  # Views
  #
  VIEW_CONSTANTS = {
    # Reporting Subsystem always uses Standard SQL Syntax
    use_legacy_sql = false,
    labels = {
      terraform_managed = "true"
    }
  }
  QUERY_TEMPLATE_SUFFIX = ".sql"
  # Local filenames for view templates. Returns something like ["latest_users.sql", "users_by_id.sql"]
  view_query_template_filenames = fileset("${path.module}/assets/views", "*.sql")
  # expanded to fully qualified path, e.g. ["/repos/workbench/terraform/modules/reporting/views/latest_users.sql", ...]
  //  view_query_template_paths = [for file_name in local.view_query_template_filenames : pathexpand("./reporting/views/${file_name}")]
  view_query_template_paths = [for file_name in local.view_query_template_filenames : pathexpand("${path.module}/assets/views/${file_name}")]

  # Create views for each .sql file in the views directory. There is no Terraform
  # dependency from the view to the table(s) it queries, and I  don't believe the SQL is even checked
  # for accuracy prior to creation on the BQ side.
  views = [for view_query_template_path in local.view_query_template_paths :
    merge({
      view_id = replace(basename(view_query_template_path), local.QUERY_TEMPLATE_SUFFIX, ""),
      query = templatefile(view_query_template_path, {
        project = var.project_id
        dataset = var.reporting_dataset_id
      })
  }, local.VIEW_CONSTANTS)]

}

# All BigQuery assets for Reporting subsystem
module "main" {
  source     = "terraform-google-modules/bigquery/google"
  version    = "~> 4.3"
  dataset_id = var.reporting_dataset_id
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
