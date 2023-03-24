# Developer System Initialization
This is a guide to some first-time activities to run after installing the
software in [System Requirements](developer-system-requirements.md).

## Gradle setup

If your system's default `java` version is 11, you can skip this.

```Shell
java --version
openjdk 17.0.5 2022-10-18
OpenJDK Runtime Environment GraalVM CE 22.3.0 (build 17.0.5+8-jvmci-22.3-b08)
OpenJDK 64-Bit Server VM GraalVM CE 22.3.0 (build 17.0.5+8-jvmci-22.3-b08, mixed mode, sharing)
...
```

Our build requires Java 11.

- Ensure Java 11 is installed on your system
- Find Java 11 home directory, e.g. on mac run: `/usr/libexec/java_home -v 11`
- Create a Gradle properties file to point to this directory:
  - Globally for your workstation: `~/.gradle/gradle.properties`
  - or, just in the Workbench repo: `api/gradle.properties`
  ```
  # gradle.properties
  org.gradle.java.home={YOUR_PATH_TO_JDK11}

  # Example:
  $ cat api/gradle.properties
  org.gradle.java.home=/usr/local/buildtools/java/jdk11
  ```

## Gcloud Setup
After you've installed `gcloud`, login using your `pmi-ops` account:

```shell
gcloud auth login
```
Earlier editions of this document recommended choosing a default project, but this is
no longer considered a best practice. Since we have many nearly identical and similarly-named
GCP projects, you should instead pass `--project=all-of-us-workbench-test`
to every `gcloud` command. The reasoning is that by using default projects via
`gcloud config set project` you introduce the ability to operate on the wrong project
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
git submodule update -f --init --recursive
```

## .npmrc

In the UI, we use some [Fontawesome](https://fontawesome.com/) Pro icons. In order to install the Fontawesome Pro packages, you will need to download an `.npmrc` file with a Fontawesome license token. Copy it into the workbench root directory:
```
workbench$ gsutil cp gs://all-of-us-workbench-test-credentials/.npmrc .
```

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

## Initial Smoke Test
Before doing any development, you must run the following from `/api`:
```Shell
./gradlew compileJava
```
Before compiling, this will also generate Java files from MapStruct and OpenAPI (Swagger) 
definitions which the app requires to compile. Depending on your personal development environment,
you may need to frequently run `./gradlew clean compileJava` to pick up MapStruct/OpenAPI changes, 
particularly when changing branches.
