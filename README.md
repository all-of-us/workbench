# Workbench

[![CircleCI Build Status](https://circleci.com/gh/all-of-us/workbench.svg)](https://circleci.com/gh/all-of-us/workflows/workbench)

## Setup for Development

System requirements:

  * [Docker CE](https://www.docker.com/community-edition)
    * Docker must be installed to build and run code (For Google workstations, see http://go/installdocker).
    * __IMPORTANT__: be sure to allocate 12GB of memory (if possible) and 2GB swap to the Docker Engine. See
See https://docs.docker.com/docker-for-mac/#advanced for screenshots and instructions for Mac.
  * [Ruby](https://www.ruby-lang.org/en/downloads/)
    * Our team's dev/ops scripts are written in Ruby. Most common operations are launched via the project.rb script at the root of each sub-project.
  * [Python](https://www.python.org/downloads/) >= 2.7.9
    * Python is required by some project-specific scripts and by the Google Cloud Platform tools.
  * [gcloud](https://cloud.google.com/sdk/docs/#install_the_latest_cloud_tools_version_cloudsdk_current_version)

For local development, also install:

  * [yarn](https://yarnpkg.com/lang/en/docs/install/#mac-stable)
  * [Node.js](https://nodejs.org/en/)
  * [Java 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

After you've installed `gcloud`, login using your `pmi-ops` account:

```shell
gcloud auth login
```

To initialize the project, run the following:

```shell
git clone https://github.com/all-of-us/workbench
cd workbench
git submodule update --init --recursive
```

Then set up [git secrets](#git-secrets) and fire up the [development servers](#running-the-dev-servers). Optionally, you can [set up your Intellij](https://docs.google.com/document/d/1DtESBapEzvuti7xODTFPHorwmLM7LybF-6D5lhbIkLU/edit) for UI or API work.

Before doing any development, you must run:
```Shell
./gradlew compileGeneratedJava appengineRun
```
This will generate compiled Java and MapStruct files that are necessary for the app to compile. On Macs, this command will never complete - when it has gotten to 97% it will hang forever. It can safely be `ctrl+c`'d at that point.

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

#### API: Faster API startup for MacOS
The above steps for starting the API server can take upwards of 8-10 minutes on MacOS, most likely due to performance issues with Docker for Mac. Follow these steps to set up your developer environment to start the API server outside of docker. A full restart should take ~30 seconds with this method.

All commands should be run from `workbench/api`

##### Setup
* Install Java 8
* Add following to `~/.bash_profile`. Note:
    * Your Java8 library directory may be different.  
      YOUR_WORKBENCH_DIRECTORY_PATH is the pathname to your workbench git repo.
      ```Shell
      export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_201.jdk/Contents/Home
      export WORKBENCH_DIR=[YOUR_WORKBENCH_DIRECTORY_PATH]
      source $WORKBENCH_DIR/api/db/load_vars.sh
      ```
        * If you are using zsh, add the lines above to `~/.zshrc`.
    * Source your bash profile or open a new terminal
      ```Shell
      source `~/.bash_profile`
      ```
* Install Java App Engine components
    * ```Shell
      gcloud components install app-engine-java
      ```
* Generate Google Cloud access token
    * ```Shell
      ./project.rb get-test-service-creds
      ```

##### Usage
* If you have schema migrations or pending configuration updates, run through the [normal docker developer startup process](#api-dev-appengine-appserver) at least once successfully. This is typically only necessary when switching between branches.
* Start services required for API server
    * ```Shell
      ./project.rb start-api-reqs
      # the counterpart command is ./project.rb stop-api-reqs
      ```
* Start API server through gradle
    * ```Shell
      ./gradlew appengineRun
      ```

#### Hot Code Swapping

While the API is running locally, saving a .java file should cause a recompile and reload of that class. Status is logged to the console. Not all changes reload correctly (e.g., model classes do not appear to reload more than once).

#### Caveats

The first time a server is started after a database clear (`./project.rb docker-clean`, `./project.rb run-local-all-migrations`), it may take some time for a billing project to become available for workspace creation. Generally, this takes about 5 minutes, but has been known to take upwards of 40 in some cases. The billing project buffer size is set to 2 for local environments (`config_local.json`). A cron job runs every minute while the API server is running to check if there are empty slots.

To check whether you have available billing projects:
```Shell
./project.rb connect-to-db
select * from billing_project_buffer_entry;
```

The `status` column enum values can be found in `org.pmiops.workbench.db.model.StorageEnums`.

### UI

Before launching or testing the UI, yarn must first install the neccessary packages. From the `ui/` directory:
```Shell
yarn install
```

To launch the local UI:
```Shell
yarn dev-up
```

You can view your local UI server at http://localhost:4200/.  

By default, this connects to our test API server. Use `--configuration=$ENV` to
use an alternate `src/environments/environment.$ENV.ts` file and connect to a
different API server. To connect to your own API server running at
`localhost:8081`, pass `--configuration=local`.

To run react UI tests:
```Shell
yarn test-react
```

Other useful yarn commands:
```Shell
# To upgrade yarn packages:
yarn

# To lint the UI and automatically fix issues:
yarn lint --fix
```


#### Legacy startup
You can also run the UI through project.rb. NOTE: this is slower and not recommended.  
From the `ui/` directory,
```Shell
./project.rb dev-up
```

[legacy] UI tests in Angular can be run and viewed at http://localhost:9876/index.html.

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

## Generate cdr count databases for a CDR version

The following scripts need to be run anytime a new cdr is released or if you want all the count data for cohort builder.

Description of arguments these scripts take are as follows.
* bq-project : Project where BigQuery cdr lives. Ex: all-of-us-ehr-dev, all-of-us-workbench-test
* bq-dataset : BigQuery Dataset name of the cdr release. Ex: synthetic_cdr20180606
* workbench-project:  Project where private count dataset (cdr) is generated. This must exist.
* cdr-version: Name of the cloud cdr your creating. Ex: synth_r_2019q3_1
* bucket: A GCS Bucket where csv data dumps are of the generated data. This must exist.
* instance: Cloud Sql Instance. Ex: workbenchmaindb

### Examples below need to be run in the following order. It's also very important that the prep tables are in a viable state before starting this process(Check with CB team on this).

#### Generate all denormalized tables(search, review and data set) in the BigQuery cdr only one time when it is released or as needed
`./project.rb make-bq-denormalized-tables --bq-project all-of-us-ehr-dev --bq-dataset synthetic_cdr20180606`
##### Result is
* The BigQuery dataset has new denormalized tables(search, review and data set) for cohort builder to work.
* Each of these can be run individually if needed(sequential ordering is very important here:
* `./project.rb make-bq-denormalized-search --bq-project all-of-us-ehr-dev --bq-dataset synthetic_cdr20180606`
* `./project.rb generate-cb-criteria-tables --bq-project all-of-us-ehr-dev --bq-dataset synthetic_cdr20180606`
* `./project.rb make-bq-denormalized-review --bq-project all-of-us-ehr-dev --bq-dataset synthetic_cdr20180606`
* `./project.rb make-bq-denormalized-dataset --bq-project all-of-us-ehr-dev --bq-dataset synthetic_cdr20180606`
* `./project.rb make-bq-dataset-linking --bq-project all-of-us-ehr-dev --bq-dataset synthetic_cdr20180606`
* Info/examples for dataset script below:
* Cdr BigQuery dataset:  all-of-us-workbench-test:cdr20181107
* CSV dumps of tables in bucket all-of-us-workbench-private-cloudsql: cdr20181107/*.csv.gz
* Browse csvs in browser like here :https://console.cloud.google.com/storage/browser?project=all-of-us-workbench-test&organizationId=394551486437
* Note cdr-version can be '' to make dataset named cdr

#### The next 2 scripts are used to generate cloud cdr database instances. Generate cdr count data using deidentified cdr release.
`./project.rb generate-private-cdr-counts --bq-project all-of-us-ehr-dev --bq-dataset synthetic_cdr20180606 --workbench-project all-of-us-workbench-test --cdr-version synth_r_2019q3_1 --bucket all-of-us-workbench-private-cloudsql/synthetic_cdr20180606`
##### Result is
* Generates csv.gz files in the specified bucket. These files will be used in the next step

#### Generate cloudsql databases from a bucket using *.csv.gz files
`./project.rb generate-cloudsql-db --project all-of-us-workbench-test --instance workbenchmaindb --database synth_r_2019q3_1 --bucket all-of-us-workbench-private-cloudsql/synthetic_cdr20180606/synth_r_2019q3_1`
##### Result is
* Databases are live on cloudsql.

#### Tell workbench about your new cdr release so they can use it
1. For the environment you want, in the workbench/api/config/cdr_versions_ENV.json , add a new object to the array for your cdr. Properties are:
   * name: unique name
   * dataAccessLevel: 1 = registered, 2 = controlled
   * bigqueryProject: project the BigQuery cdr is
   * bigqueryDataset: dataset of cdr,
   * creationTime: date string in this format "2018-09-20 00:00:00Z",
   * releaseNumber: gets incremented by 1 each time an official release is made. It has the same value for a registered and controlled cdr release.
   * numParticipants: Number of participants in CDR.
   * cdrDbName: name of the the cloudsql count database used by workbench "synth_r_2019q3_1". CDR versioning doc: https://docs.google.com/document/d/1W8DnEN7FnnPgGW6yrvGsdzLZhQrdOtTjvgdFUL6e4oc/edit
2. Set the default cdr version for the environment in config_ENV.json.
   * You probably don’t want to set your new cdr to the default before testing it.
   * NOTE The cloudsql instance is set in code for each environment in /api/libproject/devstart.rb  
3. Make your config changes take effect:
   * For non local environments:
     * commit and merge your config files with master and the changes will take effect on the next build.
     * OR run `./project.rb update-cloud-config --project <project>` where project is the project for your environment. You can find this project in config_<ENV>.json server.projectId
   * For local , run dev-up to build your api

#### Generate full local mysql test databases -- cdr for data generated above if you need to develop with a full test database
* DO NOT do this with production data. It is not allowed.
* Make a sql dump from cloud console of the database you want.
* Run `./project.rb local-mysql-import --sql-dump-file <FILE.sql> --bucket <BUCKET>`
* Update your local environment per above.

Alternatively if you want to make a local database from csvs in gcs  
 * Run `./project.rb generate-local-count-dbs --cdr-version synth_r_2019q3_1 --bucket all-of-us-workbench-private-cloudsql`
 * You may want to do this if generate-cloudsql-db fails because of limited gcloud sql import csv functionality
 * Or you have some local schema changes you need and just need csv data
##### Result is
* Local mysql database or databases.
* cdr-version in the alternative method can be an empty string, '',  to make databases named 'cdr'

#### Put mysqldump of local mysql database in bucket for importing into cloudsql. Call once for each db you want to dump
`./project.rb mysqldump-local-db --db-name synth_r_2019q3_1 --bucket all-of-us-workbench-private-cloudsql`
##### Result is
* synth_r_2019q3_1.sql uploaded to all-of-us-workbench-private-cloudsql

#### Import a dump to cloudsql instance by specifying dump file in the --file option.
`./project.rb cloudsql-import --project all-of-us-workbench-test --instance workbenchmaindb --bucket all-of-us-workbench-private-cloudsql --database synth_r_2019q3_1 --file synth_r_2019q3_1.sql `
##### Note a 3GB dump like cdr and public can take an hour or so to finish. You must wait before running another import on same instance (Cloudsql limitation) You can check status of import at the website: https://console.cloud.google.com/sql/instances/workbenchmaindb/operations?project=all-of-us-workbench-test
##### Or with this command:
`gcloud sql operations list --instance [INSTANCE_NAME] --limit 10`
##### Result
* databases are in cloudsql

#### Import a dump to local mysql db.
`./project.rb local-mysql-import --sql-dump-file synth_r_2019q3_1.sql --bucket all-of-us-workbench-private-cloudsql`

##### Result
* mysql db is in your local mysql for development. You need to alter your env per above to use it.


## Elasticsearch

Elasticsearch is being integrated as an auxilliary backed on top of the BigQuery
CDR for cohort building. Currently it can only be run via docker-compose on a
local instance. See the full design:
https://docs.google.com/document/d/1N_TDTOi-moTH6wrXn1Ix4dwUlw4j8GT9OsL9yXYXYmY/edit

### Indexing

```
./project.rb load-es-index
```

### Development

As of 3/4/19, you'll need to enable Elasticsearch locally to utilize it in the
Cohort Builder.

```
sed -i 's/\("enableElasticsearchBackend": \)false/\1true/' config/config_local.json
```

#### Example criteria

Currently the default setting for the indexer is to only index ~1000
to keep the local data size small. Some example criteria that will match this
default dataset:

- Conditions ICD9: Group 250 Diabetes mellitus
- Drugs: Acetaminophen
- PPI: Anything (support for individual answers coming soon)
- Procedures CPT: 99213

#### Elasticsearch Direct Queries

Requires that Elastic is running (via run-api or dev-up).

Show the top 5 standard condition concept IDs:

```
curl -H "Content-Type: application/json" "localhost:9200/cdr_person/_doc/_search?pretty" -d '{"size": 0, "aggs": {"aggs": {"terms": {"field": "condition_concept_ids", "size": 5 }}}}'
```

The above IDs can be cross-referenced against the Criteria table in SQL or
BigQuery to determine cohort builder search targets.

Dump all participants matching a condition source concept ID (disclaimer: large):

```
curl -H "Content-Type: application/json" "localhost:9200/cdr_person/_doc/_search?pretty" -d '{"query": {"term": {"condition_source_concept_ids": "44833466"}}}' > dump.json
```

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

To run both api and common api unit tests, in the api dir run:

```
./project.rb test
```

To run just api unit tests, run:

```
./project.rb test-api
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
scopes of `email`, `profile`, and `cloud-billing`.

```Shell
# From the "api" directory, use `oauth2l` to retrieve an authorization header: 
`~/go/bin/oauth2l header --json ./sa-key.json email profile cloud-billing`
```

Now we'll demonstrate calling Firecloud's [profile/billing API](https://api.firecloud.org/#!/Profile/billing)
with the service account credentials.
```Shell
# call Firecloud Billing API, format the JSON output and view in less 

curl -X GET -H "`~/go/bin/oauth2l header --json ./sa-key.json email profile cloud-billing`" \
    -H "Content-Type: application/json" \
    "https://firecloud-orchestration.dsde-dev.broadinstitute.org/api/profile/billing" \
    | jq | less

# If you get 401 errors, you may need to clear your token cache.
oauth2l reset
```

### Action Audit System
We have a framework for user action auditing with its own [query system](doc/action-audit-queries.md).
Read more about its implementation [here](doc/action-audit-implementation.md).
