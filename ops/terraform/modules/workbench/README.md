# Workbench Child Modules
The module directories here represent individually deployable subsystems, 
microservices, or other functional units. It's easy enough to put all buckets, say,
in a `gcs` module, but that wouldn't really let us operate on an individual components's bucket.

Following is a broad outline fo each child module. If you feel irritated that you can't see, for example,
all dashboards in one place, you can still go to the Console or use `gcloud`.

## Reporting
The state for reporting is currently the BigQuery dataset and its tables and views. In the future,
it makes sense to add j
* Reporting-specific metrics
* Notifications on the system
* Reporting-specific logs, specific logs
* Data blocks for views (maybe)

## Backend Database (future)
This resource is inherently cross-functional, so we can just put
* The application DB 
* backup settings
This will take advantage of the `google_sql_database_instance` resource.

Schema migrations work via `Ruby->Gradle->Liquibase->MySql->🚂` 
Maybe it needs a `Terraform` caboose. It looks like there's not currently a Liquibase provider.
