# Denormalized Tables

The Researcher Workbench publishes a number of subtables to the CDR for internal use around cohort search, cohort review, and the data set builder. These tables are maintained via shell scripts in `/db-cdr/generate-cdr` and stored in the synthetic and production CDR datasets in BigQuery. 

##Adding a new denormalized table:
First, make sure it needs to be a new table in the CDR. If not, probably best to store it elsewhere. If it does need to be part of the CDR/part of our published set of tables. If so, follow the following steps:
1) Write the shell script (See another shell script for an example)
2) Add the script to `make-bq-denormalized-tables.sh`
3) Add the ruby endpoint in `devstart.rb`
4) Add documentation of the shell script to the README
