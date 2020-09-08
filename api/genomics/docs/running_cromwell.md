## Running Cromwell on Terra using AoU Billing

1. Ask sysadmins for BillingAccountUser role on your pmi-ops account. (Clone https://precisionmedicineinitiative.atlassian.net/browse/PD-5427)
2. Create an account on https://app.terra.bio using your pmi-ops Google account
3. Set up billing project on https://app.terra.bio/#billing, name can be w/e but select "VUMC PMI DRC P0 Account" for billing account.
4. Create a workspace, use the billing project you just created
5. Go to the "Workflow" tab in your workspace
6. Click "Find a Workflow"
7. Click "Broad Methods Repository" at the bottom of the modal. Authenticate with your pmi-ops account if asked
8. Click "Create New Method"  
   - Namespace = namespace from your Terra workspace
   - Name = name of your method, can be w/e
   - WDL = Your WDL. I'll provide a Hello World wdl that you can run for testing
9. Click "Upload"
10. Click "Export to Workspace..."
11. Click "Use Blank Configuration"
    - Destination Workspace = your Terra workspace
12. Click "Export to Workspace"
13. Go back to the "Workflows" tab in your Terra workspace
14. Click on the Workflow you just created
15. Select "Run workflow with inputs defined by file paths" radio button
16. Enter your input attributes, remember to wrap strings and file paths with double quotes
17. Click "Save"
18. Click "Run Analysis" 
19. Click "Launch"

You should now be on a page within "Job History" which will monitor your job. I believe the status will automatically update periodically.

Troubleshooting
- If you run into an access error with docker pull, you're probably trying to access a private GCR image. Determine the GCS bucket that contains the image, https://cloud.google.com/container-registry/docs/access-control#grant-bucket. And grant viewer access to that bucket to the "Proxy Group" user which can be found in your terra profile, https://app.terra.bio/#profile. 
