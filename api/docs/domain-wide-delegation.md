# Domain-wide delegation

The Research Workbench (RW) backend uses `domain-wide delegation of authority` in a few places to 
carry out administrative and maintenance tasks.

## Background

Domain-wide delegation allows the RW backend to impersonate users when making calls to
OAuth-authenticated APIs. This works because RW is in full control of the GSuite account that provides
user identities for @researchallofus.org (and @fake-research-aou.org) accounts.

For more info, see https://developers.google.com/identity/protocols/OAuth2ServiceAccount.

## GSuite / Directory API

The RW backend needs the ability to create new GSuite users in response to an API request. We also need
to manipulate GSuite user data (e.g. update schema values, backfill contact email addresses, fetch
two-factor auth status) in the context of cron jobs or command-line tools. 

To handle this, we use the following setup:

- Create a `directory-admin@domain.org` GSuite user, with Super-admin privileges.
- Create a `directory-admin@project.gserviceaccount.com` Service Account, and grant domain-wide
  authority for this account to make API calls on behalf of any user within `domain.org`, scoped
  to the GSuite Directory API.
- The AoU backend loads credentials for the `directory-admin` Service Account, generates an
  OAuth access token impersonating the `directory-admin` GSuite user, and uses that token to
  make API calls against the Directory Service API.
  
For more details, see:
- https://broad.io/aou-new-environment - details on how to create service accounts & domain-wide
  delegation in a new AoU environment.
- DirectoryServiceImpl.java#createCredentialWithImpersonation - the method where we generate the impersonated
  access token using the `directory-admin` Service Account credentials.

## FireCloud APIs

The AoU backend needs the ability to fetch FireCloud user data in the context of cron jobs and maintenance
scripts, where we don't have a direct OAuth token for the user whose data we are accessing.

To handle this, we use the following setup:

- Create a firecloud-admin@project.gserviceaccount.com` Service Account, and grant domain-wide
  authority for this account to make API calls scoped to the FireCloud API's OAuth scopes (openid, profile, email, 
  https://googleapis.com/auth/cloud-billing).
- Ask FireCloud to whitelist the Service Account client ID for API access.
- The AoU backend loads credentials for the `firecloud-admin` Service Account, generates an OAuth access token
  impersonating any Google Account on the AoU domain, and uses that token to make API calls to FireCloud.
