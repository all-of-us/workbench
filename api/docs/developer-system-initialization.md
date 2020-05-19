# Developer System Initialization
## Gcloud Setup
After you've installed `gcloud`, login using your `pmi-ops` account:

```shell
gcloud auth login
```
Earlier editions of this document recommended choosing a default project, but this is
no longer considered a best practice. Since we have many nearly identical and similarly-named
projects, you shold instead pass `--project=all-of-us-workbench-test`
to every `gcloud` command. The reasoning is that by using default projects via
`gclod config set project` you introduce the ability to operate on the wrong project
silently.

## Git Repository Initialization
To initialize the Git repository, run the following:

```shell
cd ~/my/repositories/directory/whever
git clone https://github.com/all-of-us/workbench
cd workbench
# We have a couple of dependencies pulled in via Git submodules,
# though this mechanism is being phased out. In the meantime, we need
# to run this on every clone (or on certain changes to the upstream
# repositories). 
git submodule update --init --recursive
```

Then set up [git secrets](#git-secrets) and [git lfs](#git-lfs) and fire up the [development servers](#running-the-dev-servers).


## Initial Smoke Test
Before doing any development, you must run the following from `/api`:
```Shell
./gradlew compileGeneratedJava appengineRun
```
This will generate compiled Java and MapStruct files that are necessary for the app to compile. On Macs, this command will never complete - when it has gotten to 97% it will hang forever. It can safely be `ctrl+c`'d at that point.
