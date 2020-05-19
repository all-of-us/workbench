## Developer System Requirements
### Operating System
We currently only support MacOS and Linux for development and testing.
### Prerequisite Software
  * [Docker CE](https://www.docker.com/community-edition)
    * Docker must be installed to build and run code (For Google workstations, see http://go/installdocker).
    * __IMPORTANT__: be sure to allocate ~70-80% of available memory and swap to the Docker Engine. This should be
      at least 12GB memory and 2GB swap to avoid OOM isues. See [the official instructions](https://docs.docker.com/docker-for-mac/#advanced)
      for screenshots and instructions for Mac.
  * [Ruby](https://www.ruby-lang.org/en/downloads/)
    * Our team's dev/ops scripts are written in Ruby. Most common operations are launched via the project.rb script at the root of each sub-project.
  * [Python](https://www.python.org/downloads/) >= 2.7.9
    * Python is required by some project-specific scripts and by the Google Cloud Platform tools.
  * [gcloud](https://cloud.google.com/sdk/docs/#install_the_latest_cloud_tools_version_cloudsdk_current_version)
    * Command-line interface for Google Cloud Platform.
    * For use with Stackdriver alert policies and certain other new features you may
    need to install `alpha` or `beta` channels via 
    ```text
    gcloud components install alplha
    ```
    

For local development, also install:

  * [yarn](https://yarnpkg.com/lang/en/docs/install/#mac-stable)
  * [Node.js](https://nodejs.org/en/) >= 8.  Currently known to work up to 12.16.
  * [Java 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
  * [Docker sync](https://docker-sync.io)
    * `sudo gem install docker-sync`
    * If you'd prefer to install as non-root, you can [follow instructions for user-level install](https://docker-sync.readthedocs.io/en/latest/getting-started/installation.html).

## git-lfs

### Setup

Download the git-lfs tool.
If you are on a mac, run:
```Shell
  brew install git-lfs
```

Enable git lfs in the top level directory.
```Shell
  git lfs install
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
