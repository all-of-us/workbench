## How to update the WGS Extraction WDL

### Testing  
1. Create a new branch in GATK off of the `ah_var_store` branch
2. Make your WDL changes in the GATK branch
3. Commit and upload your changes to Github
4. Upload your changes to Agora using the cmd line tool
    1. `./project.rb create-terra-method-snapshot --source-git-repo broadinstitute/gatk --source-git-path scripts/variantstore/wdl/GvsExtractCohortFromSampleNames.wdl --source-git-ref songe/add-manifest --method-name manifest --project all-of-us-workbench-test`  
       1. The `--source-git-ref` paramater must match the branch name created in step 1. 
       2. The `--method-name` can be any value that you're using for testing. 
       3. Make note of the snapshot identifiers in the output `Ex. New snapshot namespace/name/version: aouwgscohortextraction-test/manifest/3`
5. Update config_local.json to match the new snapshot namespace/name/version in the output of the previous step
6. Start the local development environment 
7. Create a cohort (ideally smaller than 10 participants so that it finishes in ~1 h)
8. Create a dataset using the cohort and select the “All whole genome variant data” concept set
9. Run “Extract VCF Files” from the dataset card or by clicking through “Analyze”
10. Wait for it to finish. ETA ~1 h

### Debugging
1. Prereq - you need access to the Terra workspace specified by `wgsCohortExtraction.operationalTerraWorkspaceName____` in your environment config.
2. Log into Terra and open that workspace
3. Go to Job History
    1. The extraction you started should show up in the Job History table. Click into a row to see details and use Terra’s cromwell debugging tools. I personally find the Google Cloud Storage browser to be the most useful.

### Merging
1. Create a PR with your GATK branch
2. Get it reviewed by the Variants team and merge it
3. Upload the new snapshot to all environments using the `--all-projects` parameter of `create-terra-method-snapshot`
   1. `./project.rb create-terra-method-snapshot --source-git-repo broadinstitute/gatk --source-git-path scripts/variantstore/wdl/GvsExtractCohortFromSampleNames.wdl --source-git-ref ah_var_store —method-name GvsExtractCohortFromSampleNames —all-projects`
      1. The `--source-git-ref` should match the branch that your changes were merged into
      2. The `--method-name` parameter can be anything but our convention has been to use the WDL workflow's name.
   2. Update the config_*.json files. The version number for each environment will be in the output of the previous step.
