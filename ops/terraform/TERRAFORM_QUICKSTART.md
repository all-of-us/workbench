# Terraform Quickstart
The [official documentation](https://www.terraform.io/) for Terraform
is quite readable and exposes the functionality and assumptions at a good pace.
In particular, I found the [Get Started - Google Cloud](https://learn.hashicorp.com/collections/terraform/gcp-get-started) guide to  be very helpful. 

It's worth making an alias for terraform and putting it in your `.bash_profile` or other shell init file, as
it's difficult to spell `terraform` correctly when caffeinated.
```shell script
alias tf='terraform'
```
The above tip also serves as a warning and non-apology that I'm going to forget to spell out the
command name repeatedly below.

## Installation
For the work so far, I've used the [Terraform CLI](https://www.terraform.io/docs/cli-index.html), which has the  advantage of not costing
money or requiring an email registration. On the mac, `brew inistall terraform` is pretty much all it takes. 

Terraform works by keeping state  on the local filesystem for evaluating diffs and staging changes. Primary files for users to author
and check in to source control are:
* main.tf - listing providers and specifying Terraform version  and other global options
* <subsystem_name>.tf - list of resources and their properties and dependencies. This file can reference any other .tf flies in the local directory.
* variables.tf - any string, numeric, or map variables to be provided to the script.
* external text files - useful files with text input, such as BigQuery table schema JSON files

Output files provided Terraform (and not checked in to source control) include
* tfstate files - a record of the current known state of resources under Terraform's control.

## Organization
Terraform configuration settings are reusable for all environments (after bvinding environment-specific
variables in `.tfvars` files). The reuse is provided by Terraform
## Running
If you have a small change to make to a resource under Terraform's management, in the simplest case the workflow is
* Run `terraform init` to initialize the providers
* Run `terraform state list` to list all  artifacts currently known and managed by Terraform within
the scope of the `.tf` files in the current directory.
* Run `terraform show` to view the current  state of the (managed) world, and check any errors.
* change the setting in the tf file (such as reporting.tf). 
* Run `terraform plan` to see the execution plan. This can be saved with the `-out` argument in
situations where it's important to apply exactly the planned changes. Otherwise, new changes to the
environment might be picked up in the `apply` step, giving possibly significantly different behaviors
than were expected based on the `plan` output.
* Run `terraform apply` to execute the plan and apply the changes. You'll need  to type "yes" to
 proceed with the changes (or use `-auto-approve` in a non-interactive workflow.)
* Check in changes to the terraform file.

## Managing Ownership
### Importing resources
Frequently, resources to be managed already exist. By default, Terraform will try to re-create them
if they're added to a configuration and fail because the name or r other unique identifier is already  in use.
Using `terraform import` allows the existing resource to be included
in the `tfstate` file as if Terraform created it from scratch. 

### Removing items from Terraform
Occasionally, it's desirable to remove a resource form Terraform state. This can be helpful when reorganizing
resources or `tf` files. The `terraform state rm` command accomplishes this, and moves those resources
into a state where Terraform doesn't know  it either created or owned them. The
[official](https://www.terraform.io/docs/commands/state/rm.html) do are pretty good for this.

## Good Practices
### Formatting
A builtin linter is available with the `terraform fmt` command. It spaces assignments in clever ways
that would be difficult to maintain by hand, but that are easy to read. It's easy to set up in IntelliJ
by installing the FileWitchers plugin and adding a Terraform Format action. Runs fast,too.

### Labels
It's handy to have a human-readable label called `managedByTerraform` and set it to `true` for all TF artifacts.
It's possible to set up default labels and things for this.
### Local Variables
Using a `locals` bock allows you to assign values (computed once) to variables to be used elsewhere. This
is especially useful for nested map lookups: 
```hcl-terraform
locals {
  project = var.aou_env_info[var.aou_env]["project"]
  dataset = var.aou_env_info[var.aou_env]["dataset"]
}
```

Later, simply reference the value by `dataset_id = local.dataset`. Note that these "local" variables
are available to other `.tf` files, but apparently, since things are all initialized at once and immutable,
it doesn't really matter whether you define them in `chicken.tf` or `egg.tf`. It just works as long
as both files are part of the same logical configuration.

It's useful in some cases to specify `default` values for the resources in use, but it's advisable to
force the user to specify certain fundamental things (such as the AoU environment) every time in order
to avoid migrating the wrong environment prematurely (such as removing artifacts that code running on
that environment expects to be there).

### Starting with a scratch state collection
It's much faster to work Terraform-created artifacts, properties, etc, than to attach to existing infrastructure.
For this purpose, it can be handy to add new BigQuery datasets just for the development of the configuration,
capture resource and module identifiers for import, and then tear down the temporary artifacts with `terraform destroy`.

### Use Modules
[Modules](https://www.terraform.io/docs/configuration/modules.html) are the basis of reuse,
encapsulation, and separation of concerns in Terraform. Frequently, the provider (such as Google
Cloud Platform) has already written handy base modules that provide reasonable
defaults, logical arrangement of resources, and convenient output variable declarations.

### Separate Private Vars from Community-use Settings
Names of artifacts, deployments (such as test and staging), service accounts, or other pseudo-secrets
should be kept separate from the primary module definitions outlining behavior. For example, looking
at the reporting project, we have:
* public: table schemas, names, and clustering/partitioning settings
* public: view queries (with dataset and project names abstracted out)
* private: names of AoU environments (currently exposed in several places publicly, but of no legitimate
use to the general public)
* private: BigQuery dataset names. We have a simple convention of naming it after the environment,
but this isn't a contract enforced by our application code or the Terraform configurations.

Why do we include the environment name in the dataset name (as opposed to just calling it `reporting`) in every
environment? Firstly, we have two environments that share a GCP project, so we would have a name clash.
More fundamentally, though, is that it would be too easy to apply a query to a dataset in the wrong environment
if it simply referred to `reporting.workspace` instead of `reporting_prod.workspace`, as the BigQuery
console lets you mix datasets from multiple environments as long as you have the required credentials. In most
cases, I'd argue against such inconsistent resource naming.

### Don't fear the `tfstate` file
Despite the scary name, the contents of `tfstate` are in JSON, and largely readable. You can operate
on it with utilities such as `jq`

```shell script
$ jq '.resources[0].instances[0].attributes.friendly_name' terraform.tfstate
"Workbench Scratch Environment Reporting Data"
```

I'd keep any operations read-only whenever possible, but I have a feeling one of the keys to mastering
Terraform will be understanding the `tfstate` file.
## Gotchas
## A Terra by any other name
[Terra](https://terra.bio/) and [Terraform](https://www.terraform.io/) are different things, and for
the most part going to one organization for help with the other's platform will result in bemusement
at best. Good luck differentiating them on your resume.

### Mis-configuring a tfstate file
The file really shouldn't be checked into source contol, because
it's not safe to have multiple developers working with it. It's too easy to getinito an inconsistent view of the world.

However, that doesn't mean it's safe to lost track of the tfstate JSON file altogether.
When working with multiple people, a shared   online backend  with locking is really
required.

### Using two terminals in the same terraform root module working directory.
Frequent error messages about the lock file and how you can use `lock=fale` but should really never
do so. It's basically that two processes think they own something in `.terraform/`. So don't do that.

### Using `terraform state show` with `for-each` or an array-declared value.
When creating many items of hte same type at the same level/scope, it's useful to use arrays or 
`for-each`. However, the syntax for `tf state show` is trickier because you need to pass a double-quoted
string index from the command line.

Given the following output of `terraform state list`:
```
$ tf state list
module.bigquery_dataset.google_bigquery_dataset.main
module.bigquery_dataset.google_bigquery_table.main["cohort"]
module.bigquery_dataset.google_bigquery_table.main["user"]
module.bigquery_dataset.google_bigquery_table.main["workspace"]
module.bigquery_dataset.google_bigquery_table.view["latest_users"]
```
The naive approach gives you this [cryptic error message](https://github.com/hashicorp/terraform/pull/22395).
```
$ tf state show module.bigquery_dataset.google_bigquery_table.main["cohort"]
Error parsing instance address: module.bigquery_dataset.google_bigquery_table.main[cohort]

This command requires that the address references one specific instance.
To view the available instances, use "terraform state list". Please modify 
the address to reference a specific instance.

```
The approach that seems to work in Bash is
```
 terraform state show ¨
```

### Cloud not quite ready to use newly created resource
When creating a new BigQuery dataset with tables and views
all at once, I once  run into an issue where the new  table
wasn't  ready for a view creation yet. The error message was
```
Error: googleapi: Error 404: Not found: Table my-project:my_dataset.user, notFound

  on .terraform/modules/aou_rw_reporting/main.tf line 76, in resource "google_bigquery_table" "view":
  76: resource "google_bigquery_table" "view" {
```

Re-running `terraform apply` fixed this.
### Renaming  files and  directories
It's really easy to refactor yourself into a corner by renaming modules or directories in their paths.
If you see this  error,  it  probably means you've moved something in the local filesystem  that  the
cached state was depending on.
```
Error: Module not found

The module address
"/repos/workbench/ops/terraform/modules/aou-rw-reporting/"
could not be resolved.

If you intended this as a path relative to the current module, use
"/repos/workbench/ops/terraform/modules/aou-rw-reporting/"
instead. The "./" prefix indicates that the address is a relative filesystem
path.
```
So the last chance to rename things  relatively is just before you've created them and people are depending on them in prod.
It not really easy to rework your tf  files after deployment. (Another good reason for a scratch project).

### Running in wrong terminal window
If things get created on the wrong cloud, that's not good. I was really confused when I tried running
the AWS tutorial tf file. `tf destroy`  is cathartic in such situations. I'm not  even sure it's OK to use  two
terminals in the same root module at once.

### Using new BigQuery resources
The BigQuery console UI frequently doesn't list all of the new datasets for several minutes, so using
`bq show` is helpful if you want to see things  "with  your own eyes after tf operation".

### Yes Man
If you "yes" out of habit but `terraform apply` or `destroy` bailed out earlier than the prompt,
you see a string of `y`s in  your terminia. I nearly filed a bug for this, but then realized the `yes`
command with no argument does that for all time (at least, so far...).
