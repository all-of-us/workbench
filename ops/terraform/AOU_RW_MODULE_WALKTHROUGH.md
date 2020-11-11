# AoU Researcher Workbench Module  Walkthrough
## 0. Module Structure
The state associated with the current deployment  consists of
one `root` module for each environment, in separate directories

In  order to deploy a full (or partial) environment we need to declare what modules are used and to supply
values to all unbound declared variables. The environment module is  unioned with the modules in the
`source` statement.

The overall source structure looks like the following. Note that 
Terraform will collect all `.tf` files in a referenced directory,
so the calling module will need to specify values for the chilid
modules' `variable` blocks that don't have defaults.

```text
/repos/workbench/ops/terraform/
├── AOU_RW_MODULE_WALKTHROUGH.md
├── TERRAFORM-QUICKSTART.md
├── environments
│   ├── local
│   ├── scratch
│   │   ├── SCRATCH-ENVIRONMENT.md
│   │   ├── scratch.tf
│   │   ├── terraform.tfstate
│   │   ├── terraform.tfstate.backup
│   │   └── terraform.tfstate.yet.another.backup
│   └── test
└── modules
    └── aou-rw-reporting
        ├── providers.tf
        ├── reporting.tf
        ├── schemas
        │   ├── cohort.json
        │   ├── institution.json
        │   ├── user.json
        │   └── workspace.json
        ├── variables.tf
        └── views
            ├── latest_cohorts.sql
            ├── latest_institutions.sql
            ├── latest_users.sql
            ├── latest_workspaces.sql
            └── table_count_vs_time.sql
```
The `modules` directory contains independent, reusable modules foor
subsystems that are 
* logical to deploy and configure operationally,
* don't depend on each other (at least for exported modules) and
* can be used by AoU or potentially another organization interested in deploying a copy
of all or part of our system.

## Prerequisites
### 1. Get Terraform
Install Terraform using the directinos at [TERRAFORM-QUICKSTART.md]
### 2. Change to the `environments/scratch directory` `get` and `init`
The environment for this outline is `scratch`, which exists in a target environment
of your choice. 
### 3. Assign Values to Input Variables

