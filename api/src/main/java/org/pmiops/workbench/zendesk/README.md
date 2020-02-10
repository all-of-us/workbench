# Zendesk Integration

The current Zendesk integration is very light-weight, one-step up from email.
We use the [Zendesk REST API](https://developer.zendesk.com/rest_api/docs/support)
via a [Java client library](https://github.com/cloudbees/zendesk-java-client).

We currently only send unauthenticated calls to Zendesk, which is sufficient to
create new Zendesk requests. If authentication is desired in the future, the
following paths have been explored:

1. Using an admin-generated API token. This can be created/revoked via the
   Zendesk admin console. https://developer.zendesk.com/rest_api/docs/support/requests#api-token
   We'd likely want to store that key in GCS as a secret; note that this grants
   fairly wide unscoped permissions (admin-level).
2. Using oauth: as of 1/31/20 this appears to be a very heavy integration which
   requires a web flow for the end user in order to leverage. This path does not
   seem promising for the purpose of backend Zendesk API requests.

## Workspace Review Requests

When a workspace review is requested, a Zendesk ticket is automatically filed.
This ticket template must be kept up-to-date with the latest workspace research
purpose details as the model evolves, though a raw JSON view of the workspace is
also included as a fallback.

As of 1/31/20, this is a one-way process. Any outreach back to the owner or
follow-up to approve/reject the workspace would need to be managed within the
Zendesk ticket.
