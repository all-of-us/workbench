# Contributing

## Opening a Pull Request

You most likely want to do your work on a feature branch based on master.
Typical naming convention is `username/feature`. Username could be 
your initials or some other similar identifier. Feature could be a short 
description of the feature or a reference to the jira ticket number

Preface the PR title with JIRA issue number and security risk (e.g. `[RW-123][risk=no]`)
to the PR title. This will allow automated deployment processes to generate appropriate 
documentation. 

Please ensure that both integration and unit tests are passing.

## Security Risk Levels

Risk levels indicate to our security review team some measure of the impact
the change might have on anything security related. Sensitive information that
relates to users or patients would be an example where we need to indicate the
level of risk involved. 

- **no**: None 
- **low**: TODO: Need a description of all risk factors
- **moderate**: TODO: Need a description of all risk factors
- **severe**: TODO: Need a description of all risk factors

## PR Approval Process

* Get approval from at least one reviewer, preferably two.   
* Ensure all CI tests pass.  

## PR Completion

* Use the "Squash and Merge" feature in github
* Mark the jira ticket as "Done"
* Delete the feature branch after all work is complete 