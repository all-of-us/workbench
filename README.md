# Workbench
CircleCI Test Status [![CircleCI Build Status](https://circleci.com/gh/all-of-us/workbench.svg)](https://circleci.com/gh/all-of-us/workflows/workbench)

## Introduction
The [All of Us Research Program](https://allofus.nih.gov/) Researcher Workbench is an application that runs on the [Terra platform](https://terra.bio) for researchers to work with *All of Us* program data in a secure and convenient way. It provides users with an environment for running Jupyter notebooks
in R or Python that can access data in the program's Curated Data Repository. For information on research
opportunities see the [official website](https://allofus.nih.gov/get-involved/opportunities-researchers).

While the code in this repository is available according to the terms of our [license](LICENSE.txt), not all features
are available for external use. In particular, it's not currently possible for third parties to build and
deploy their own instance of the Researcher Workbench solely from the code here.

## Development Guides
[Documentation on API Structure](api/docs/code-structure.md)

[UI Style Guide](ui/docs/style-guide.md)

### Best Practices Guides
MapStruct [Best Practices](api/docs/mapstruct.md) and [Tutorial](tutorials/mapstruct-tutorial.md)

## Setup for Development
* [Software Requirements](api/docs/developer-system-requirements.md)
* [System Initialization](api/docs/developer-system-initialization.md)
* [Intellij Setup](https://docs.google.com/document/d/1DtESBapEzvuti7xODTFPHorwmLM7LybF-6D5lhbIkLU/edit?usp=sharing)
 for UI, API, and tooling work.

## Development Process
### Using Git
To make changes, do:

```shell
git checkout main
git pull
git checkout -b <USERNAME>/<BRANCH_NAME>
# (make changes and git add / commit them)
git push -u origin <USERNAME>/<BRANCH_NAME>
```

And make a pull request in your browser at
https://github.com/all-of-us/workbench based on your upload.

After responding to changes, merge in GitHub.

### API

* Autoformat Java code via google-java-format: `./gradlew spotlessApply` (git pre-push / Circle will complain if you forget)

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

When the console displays "Listening for transport dt_socket at address: 8001", your
local API server endpoints are available under http://localhost:8081/.  You can test this by
navigating to the [status endpoint](http://localhost:8081/v1/status) in your browser or
executing `curl http://localhost:8081/v1/status`

**Note:** If you haven't loaded any data locally for the app, please run the goal below. Also, this will not run while dev-up is running, so please kill dev-up first.
```Shell
./project.rb run-local-data-migrations
```

Or you can run all migrations with:
```Shell
./project.rb run-local-all-migrations
```
You can run the server (skipping config and db setup) by running:

```Shell
./project.rb run-api
```

Other available operations may be discovered by running:
```Shell
./project.rb
```

#### Caveats

The first time a server is started after a database clear (`./project.rb docker-clean`), it may take some time for a billing project to become available for workspace creation. Generally, this takes about 5 minutes, but has been known to take upwards of 40 in some cases. The billing project buffer size is set to 2 for local environments (`config_local.json`). A cron job runs every minute while the API server is running to check if there are empty slots.

To check whether you have available billing projects:
```Shell
./project.rb connect-to-db
select * from billing_project_buffer_entry where status=2;
```

The `status` column enum values can be found in `org.pmiops.workbench.db.model.StorageEnums`.

### UI

See [ui/README.md]().

## Deploying

To deploy your local workbench API code to a given AppEngine project, in the api
directory run:

```
./project.rb deploy --project PROJECT --version VERSION --[no-]promote
```

This also migrates the SQL databases, so avoid using this when you have local
SQL schema changes.

Example:

```
./project.rb deploy --project all-of-us-workbench-test --version dantest --no-promote
```

When the api is deployed, you'll be able to access it at https://VERSION-dot-api-dot-PROJECT.appspot.com. If you specify --promote, it will be the main API code
served out of https://api-dot-PROJECT.appspot.com.
Aside from releases, this command can be used to test a topic branch in the
shared test project before submitting. If possible, push to a version with your
own username and --no-promote.


To deploy your local UI code to a given AppEngine project, in the ui
directory run:

```
./project.rb deploy-ui --project PROJECT --version VERSION --[no-]promote
```

Example:

```
./project.rb deploy-ui --project all-of-us-workbench-test --version dantest --no-promote
```

When the UI is deployed, you'll be able to access it at https://VERSION-dot-PROJECT.appspot.com. If you specify --promote, you can access it at https://PROJECT.appspot.com. Note that either way, it will be pointing at the live test API
service (https://api-dot-PROJECT.appspot.com). (This can be overridden locally
in the Chrome console).

__NOTE:__ In order to test out a custom UI version in the browser, you must whitelist the base URL with the OAuth 2.0 client ID for the test Workbech environment. A common pattern is to use your GitHub username as the App Engine version name, so this setup only needs to be done once. See [this doc](https://docs.google.com/document/d/15-ktzL3Hdt5rpdFAS3z-v5vSNfk_LtjtPHCvbshjeZo/edit) for instructions.


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

```
./project.rb run-local-all-migrations
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

## Tanagra Integration

There are two commands for deploying the Tanagra app locally along with the Workbench

To deploy the Tanagra api locally (NOTE: the Tanagra api is dependent on the workbench api being deployed in a different terminal window):
```
./project.rb dev-up-tanagra
```
Optional arguments (command only allows for optional arg --version or --branch but not both):
```
--disable-auth - Disable authZ
--version      - Deploy a tag (Ex: 0.0.252). Default is :tanagra_tag in environments.rb
--branch       - Deploy a branch from the tanagra repo.
--drop-db      - Drop the Tanagra database and recreate
```

To deploy the Tanagra UI and the Workbench UI together locally:
```
yarn dev-up-tanagra-local
```
Optional arguments (command only allows for optional arg -v or -b but not both):
```
-v  - Deploy a tag (Ex: 0.0.252). Default is :tanagra_tag in environments.rb
-b  - Deploy a branch from the tanagra repo
```

### Tanagra Deploy to test

To deploy Tanagra (api and ui) to test (run from `workbench` directory). 
Test deployments will only use tag :tanagra_tag in the envirionments.rb:
```
deploy/project.rb deploy-tanagra --project all-of-us-workbench-test --promote
```

## Building a new CDR Database
[Documented here](https://docs.google.com/document/d/1St6pG_EUFB9oRQUQaOSO7a9UPxPkQ5n4qAVyKF9j9tk/edit#)

## Special case cloud sql builds
#### Import sql dump locally
* DO NOT do this with production data. It is not allowed.
* Make a sql dump from GCP and put in a specified bucket
* Run `./project.rb local-mysql-import --sql-dump-file <FILE.sql> --bucket <BUCKET>`

#### Alternatively if you want to make a local database from csvs in gcs  
 * Run `./project.rb generate-local-count-dbs --cdr-version synth_r_2019q3_1 --bucket all-of-us-workbench-private-cloudsql/synth_r_2019q3_1/imported_to_cloudsql`

#### Put mysqldump of local mysql database in bucket for importing into cloudsql.
`./project.rb mysqldump-local-db --db-name synth_r_2019q3_1 --bucket all-of-us-workbench-private-cloudsql`

Then import a dump to cloudsql instance by specifying dump file in the --file option.

`./project.rb cloudsql-import --project all-of-us-workbench-test --instance workbenchmaindb --bucket all-of-us-workbench-private-cloudsql --database synth_r_2019q3_1 --file synth_r_2019q3_1.sql `

#### Import a dump to local mysql db.
`./project.rb local-mysql-import --sql-dump-file synth_r_2019q3_1.sql --bucket all-of-us-workbench-private-cloudsql`

##### Result
* mysql db is in your local mysql for development. You need to alter your env per above to use it.

###
## Cohort Builder

#### Cohort Definition Schema
[Documented here](api/reporting/docs/cohort_definition_schema.json)

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

To run unit tests, in the api dir run:

```
./project.rb test
```

To run bigquery tests (which run slowly and actually
create and delete BigQuery datasets), run:

```
./project.rb bigquerytest
```

By default, all tests will return just test pass / fail output and stack traces for exceptions. To get full logging, pass on the command line --project-prop verboseTestLogging=yes when running tests.

To filter tests, use the [--tests flag](https://docs.gradle.org/current/userguide/java_testing.html#simple_name_pattern) on any test command:

```
./project.rb bigquerytest --tests "org.pmiops.workbench.api.CohortBuilderControllerBQTest.countSubjectsNotValidMessageException"
```
### Running tests in IntelliJ

To run tests in IntelliJ, go to your preferences, click plugins, and make sure you have the `JUnit` plugin installed.

Once you have JUnit installed, go to the test file you want to run, right click on the test file, and select `Run` or `Debug` to run or debug the tests. To run or debug on a specific test class or method, open the file, and test running options should appear as green triangles pointing right on the side bar. Clicking that will open a dialog allowing you to run that specific class or method.

### Debugging a Running API in IntelliJ

Choose `Edit Configurations...` from the dropdown at the left side of the top the menu bar
and choose `Templates` -> `Remote`.  Click `Create configuration` in the top right.  This
will create a Remote Configuration which you can name how you like.  Ensure that this
configuration uses the port the API is listening on, *8001*.

When this configuration is selected, clicking the debug icon (looks like a green bug) will
cause the running API process to stop at breakpoints you have set for inspection.

## Manual Testing

### Backend Swagger Portals

These are easiest if you need to authenticate as one of your researcher accounts.

- Firecloud
  - dev: https://firecloud-orchestration.dsde-dev.broadinstitute.org/
  - prod: https://api.firecloud.org
- Leo (notebook clusters)
  - dev: https://leonardo.dsde-dev.broadinstitute.org
  - prod: https://notebooks.firecloud.org

### Authenticated Backend Requests (CLI)

This approach is required if you want to issue a request to a backend as a service account. This may be necessary in some cases as the Workbench service is an owner on all AoU billing projects.

This approach requires [oauth2l](https://github.com/google/oauth2l) to be installed (and `brew install go` on MacOS):

```Shell
go get github.com/google/oauth2l
go install github.com/google/oauth2l
```

To obtain the service account credentials, run
```Shell
cd api
gcloud auth login <user>@pmi-ops.org # for test environment
gcloud config set account <user>@pmi-ops.org # also works if logged in.
gcloud auth list # confirm it's what you expect
./project.rb get-test-service-creds # the dev-up command should also include this step.
```
You should see a file `sa-key.json` in the current directory
The following shows how to make an authenticated backend request as the shared
workbench test service account against Firecloud dev. It retrieves required authorizaiton
scopes of `userinfo.email`, `userinfo.profile`, and `cloud-billing`.

```Shell
# From the "api" directory, use `oauth2l` to retrieve an authorization header:
`~/go/bin/oauth2l header --json ./sa-key.json userinfo.email userinfo.profile cloud-billing`
```

Now we'll demonstrate calling Firecloud's [profile/billing API](https://api.firecloud.org/#!/Profile/billing)
with the service account credentials.
```Shell
# call Firecloud Billing API, format the JSON output and view in less

curl -X GET -H "`~/go/bin/oauth2l header --json ./sa-key.json userinfo.email userinfo.profile cloud-billing`" \
    -H "Content-Type: application/json" \
    "https://firecloud-orchestration.dsde-dev.broadinstitute.org/api/profile/billing" \
    | jq | less

# If you get 401 errors, you may need to clear your token cache.
oauth2l reset
```

### Action Audit System
We have a Stackdriver- and BigQuery-powered framework for user action auditing that supports
flexible [queries](api/docs/action-audit-queries.md). Read more about its design, structure, and
implementation in this [document](api/docs/action-audit.md).

### Workbench Reporting Dataset
To support analytics, we have a reporting pipeline that exports data to a BigQuery dataset. See the [wiki](https://github.com/all-of-us/workbench/wiki/Workbench-Reporting-Dataset-(WRD)) for details.

### Monitoring, Alerts, and Dashboards
The API server periodically records various metrics, powering Stackdriver dashboards and alerts.  See the [wiki](https://github.com/all-of-us/workbench/wiki/Monitoring,-Alerts,-and-Dashboards) for details.
