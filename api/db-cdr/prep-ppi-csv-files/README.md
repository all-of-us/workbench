# Creating prep ppi csv files

## How the build works
This python script parses all surveys manually downloaded from redcap to a 
[Google Bucket](https://console.cloud.google.com/storage/browser/all-of-us-workbench-private-cloudsql/cb_prep_tables/redcap;tab=objects?organizationId=394551486437&project=all-of-us-workbench-test&prefix=&forceOnObjectsSortingFiltering=false). 
The output of this script is an attempt to create a survey tree for each survey type for the Cohort Builder. Each survey csv file produced,
will be merged into a final prep_survey table used by the CDR indices build. 

##Install pyenv
`brew install pyenv`

Install the latest version of python

`pyenv install x.x.x`

Set the global default

`pyenv global x.x.x`

Verify it worked

`pyenv version`

In order for it to work correctly, we need to add the following to our configuration file (.zshrc for me, possibly .bash_profile for you)

`echo -e 'if command -v pyenv 1>/dev/null 2>&1; then\n  eval "$(pyenv init -)"\nfi' >> ~/.zshrc`

After that command, our dotfile (.zshrc for zsh or .bash_profile for Bash) should include these lines
 ```shell script
   if command -v pyenv 1>/dev/null 2>&1; then
      eval "$(pyenv init -)"
   fi
 ```
##Install google-cloud-bigquery
`pip install google-cloud-bigquery`
##Install google-cloud-storage
`pip install google-cloud-storage`

## Project.rb command to create csv files

`cd workbench/api`

`./project.rb make-prep-ppi-csv-files --project all-of-us-workbench-test --dataset DummySR --date 2021-04-21`