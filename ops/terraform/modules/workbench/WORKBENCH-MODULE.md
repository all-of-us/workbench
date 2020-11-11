
# Workbench Module
The module directories here represent individually deployable subsystems, 
microservices, or other functional units. It's easy enough to put all buckets, say,
in a `gcs` module, but that wouldn't really let us operate on an individual components's bucket.

Following is a broad outline fo each child module. If you feel irritated that you can't see, for example,
all dashboards in one place, you can still go to the Console or use `gcloud`.

A somewhat forward-looking plan for that would look like

# Workbench Module Development Plan
The Workbench is the topmost parent module in the AoU Workbench
Application configuration. It depends on several modules for individual
subsystems.

After creating a valid Terraform configuration we're  not finished,
as we need to make sure we don't step on other tools or automatioin.
For example, items that  pertain to cloud resources will need to move
out of the workbench JSON config system.

I have automation already for stackdriver setting that fetches all of theiir configurations
and plan to migrate it to Terraform.

## Reporting
The state for reporting is currently the BigQuery dataset and its tables and views.
Highlights
* Reporting-specific metrics with the `google_logging_metric` [resource](https://www.terraform.io/docs/providers/google/r/logging_metric.html)
and others
* Notifications on the system
* Reporting-specific logs, specific logs
* Data blocks for views (maybe)

## Backend Database (future)
This resource is inherently cross-functional, so we can just put
* The application DB 
* backup settings
This will take advantage of the `google_sql_database_instance` resource.

Schema migrations work via `Ruby->Gradle->Liquibase->MySql->�`  
Maybe it needs a `Terraform` caboose. It looks like there's not currently a Liquibase provider.

## Workbench to RDR Pipeline
Instantiate [google_cloud_tasks_queue](https://www.terraform.io/docs/providers/google/r/cloud_tasks_queue.html) resource
resouorces as necessary.

## API Server
* AppEngine versions, instances, logs, etc. Isn't just named
App Engine, since that's the resource that gets crated.

## Action Audit
This module maps to 
* Stackdriver logs for each environment. (It will nedd to
 move from the applicatioin JSON config likely.)

## Tiers and Egress Detection
There is a [sumo logic provider](https://www.sumologic.com/blog/terraform-provider-hosted/) for Terraform, which is very good
news. It looks really svelte.

We will also want to control the VPC flow logs,
perimeters, etc, but it  won't be  in this `workbench` module,
because Terra-not-form owns the organization and needs to do
cration manually for now.