The following public variable declarations are representative of those
specified in `modules/reporting/variables.tf` and elsewhere. The description
string shows when interactively running from the command line without all the
vars cominig in from a `-var-file` argument.
```hcl-terraform
variable credentials_file {
  description = "Location of service account credentials JSON file."
  type        = string
}

variable aou_env {
  description = "Short name (all lowercase) of All of Us Workbench deployed environments, e.g. local, test, staging, prod."
  type        = string
}

variable project_id {
  description = "GCP Project"
  type        = string
}
```
Create a `scratch_tutorrial.tfvars` file outside of this repository. This file should
look contain values for the following [input variables](https://www.terraform.io/docs/configuration/variables.html) that will be different
for different organizations and environments.

```hcl-terraform
aou_env = "scratch" # Name of environment we're creating or attaching to. Needs to match directory name
project_id = "my-qa-project" # Should not be prod
reporting_dataset_id = "firstname_lastname_scratch_0" # BigQuery  dataset id
```

The credentials file should point to a JSON key file generated
by Google Cloud IAM (at least on lower environments). The only required
permission is `BigQuery Data Owner` Neither the credentials nor
the `.tfvars` file itself should be checked into public source control.

It's sometimes helpful to assign the full path to this `.tfvars` to an environment variable,
as it will need to e provided for most commands. There are several other ways to do this, 
but the advantage for us is separating the reusable stuff from the AoU-instance-specific
values.
```shell script
$ SCRATCH_TFVARS=/rerpos/workbench-devops/terraform/scratch.tfvars
```

### 4. Initialize Terraform
Run [`terraform init`](https://www.terraform.io/docs/commands/init.html) to initialize the current directory (which should be 
`api/terraform/environrments/scratch` if working from this repo. It should also be possible
work from a directory completely sepaated from source control. It's just 
a bit harder to refer to the module definitions.

If `init` was successful, the following message should print something like the following
like following:
```
Initializing modules...

Initializing the backend...

Initializing provider plugins...
- Using previously-installed hashicorp/google v3.5.0

Terraform has been successfully initialized!

You may now begin working with Terraform. Try running "terraform plan" to see
any changes that are required for your infrastructure. All Terraform commands
should now work.

If you ever set or change modules or backend configuration for Terraform,
rerun this command to reinitialize your working directory. If you forget, other
commands will detect it and remind you to do so if necessary.
```

After successfully `init`, while the backend, plugins, and modules are now in a reasonably good state,
but certain expensive operations are deferred for performance. Look at the `terraform.tfstate` file
in the run directory to confirming nothing is of intererst there:
```json
{
  "version": 4,
  "terraform_version": "0.13.0",
  "serial": 24,
  "lineage": "d9d8e034-fad0-03ff-df40-86bdd7a43128",
  "outputs": {},
  "resources": []
}
```
 
### 5. Build a Plan
Terraform creates a plan of action based on the difference between its view of the state
of all the resources, and what's stated in the file.

Run like
```
terraform plan -var-file=$SCRATCH_TFVARS
```
The output for me looks like [this](doc/plan_output.txt). You should see a couple of key things:
*  A dataset, several tables, and some views will be created. Searching for "will be created" is an easy way to
see this.
* All the variables are expanded in the state file, so treat this file as Eyes Only.
* The summary line should show `Plan: 10 to add, 0 to change, 0 to destroy.`

The `plan` command doesn't edit actual resources, but is important for understanding Terraform's marching
orders.

### 6. Apply the Plan
Use the `apply` command to make the chagnes necessary. It will ask you for a `yes` confirmation beofre proceeding.
In the sase of the reporting module, creating the dataset then immediately crerating tabales may mean
that we need to run one more time. Luckily, `apply` is idempotent for this case and there's no harm.

Once everything is applied, rerunning `tf plan` will show that nothing is left to do:
```
$ tf plan -lock=false -var-file=$SCRATCH_TFVARS
Refreshing Terraform state in-memory prior to plan...
The refreshed state will be used to calculate this plan, but will not be
persisted to local or remote state storage.

module.aou_rw_scratch_env.module.reporting.google_bigquery_dataset.main: Refreshing state... [id=projects/all-of-us-workbench-test/datasets/jaycarlton_terraform_tmp_2]
module.aou_rw_scratch_env.module.reporting.google_bigquery_table.view["latest_cohorts"]: Refreshing state... [id=projects/all-of-us-workbench-test/datasets/jaycarlton_terraform_tmp_2/tables/latest_cohorts]
module.aou_rw_scratch_env.module.reporting.google_bigquery_table.main["institution"]: Refreshing state... [id=projects/all-of-us-workbench-test/datasets/jaycarlton_terraform_tmp_2/tables/institution]
module.aou_rw_scratch_env.module.reporting.google_bigquery_table.view["table_count_vs_time"]: Refreshing state... [id=projects/all-of-us-workbench-test/datasets/jaycarlton_terraform_tmp_2/tables/table_count_vs_time]
module.aou_rw_scratch_env.module.reporting.google_bigquery_table.view["latest_institutions"]: Refreshing state... [id=projects/all-of-us-workbench-test/datasets/jaycarlton_terraform_tmp_2/tables/latest_institutions]
module.aou_rw_scratch_env.module.reporting.google_bigquery_table.view["latest_users"]: Refreshing state... [id=projects/all-of-us-workbench-test/datasets/jaycarlton_terraform_tmp_2/tables/latest_users]
module.aou_rw_scratch_env.module.reporting.google_bigquery_table.main["cohort"]: Refreshing state... [id=projects/all-of-us-workbench-test/datasets/jaycarlton_terraform_tmp_2/tables/cohort]
module.aou_rw_scratch_env.module.reporting.google_bigquery_table.view["latest_workspaces"]: Refreshing state... [id=projects/all-of-us-workbench-test/datasets/jaycarlton_terraform_tmp_2/tables/latest_workspaces]
module.aou_rw_scratch_env.module.reporting.google_bigquery_table.main["user"]: Refreshing state... [id=projects/all-of-us-workbench-test/datasets/jaycarlton_terraform_tmp_2/tables/user]
module.aou_rw_scratch_env.module.reporting.google_bigquery_table.main["workspace"]: Refreshing state... [id=projects/all-of-us-workbench-test/datasets/jaycarlton_terraform_tmp_2/tables/workspace]

------------------------------------------------------------------------

No changes. Infrastructure is up-to-date.

This means that Terraform did not detect any differences between your
configuration and real physical resources that exist. As a result, no
actions need to be performed.
```

### 7. Selectively removing state
If it's necessary to detach one or more online resources from the local Terraform state (as if it has
never been created or imported), use the `terraform state rm` command. The general pattern is
`terraform remove tfitem_id cloud_id`. For example, let's say I've decided I no longer want the view
named `latest_workspaces` to be included in the state file.

### 8. Handy State commands
The [state command](https://www.terraform.io/docs/commands/state/index.html) is one of the more powerful ones to use, and lets you avoid interacting directly with `.tfstate`
files.
#### Import
Working with resources tha already exist requires `terraform import` command. This seems unintuitive,
but the sample `tarraform state list` output shows what's expected. Third party modules should show
the expected syntx. For [importing a BigQuery dataset](https://www.terraform.io/docs/providers/google/r/bigquery_dataset.html#import)
from the `scratch` environment to the `local` environment, simply do;

```shell script
terraform import -var-file=$TFVARS_LOCAL \
    module.local.module.reporting.google_bigquery_dataset.main \
    reporting_local
``` 
The output should look like this if successful. There are several failure modes involving directory structure,
module path, and differing asset ID configurations for different providers. 

```
terraform import -var-file=$TFVARS_LOCAL module.local.module.reporting.google_bigquery_dataset.main reporting_local
module.local.module.reporting.google_bigquery_dataset.main: Importing from ID "reporting_local"...
module.local.module.reporting.google_bigquery_dataset.main: Import prepared!
  Prepared google_bigquery_dataset for import
module.local.module.reporting.google_bigquery_dataset.main: Refreshing state... [id=projects/my-project/datasets/reporting_local]

Import successful!

The resources that were imported are shown above. These resources are now in
your Terraform state and will henceforth be managed by Terraform.
```

`tf state` should now show that we are managing the resource:
```shell script
tf state list
module.local.module.reporting.google_bigquery_dataset.main
```

```shell script

terraform import -var-file=$TFVARS_LOCAL module.local.module.reporting.google_bigquery_table.main[\"cohort\"] \projects/all-of-us-workbench-test/datasets/reporting_local/tables/cohort
terraform import -var-file=$TFVARS_LOCAL module.local.module.reporting.google_bigquery_table.view[\"latest_users\"]  projects/all-of-us-workbench-test/datasets/reporting_local/tables/latest_users
```
is an example of importing a table. remember that equation marks must be escaped.

```shell script
$ tf state list
module.local.module.reporting.google_bigquery_dataset.main
module.local.module.reporting.google_bigquery_table.main["cohort"]
```
None of the `teraform state` commands accept variable values, as those have already been interpolated
during a `plan` or `apply` operation.

**NOTE** While Terraform is managing the dataset, it's not yet managing any data in it directly.
Running `tf plan` at this point will indicate that, while the dataset is controlled, the tables and
views in it are not. It's probalby not a good idea to `terraform destory` imported resources that
contain other resources you care about; always study the `plan` output carefully.
#### `state list`
`terraform state list` lists all modules and resources under management for the current module. It's
especially handy when trying to find the desired module path string for `import` if you're reusing a
oonfiguration for another environment or system.
#### `state show`
terraform show is a more detailed listing for a given item in the state tree. The comm
```
terraform state show module.local.module.reporting.google_bigquery_dataset.main
```

#### `state pull`
To show the active state file (by default named `terraform.tfstate`), simply do
 ```terraform state pull | jq```.
The `jq` command makes the JSON colorized, though it already has a nice structure.

I don't know why you'd use `terraform push`, which applies state that's externalized as JSON somehow.
Likely an advanced feature.

#### `state rm`
The opposite of `state import`, the `state rm` subcommand removes a tracked resource from the
Terraform state file. Some uses for this are for repairing configurations, spliting them up,
or allowing someone else to experiment with changes on a deployed artifact before bringing it
back under control. Happily, this command does not `destroy` objects when removing them.
