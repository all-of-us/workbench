
resource "google_sql_database_instance" "main" {
  name             = "dummydb0"
  database_version = "MYSQL_5_7"
  region           = var.region
  project          = var.project_id

  settings {
    tier = "db-f1-micro"
  }
}
