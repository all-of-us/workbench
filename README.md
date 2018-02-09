# Workbench

[![CircleCI Build Status](https://circleci.com/gh/all-of-us/workbench.svg)](https://circleci.com/gh/all-of-us/workflows/workbench)

## Setup for Development

System requirements:

  * [Docker CE](https://www.docker.com/community-edition)
  * [Ruby](https://www.ruby-lang.org/en/downloads/)
  * [Python](https://www.python.org/downloads/) >= 2.7.9
  * [gcloud](https://cloud.google.com/sdk/docs/#install_the_latest_cloud_tools_version_cloudsdk_current_version)

Docker must be installed to build and run code (For Google workstations, see
go/installdocker.). Ruby is required to run our development scripts, which
document common operations and provide a convenient way to perform them.
Python is required by some scripts and the Google Cloud Tools.

After you've installed `gcloud`, login using your `pmi-ops` account:

```shell
gcloud auth login
```

To initialize the project, run the following:

```shell
git clone https://github.com/all-of-us/workbench
cd workbench
git submodule init
git submodule update --recursive
```

Then set up [git secrets](#git-secrets) and fire up the [development servers](#running-the-dev-servers).

## Development Process

To make changes, do:

```shell
git checkout master
git pull
git checkout -b <USERNAME>/<BRANCH_NAME>
# (make changes and git add / commit them)
git push -u origin <USERNAME>/<BRANCH_NAME>
```

And make a pull request in your browser at
https://github.com/all-of-us/workbench based on your upload.

After responding to changes, merge in GitHub.

### UI

* Direct your editor to write swap files outside the source tree, so Webpack
does not reload when they're updated.
[Example for vim](https://github.com/angular/angular-cli/issues/4593).

## Running the Dev Servers

### API: dev AppEngine appserver

From the `api/` directory:
```Shell
./project.rb dev-up
```

When the console displays "Dev App Server is now running", you can hit your
local API server under http://localhost:8081/api/. 

**Note:** If you haven't loaded any data locally for the app, please run the goal below. Also, this will not run while dev-up is running, so please kill dev-up first.
```Shell
./project.rb run-local-data-migrations
```

**Big data such as concept and concept_relationship required for some functionality can be populated by  
```Shell
./project.rb run-local-bigdata-migrations
```

You can run the server (skipping config and db setup) by running:

```Shell
./project.rb run-api
```


Other available operations may be discovered by running:
```Shell
./project.rb
```

### Public API: dev AppEngine appserver

After running dev-up, run-local-data-migrations, and
run-local-bigdata-migrations, run in the api dir:

```Shell
./project.rb run-public-api
```

This will start up the public API on http://localhost:8083/.

### UI

From the `ui/` directory:
```Shell
./project.rb dev-up
```

After webpack finishes the build, you can view your local UI server at
http://localhost:4200/. You can view the tests at http://localhost:9876/debug.html.

By default, this connects to our test API server. Use `--environment=$ENV` to
use an alternate `src/environments/environment.$ENV.ts` file and connect to a
different API server. To connect to your own API server running at
`localhost:8081`, pass `--environment=local`.

Other available operations may be discovered by running:
```Shell
./project.rb
```

#### You can regenerate classes from swagger with

```Shell
./project.rb swagger-regen
```

## Deploying

To deploy your local workbench API code to a given AppEngine project, in the api 
directory run:

```
./project.rb deploy-api --project PROJECT --account ACCOUNT@pmi-ops.org --version VERSION [--promote]
```

Example:

```
./project.rb deploy-api --project all-of-us-workbench-test --account dan.rodney@pmi-ops.org --version dantest
```

When the api is deployed, you'll be able to access it at https://VERSION-dot-api-dot-PROJECT.appspot.com. If you specify --promote, it will be the main API code
served out of https://api-dot-PROJECT.appspot.com.

To deploy your local public API code, in the api directory run:

```
./project.rb deploy-public-api --project PROJECT --account ACCOUNT@pmi-ops.org --version VERSION [--promote]

```

To deploy your local UI code to a given AppEngine project, in the ui 
directory run:

```
./project.rb deploy-ui --project PROJECT --account ACCOUNT@pmi-ops.org --version VERSION [--promote]
```

Example:

```
./project.rb deploy-ui --project all-of-us-workbench-test --account dan.rodney@pmi-ops.org --version dantest
```

When the UI is deployed, you'll be able to access it at https://VERSION-dot-PROJECT.appspot.com. If you specify --promote, you can access it at https://PROJECT.appspot.com. Note that either way, it will be pointing at the live test API 
service (https://api-dot-PROJECT.appspot.com). (If there's a need, we may
want to make this configurable in future.)

## git-secrets

### Setup

Download the git-secrets tool.
If you are on a mac, run:
```Shell
  brew install git-secrets
```
If you are on Linux, run:
```Shell
rm -rf git-secrets
git clone https://github.com/awslabs/git-secrets.git
cd git-secrets
sudo make install && sudo chmod o+rx /usr/local/bin/git-secrets
cd ..
rm -rf git-secrets
```
### Running

git-secrets by default runs every time you make a commit. But if you
want to manually scan:
#### The Repository
```Shell
git secrets --scan
```
#### A File(s)
```Shell
git secrets --scan /path/to/file (/other/path/to/file *)
```
#### A Directory (recursively)
```Shell
git secrets --scan -r /path/to/directory
```

## API Server Configuration

Spring application configs, in `application.properties` files, specify behavior
like logging. They are static files bundled with the built Java binary.

Database connection information is read from `application-web.xml`. These
secrets vary per-environment; Ruby setup scripts pull the values from Google
Cloud Storage and generate the XML, which is then deployed with the Java binary.

Server behavior configuration is stored in the database. It may be changed
without restarting the server, but is usually set only at deployment time. It's
based on config\_$ENV.json files (which are converted into `WorkbenchConfig`
objects) and loaded into the database by `workbench.tools.ConfigLoader`.

`CacheSpringConfiguration`, a Spring `@Configuration`, provides
the `@RequestScoped` `WorkbenchConfig`. It caches the values fetched from the
database with a 10 minute expiration.

## API Server Database Updates

Loading of local tables/data for both schemas (workbench/cdr) happens in a manual goal(creates tables in both schemas and insert any app data needed for local development):

```./project.rb run-local-all-migrations
```

Local tables loaded with data are:
  * **workbench** - cdr_version
  * **cdr** - criteria, achilles_analysis, concept, concept_relationship, vocabulary, domain, achilles_results, achilles_results_concept and db_domain

When editing database models, you must write a new changelog XML file. See
[Liquibase change docs](http://www.liquibase.org/documentation/changes/index.html),
such as [createTable](http://www.liquibase.org/documentation/changes/create_table.html).

You can get Hibernate to update the schema for inspection (and then backport
that to liquibase's XML files) by editing `api/db/vars.env` to make Hibernate
run as the liquibase user and adding `spring.jpa.hibernate.ddl-auto=update`
to `api/src/main/resources/application.properties`.

Then use `api/project.rb connect-to-db` and `SHOW CREATE TABLE my_new_table`.
Revert your changes or drop the db when you're done to verify the changelog
works.

Finally, write a new changelog file in `api/db/changelog/` and include it in
`db.changelog-master.xml`.

`liquibase` does not roll back partially failed changes.

Workbench schema lives in `api/db` --> all workbench related activities access/persist data here

CDR schema lives in `api/db-cdr` --> all cdr/cohort builder related activities access/persist data here

## Generate cdr and public count databases for a CDR version

This happens anytime a new cdr is released or if you want all the count data for databrowser and cohort builder generated locally. 

Description of arguments for these scripts are as follows. See examples below.
* bq-project : Project where BigQuery cdr lives that you want to generate data from. This must exist
* bq-dataset : BigQuery Dataset for the cdr release that you want to generate data from. This must exist
* workbench-project:  Project where private count dataset (cdr) is generated. This must exist.
* public-project: Project where public count dataset (public) is generated. This must exist.
* cdr-version: Version of form YYYYMMDD or empty string '' . It is used to name resulting datasets, csv folders, and databases. 
* bucket: A GCS Bucket where csv data dumps are of the generated data. This must exist.
* db-name: Name of local mysql database

###Examples:
#### Generate count data in BigQuery from a cdr release 
`./project.rb generate-cdr-counts --bq-project all-of-us-ehr-dev --bq-dataset test_merge_dec26 --workbench-project all-of-us-workbench-test --public-project all-of-us-workbench-test 
  --cdr-version 20180206 --bucket all-of-us-workbench-private-cloudsql`
##### Result is 
1. BigQuery datasets:  all-of-us-workbench-test:cdr20180206 and all-of-us-workbench-test:public20180206
2. CSV dumps of tables in bucket all-of-us-workbench-private-cloudsql: cdr20180206/*.csv and public20180206/*.csv  
3. Note cdr-version can be ''  to make datasets named cdr public
#### Generate local mysql databases -- cdr and public for data generated above
`./project.rb generate-local-count-dbs --cdr-version 20180206 --bucket all-of-us-workbench-private-cloudsql`
##### Result is 
1. Local mysql database cdr20180206 fully populated with count data from cdr version 20180206
2. Local mysql database public20180206 fully populated with count data from cdr version 20180206
3. Note cdr-version can be '' to make databases named cdr public

#### Put mysqldump of local mysql database in bucket for importing into cloudsql. Call once for each db you want to dump
`./project.rb mysqldump-db --db-name cdr20180206 --bucket all-of-us-workbench-private-cloudsql`
`./project.rb mysqldump-db --db-name public20180206 --bucket all-of-us-workbench-public-cloudsql`
##### Result is 
1. cdr20180206.sql uploaded to all-of-us-workbench-private-cloudsql
1. public20180206.sql uploaded to all-of-us-workbench-public-cloudsql

#### Import a dump to cloudsql instance.
`./project.rb cloudsql-import  --account all-of-us-workbench-test@appspot.gserviceaccount.com --project all-of-us-workbench-test --instance workbenchtest --sql-dump-file cdr20180206.sql --bucket all-of-us-workbench-private-cloudsql`
##### Note a 3GB dump like cdr and public can take an hour or so to finish. You must wait before running another import on same instance (Cloudsql limitation) You can check status of import at the website: https://console.cloud.google.com/sql/instances/workbenchmaindb/operations?project=all-of-us-workbench-test 
##### Or with this command: 
`gcloud sql operations list --instance [INSTANCE_NAME] --limit 10`

##### Run again for the public db
`./project.rb cloudsql-import  --account all-of-us-workbench-test@appspot.gserviceaccount.com --project all-of-us-workbench-test --instance workbenchtest --sql-dump-file public20180206.sql --bucket all-of-us-workbench-public-cloudsql`
 
##### Result
1) databases are live in cloudsql 

###
## Cohort Builder

During ```./project dev-up``` the schema activity is the only activity run, which only creates tables for the cdr schema. 

Loading of cloud data for the criteria trees and cdr version happens in a manual goal(deletes and inserts tree data into the criteria table):

```./project.rb run-cloud-data-migrations```

CDR Schema - We now have 2 activities in `api/db-cdr/build.gradle` file:
```
liquibase {
    activities {
        schema {
            changeLogFile "changelog/db.changelog-master.xml"
            url "jdbc:mysql://${db_host}:${db_port}/cdr"
            username "liquibase"
            password "${liquibase_password}"
        }
        data {
            changeLogFile "changelog-local/db.changelog-master.xml"
            url "jdbc:mysql://${db_host}:${db_port}/cdr"
            username "liquibase"
            password "${liquibase_password}"
        }
        runList = project.ext.runList
    }
}
```

CDR Schema - In the `api/db-cdr/run-migrations.sh` for **local deployments** we call the liquibase update task with the specific activity name like so:
```
echo "Upgrading database..."
../gradlew update -PrunList=schema
```

CDR Schema - In the `api/libproject/devstart.rb` for **test deployment** we call the liquibase update task with the specific activity name like so:
```
ctx.common.run_inline("#{ctx.gradlew_path} --info update -PrunList=schema")
```

## Running test cases

To run both api and public api unit tests, in the api dir run:

```
./project.rb test
```

To run just api unit tests, run:

```
./project.rb test-api
```

To run just public api unit tests run:
```
./project.rb test-public-api
```

To run bigquery tests (which run slowly and actually
create and delete BigQuery datasets), run:

```
./project.rb bigquerytest
```
