## CircleCI
We use the latest version (2.1) of the CircleCI continuous integration tool.  Here is a [documentation overview](https://circleci.com/docs/2.0/?section=getting-started) and a [config reference](https://circleci.com/docs/2.0/configuration-reference).
    
#### Skipping CI build:
    
    * Adding [ci skip] or [skip ci] string in commit message. This not only skips the marked commit, but also all other commits in the push.
    * Adding `skip e2e` (case-insensitive) in commit message. This skips build on marked commit.
    

#### Short-circuit evaluation: 
    * API/UI tests will not run if file has not changed inside `ui, api, or api/src/main/resources` directories.
    * Puppeteer tests will not run if file has not changed inside `e2e, ui, or .circleci` directories.
    * Puppeteer tests will not run if changed files match patterns specified in `e2e-job-ignore-patterns.txt` file.
     
#### Running tests in parallel:

    * `puppeteer-test` job run Puppeteer end-to-end tests in parallel.
    
    * `api-unit-test` job runs unit tests in parallel.
      
      command: |
        CLASSNAMES=$(circleci tests glob "src/test/java/**/*Test.java" "src/test/java/**/*Test.kt" \
          | cut -c 1- \
          | sed 's@src/test/java/@@' \
          | sed 's@/@.@g' \
          | sed 's/\.[^.]*$//' \
          | circleci tests split --split-by=timings --index=$CIRCLE_NODE_INDEX)
        GRADLE_ARGS=$(echo $CLASSNAMES | awk '{for (i=1; i<=NF; i++) print "--tests", $i }')
        gradle :test $GRADLE_ARGS
    
      
      Command Explanation:
      
      # See https://circleci.com/docs/2.0/language-java/#sample-configuration
      # 
      # "circleci tests split" command provides each CircleCI node with a list of fully qualified test names to execute.
      #  JUnit XML test reports provide tests timing data. CircleCI partition tests based on timing data.
      #
      # Formatting fully qualified test names for gradle and running them with "gradle test" command. Short explanation on commands:
      #   Find all unit tests recursively in "api/src/test/java/" directories.
      #   Strips off "src/test/java/" substring;
      #   Replaces all path separator "/" with ".";
      #   Removes file extensions.
      #   Append "--tests" in front of file name.
      #
      # Example of string before formatting:
      #   src/test/java/org/pmiops/*********/monitoring/LogsBasedMetricsServiceTest.java src/test/java/org/pmiops/*********/monitoring/MonitoringServiceTest.java
      # Example of Gradle command after formatting:
      #   gradle test --tests org.pmiops.*********.monitoring.LogsBasedMetricsServiceTest --tests org.pmiops.*********.monitoring.MonitoringServiceTest
      #
      # We exclude the :genomics:test task below because otherwise gradle will attempt to run all the regular API unit tests under the genomics
      # submodule and will be unable to find them and will barf
 
 