## Developer System Requirements
### Operating System
We currently only support MacOS and Linux for development and testing.
### Prerequisite Software
  * [Java 17](https://adoptium.net/temurin/releases/)
    * This must be installed, but needn't be the default.
    * See [Developer System Initialization](developer-system-initialization.md) for configuration if your
      system has a different default version of Java
  * [Docker CE](https://www.docker.com/community-edition)
    * Docker must be installed to build and run code (For Google workstations, see http://go/installdocker).
    * __IMPORTANT__: be sure to allocate ~70-80% of available memory and swap to the Docker Engine. This should be
      at least 12GB memory and 2GB swap to avoid OOM isues. See [the official instructions](https://docs.docker.com/docker-for-mac/#advanced)
      for screenshots and instructions for Mac.
  * [Ruby](https://www.ruby-lang.org/en/downloads/)
    * Our team's dev/ops scripts are written in Ruby. Most common operations are launched via the project.rb script at the root of each sub-project.
    * You may need to add ruby gems to your path if things are not working. If so follow [these instructions](https://guides.rubygems.org/faqs/#i-installed-gems-with---user-install-and-their-commands-are-not-available).
  * [Python](https://www.python.org/downloads/) >= 3.5
    * Python is required by some project-specific scripts and by the Google Cloud Platform tools.
  * [gcloud](https://cloud.google.com/sdk/docs/#install_the_latest_cloud_tools_version_cloudsdk_current_version)
    * Command-line interface for Google Cloud Platform.
    * Recent versions of gcloud are expected to work. However, a specific version is used in
      CircleCI, which is guaranteed to work: https://github.com/all-of-us/workbench/blob/main/ci/Dockerfile.circle_build
    * For use with Stackdriver alert policies and certain other new features you may
    need to install `alpha` or `beta` channels via 
    ```text
    gcloud components install alpha
    ```

For local development, also install:

  * [yarn](https://yarnpkg.com/lang/en/docs/install/#mac-stable)
  * [Node.js](https://nodejs.org/en/) >= 18.  Currently known to work up to 20.9.0
  * `envsubst` which is part of [gettext](https://www.gnu.org/software/gettext/)
    * Mac: `brew install gettext`

### Initialization
Next, follow the steps in [Developer System Initialization](developer-system-initialization.md).
