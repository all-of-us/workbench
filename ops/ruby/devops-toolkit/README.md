# DevOps Framework

## Purpose
This is a small but flexible framework for automating DevOps actions and ensuring they are
taken in a traceable, repeatable, and consistent way. It operates by  communicating to Google Cloud Platform
directly, independently of the existing build, operations, and deployment pipeline(s).

This is an experiment in an approach to a devops framework. The rationale for it coexisting with the
other All of Us Workbench devops tooling is (1) that the current framework has many issues, and it would
be a large undertaking to refactor the entirety of the existing system in one go, and (2) that we do not
want new code to depend on or interfere  with existing devlopment tooling or the local dev environment except
where  necessary (and explicit). At a later point we 
will evaluate the best ways to configure, distribute, and provide an interface to these capabilities. In
particular, the command-line options parser used is likely not going to be the long-term model.

## Usage
The entry point to all functionality is `devops.rb`, which provides command line and environment
loading and dispatches to Task classes (not scripts) that do the real work.

```
chmod +x devops.rb # allows running without ruby command
./devops.rb  -t <task_name> -e <env_file> --task-option-1 value1
```

There is also a dynamically-generated help file from the descriptions given in the options.

```
$ ./devops.rb  --help
Usage: devops [options]
    -t, --task [TASK]                Task to be run in in each provided environment
    -e, --envs-file [ENVS_FILE]      Path to environments JSON file.
```

The environments JSON file lists target environments for each command.
Some tasks may only need to use a subset of the environments, and can identify which 
one(s) via an additional parameter. (More to come here). The format for this file is 
a JSON object that has a single member named `environments` which is an
array of objects with four keys each, e.g.:
```json
{
  "environments": [
    {
      "short_name": "qa",
      "project_id": "my-qa-gcp-project",
      "project_number": 11111111,
      "service_account": "devops-service-account@my-qa-gcp-project.iam.gserviceaccount.com"
    },
    {
      "short_name": "scratch",
      "project_id": "my-scratch-gcp-project",
      "project_number": 22222222,
      "service_account": "devops-service-account@my-scratch-gcp-project.iam.gserviceaccount.com"
    }
  ]
}

```

##  Installation
We need a newish version of Ruby. I'm running 2.6.5 as of this writing, but haven't gone
back and tested with older versions. We also currently need the `gcloud` tools and an account with
sufficient privilege account to create service account tokens.

A `Gemfile` specifies the dependencies needed (currently just two google monitoring gems).
To use it, do
```
bundle install
```

## Tasks
A variety of tasks (sub-commands) are available via the required `-t` option. Here  are their
brief descriptions:
- `delete-all-service-account-keys`: Delete all the keys for the servie accounts and environments
supplied. This action will interfere with any other users of this service account, so use with care.
Main use case is cleaning up after debugging sessions where the process with a breakpoint is killed
instead of allowed to continue and remove the key(s) that it created.
    - usage: `./devops.rb --task delete-all-service-account-keys
                          --envs-file ~/inputs/target_envs.json`
- `list-all-service-account-keys`: Lists all the known service account keys for the supplied
service accounts & environments.
    - usage: `./devops.rb --task list-all-service-account-keys
                          --envs-file monitoring_env_targets.json`
- `list-dashboards`: Visit all the Stackdriver Dashboards in all the supplied environments and log
summary information.
    - usage: `./devops.rb -t list-dashboards
              -e monitoring_env_targets.json`
- `list-dev-tools`: Gather information about installed tools, including version and installation
location. Additionally retrieve system, user, and OS information. Use case for this is getting info
on the systems of a group of developers collaborating on a project to make sure none of them are too
far ahead or behind.
    - usage: `./devops.rb -t list-dev-tools
                          -i ./tasks/input/aou-workbench-dev-tools.yaml
                          -o ~/aou-dev-tools.yaml`
-  `inventory`: List information about a variety of monitoring assets, including dashboards, metrics,
logs-based, metrics, and alerting policies. This option may be broken into individual list options in
the future.
    - usage: `./devops.rb -tinventory
              -e monitoring_env_targets.json`
- `replicate-dashboard`: Given a dashboard identifier and containing project, this tool creates new
dashboards in all passed-in target environments. A few fields are updated for each copy including the
`namespace` for the metric filters and an environment short name suffix in the title, such as `[test]`.
    - usage: `./devops.rb --task replicate-dashboard
              --envs-file monitoring_env_targets.json
              --source-uri=100000000000000
              --source-env=qa`
## Design Decisions
I should justify some of the  directions I've taken here, since they're departures to varying
degrees from how we've traditionally done things.

### A New Ruby Application
I wrestled a bit with this actually. I haven't seen anything that Ruby can do that Python can't,
for example, and in general the latter language has much more documentation, a more active
community, and many more programmers. Ultimately, it would've  been a big reset to change
code bases and languages all at once, so I'll continue to write the first  version in Ruby until
I have a reason to port to Python or some  other language. One such requirement could come in the form
of other teams we depend  on and wish to integrate tightly with using some other language.

### Task Classes Instead of Scripts
I  decided to make the tasks full  Ruby classes instead of stand-alone scripts for a couple  of reasons.
First, it ensures that we can minimize usage of global state (which can get away from you easily
in Ruby). Second, we can now configure things like log levels and common options in one place.

### Independence of All Things All of Us
There are no dependencies on any AoU application or build/release code, or anything in
the utils repository. This allows us to avoid refactoring the legacy Ruby stack, and ensures
that this framework can be reused externally. Some notably absent things:
* `project.rb` System: This system depends on and is depended on by our  build & release framework, 
which means we could theoretically interfere  with local development or deploy processes. In any case,
such a dependency would render this new functionality very difficult to share with other projects.
* `ServiceAccountContext`: This system was a  major source of inspiration  for the new
 `ServiceAccountManager`, but had some customizations and legacy assumptions that were not compatible with the goals
 of this project.
* `Common.rb`: There are a number of useful snippets and  functions in this class, and I decided to steal
selectively and adapt rather than try to reuse it. It isn't a standard package, isn't actively maintained,
and it's not laid out in such a way that I can only bring in the  pieces I want. Logging  uses the
standard Ruby logger, and I'm re-evaluating how we do subprocesses.

### Use of Google Client Libraries Instead of `gcloud` Tool
`gcloud` is certainly a handy and indespensible tool for daily use for one-off tasks,
and has the advantage of working in the Cloud Shell. I don't propose to replace it for the
semi-manual tasks to which it is well suited.

However, I do want to avoid making  system calls just to use `gcloud` when there is an equivalent
library method already provided. This will make the framework feel more like an application
(which most experienced programmers have an idea how to scale) than a collection of scripts.

Ideally, we would not depend on `gcloud` being installed locally at all for someone to use
these tools.

### Use of a Limited Service Account
The framework supports a bring-you-own-service-account model. I.e. any evaluation of permissions
is left up to GCP. I requested new  monitoring-speific service accounts for this framework so
that no matter how bad an error we have in our Ruby code, we can't break any services AoU actively
depends on. It also helps ensure we're not sloppy with assumptions that happen to be true
only in special cases.
