"""OAuth against the AoU Workbench API using bearer tokens.

The credentials are obtained by oauth2client, from one of:
*   Google application default credentials (expected case, from notebook servers).
*   A private key file, path specified in GOOGLE_APPLICATION_CREDENTIALS environment variable.

GoogleCredentials.create_scoped() is a noop for application-default credentials. (However, it is
used when GOOGLE_APPLICATION_CREDENTIALS is defined and they key is read from a file.) For
application default credentials, the service account's scopes are set ahead of time, like:

  SCOPES="https://www.googleapis.com/auth/userinfo.profile"
  SCOPES+=",https://www.googleapis.com/auth/userinfo.email"
  gcloud compute instances set-service-account $INSTANCE_ID --zone us-west1-b \
      --service-account $PET_SA_NAME@$PROJECT.iam.gserviceaccount.com \
      --scopes "$SCOPES"

See https://www.googleapis.com/oauth2/v3/tokeninfo?access_token= for debugging.
"""

import time

from oauth2client.client import GoogleCredentials

from .swagger_client.api_client import ApiClient


# These are sometimes ignored, see module doc.
CLIENT_OAUTH_SCOPES = (
      'https://www.googleapis.com/auth/userinfo.profile',
      'https://www.googleapis.com/auth/userinfo.email',
)


# TODO(markfickett) Get workbench host dynamically based on environment.
_WORKBENCH_API_HOST = 'https://api-dot-all-of-us-workbench-test.appspot.com/'


# We use custom cache management because cachetools.TTLCache (for example) uses a
# constant / whole-cache TTL, whereas we want a varying expiration time.
_cached_client = None
_token_expiration = 0  # Epoch seconds when the token is expired. Default to some time in the past.


def get_authenticated_swagger_client(force=False):
    """Returns a Swagger ApiClient set up to make authenticated calls to the Workbench API.

    This function caches the client until its OAuth token expires, so prefer calling this function
    frequently to get a refreshed client, rather than keeping a reference to the client locally.
    """
    global _cached_client
    global _token_expiration
    if _cached_client is None:
        _cached_client = ApiClient()
        _cached_client.configuration.host = _WORKBENCH_API_HOST
    if force or (time.time() >= _token_expiration):
        token, _token_expiration = _get_bearer_token_and_expiration()
        _cached_client.configuration.access_token = token
    return _cached_client


def _get_bearer_token_and_expiration():
    """Fetches a new token. Returns (token string, expiration time epoch seconds)."""
    # Conservatively estimate token expiration time by recording token fetch time before initiating
    # the request.
    t = int(time.time())

    # The default, unscoped credentials provide an access token.
    creds = GoogleCredentials.get_application_default()
    # Scoped credentials provide the bearer token we need. However, create_scoped is sometimes
    # ignored, see the module doc.
    scoped_creds = creds.create_scoped(CLIENT_OAUTH_SCOPES)
    token_info = scoped_creds.get_access_token()

    return token_info.access_token, t + token_info.expires_in


def clear_cache():
    global _cached_client
    global _token_expiration
    _cached_client = None
    _token_expiration = 0
