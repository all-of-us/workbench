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
    * You may need to add ruby gems to your path if things are not working. If so follow [these instructions](https://guides.rubygems.org/faqs/#i-installed-gems-with---user-install-and-their-commands-are-not-available).
  * [Python](https://www.python.org/downloads/) >= 2.7.9
    * Python is required by some project-specific scripts and by the Google Cloud Platform tools.
  * [gcloud](https://cloud.google.com/sdk/docs/#install_the_latest_cloud_tools_version_cloudsdk_current_version)
    * Command-line interface for Google Cloud Platform.
    * For use with Stackdriver alert policies and certain other new features you may
    need to install `alpha` or `beta` channels via 
    ```text
    gcloud components install alpha
    ```
    

For local development, also install:

  * [yarn](https://yarnpkg.com/lang/en/docs/install/#mac-stable)
  * [Node.js](https://nodejs.org/en/) >= 8.  Currently known to work up to 12.16.
  * [Java 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
  * [Docker Compose](https://docs.docker.com/compose/install/)
  * [Docker sync](https://docker-sync.io)
    * `sudo gem install docker-sync`
    * If you'd prefer to install as non-root, you can [follow instructions for user-level install](https://docker-sync.readthedocs.io/en/latest/getting-started/installation.html).

### Initialization
Next, follow the steps in [Developer System Initialization](developer-system-initialization.md).
