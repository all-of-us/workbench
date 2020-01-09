#Static Files in AppEngine
We host a number of public static files in GAE for cluster creation. These files are copied at build time from our cluster resources directory. Eventually (when we no longer support bucket extensions), these files should move into this directory and we should no longer gitignore them from here.

##Adding a new file
To add a new file, if it is an existing file in cluster-resources, add it as a step to the build.rb script within the cluster resources, and then add it to the .gitignore file. If it is not a cluster resource, copy on build if it exists, or just add here and don't add to the git ignore if it is a new file.

##Local Testing
To test out locally, deploy a versioned server to appengine, and go to `{{VersionedURL}}/static/{{Filename}}` to try it out.
