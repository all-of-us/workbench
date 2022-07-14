## Environment Setup for WGS cohort extraction

1. Create wgs-cohort-extraction service account.   
    - This should match the value in WorkbenchConfig.wgsCohortExtraction.serviceAccount
    - This will have to go through sysadmins for non test environments

2. Register service account as a Terra user 
     - ```
        curl \
        -H "Authorization: Bearer $(gcloud auth --impersonate-service-account EXTRACTION_SA print-access-token)" \
        -H "Content-Type: application/json" \
        -X POST -d' ' \
        https://sam.dsde-dev.broadinstitute.org/register/user/v1

3. Create extraction billing project and workspace
    - `./project.rb create-wgs-cohort-extraction-bp-workspace --project all-of-us-workbench-test --billing-project-name aou-terra-ops-test-10 --workspace-name aou-terra-ops-test-workspace-10 --owners eric.song@pmi-ops.org,songe@broadinstitute.org`
    - Make note of the proxy group and pet service account that the script will print out at the end

4. Grant pet service account some permissions through Terraform
    - `bigquery.readSessionUser` and `bigquery.jobUser` on all researcher projects
    - writer access on all researcher project buckets
    - add to VPC whitelist
    - See https://github.com/broadinstitute/terraform-terra/pull/143/files as an example for the test environment

5. Create Workflow and share with wgs-cohort-extraction service account
    - I did this by logging in to Terra through the account shared in Step 3 (`--owners`).
    - With Agora, you will need to share the workflow with the wgs-cohort-extraction service account so that it can read the workflow and create method configurations referring to it.
    - Have not tried Dockstore yet
    - Match the name, namespace, version values with the corresponding fields in WorkbenchConfig.wgsCohortExtraction.
    - **Make sure the pet service account can pull the docker image references in the Workflow  
        - Grant it read permissions to the GCR bucket, https://cloud.google.com/container-registry/docs/access-control#grant

6. Create VPC-SC BigQuery datasets that will be used by the extraction workflow and add them to WorkbenchConfig.wgsCohortExtraction. 
    - All of these datasets will contain tables with a TTL; they are only used during the extraction process.
    - We can grant the permission to the proxy group which is available from Step 3.
        - the pet SA account will also work, but the proxy group is more permissive and will apply to all workspaces created by the wgs service account  
    - These are the names I used in test but they can be anything. (configField - datasetName)
    - extractionDestinationDataset - wgs_extracted_cohorts
    - `./project.rb create-wgs-extraction-dataset  --project all-of-us-workbench-test --bq-dataset wgs_extraction_temp_tables`

7. Publish the WGS dataset and grant read permissions to the cohort extraction service account's proxy group
    - This is the WGS equivalent to publishing a CDR so this is not necessary for every environment.
    - This is different from the Step 6 command since we only need read permissions and we do NOT want a TTL.
    - `./project.rb publish-cdr-wgs --project all-of-us-workbench-test --bq-dataset 1kg_wgs`
    - This is what I did to publish the test WGS data but the process may be different once we get to real data.
    - Notify Data Apps of any changes to the test dataset or schema
