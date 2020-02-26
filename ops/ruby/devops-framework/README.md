# DevOps Framework

## Purpose
This is a small but flexible framework for automating DevOps actions and ensuring they are
taken in a traceable, repeatable, and consistent way. It operates by  communicating to Google Cloud Platform
directly, independently of the existing build, operations, and deployment pipeline(s).

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

Additionally, you'll need to install a gem or two. Right now, it's

`gem install google-cloud-monitoring` for the monitoring library

## Design Decisions
I should justify some of the  directions I've taken here, since they're departures to varying
degrees from how we've traditionally done things.

### A New Ruby Application
I wrestled a bit with this actually. I haven't seen anything that Ruby can do that Python can't,
for example, and in general the latter language has much more documentation, a more active
community, and many more programmers. Ultimately, it would've  been a big reset to change
code bases and languages all at once, so I'll continue to write the first  version in Ruby until
I get ~~an excuse~~ a requirement to port to Python. One such requirement could come in the form
of other teams we depend  on and wish to integrate tightly with using some other language.

### Task Classes Instead of Scripts
I  decided to make the tasks full  Ruby classes instead of stand-alone scripts for a couple  of reasons.
First, it ensures that we can minimize usage of global state (which can get away from you easily
in Ruby). Second, we can now configure things  like log levels in one place.

### Independence of All Things All of Us
There are no dependencies on any AoU application or build/release code, or anything in
the utils repository. This allows us to avoid refactoring the legacy Ruby stack, and ensures
that this framework can be reused externally. Some notably absent things:
* `project.rb` System: This system depends on and is depended on by our  build & release framework, 
which means we would be in danger of holding up a release if we needed to make a quick change.
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
