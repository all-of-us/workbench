# Workbench

[![CircleCI Build Status](https://circleci.com/gh/all-of-us/workbench.svg)](https://circleci.com/gh/all-of-us/workflows/workbench)

## Getting the code

* Run `git clone https://github.com/all-of-us/workbench`

To make changes, do:

* git checkout master
* git pull
* git checkout -b USERNAME/BRANCH_NAME
* (make changes and git add / commit them)
* git push -u origin USERNAME/BRANCH_NAME

And make a pull request in your browser at
https://github.com/all-of-us/workbench based on your upload.

After responding to changes, merge in GitHub.

## Setup

Docker must be installed to build and run code (For Google workstations, see
go/installdocker.). Ruby is required to run our development scripts, which
document common operations and provide a convenient way to perform them.

Add `export UID` to your `.bashrc`. All our Docker commands run as your user,
not root. Exporting `UID` ensures one-off commands will provide `UID` (which is
a shell var not an env var by default) to our containers.

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

Other available operations may be discovered by running:
```Shell
./project.rb
```

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

To deploy your local API code to a given AppEngine project, in the api 
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
based on config_$ENV.json files (which are converted into `WorkbenchConfig`
objects) and loaded into the database by `workbench.tools.ConfigLoader`.

`CacheSpringConfiguration`, a Spring `@Configuration`, provides
the `@RequestScoped` `WorkbenchConfig`. It caches the values fetched from the
database with a 10 minute expiration.

## API Server Database Updates

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

## Docker Troubleshooting

### Permissions errors

Always `export UID` (or place that in your `.bashrc`) before running
`docker-compose` commands outside of `project.rb` (which takes care of setting
`UID`).

If you get permissions errors like
`Exception in thread "main" java.io.FileNotFoundException: /.gradle/wrapper/dists/gradle-4.0-all/ac27o8rbd0ic8ih41or9l32mv/gradle-4.0-all.zip.lck (Permission denied)` or from MySQL like
`ERROR 1130 (HY000): Host '172.18.0.3' is not allowed to connect to this MySQL server`
it may be because you ran a `docker` command as root (the default) whereas
our `project.rb` scripts run as you. To clear permissions in Docker volumes,
remove the volumes:

```
docker volume rm $(docker volume ls -q)
```

### Timeout connecting to database

```
api$ ./project.rb dev-up
...
Running database migrations...
+ docker-compose run db-migration
Starting api_db_1 ... done
Operation timed out
```

Workaround: Ready your `db` container separately before running the API.

```
$ cd workbench/api
$ docker volume rm $(docker volume ls -q)
$ export UID
$ docker-compose run -d db  # prints name of container
$ docker logs api_db_run_N --follow
# until you see "mysqld: ready for connections" then ^C
$ docker kill api_db_run_N
# Now normal statup should work.
$ ./project dev-up
```
