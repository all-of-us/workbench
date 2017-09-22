"""OAuth against the AoU Workbench API using bearer tokens provided by gcloud."""

import json
import logging
import subprocess
import time
import urllib2

from swagger_client.api_client import ApiClient


_METADATA_URL = (
        'http://metadata.google.internal/computeMetadata/v1/'
        'instance/service-accounts/default/token')
_METADATA_HEADER = {'Metadata-Flavor': 'Google'}
_TOKEN_INFO_URL_T = 'https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=%s'
# TODO(markfickett) Get workbench host dynamically based on environment.
_WORKBENCH_API_HOST = 'https://api-dot-all-of-us-workbench-test.appspot.com/'


# Note that we use custom cache management because cachetools.TTLCache (for example) uses a
# constant / whole-cache TTL, whereas we want a varying expiration time.
_cached_client = None
_token_expiration = 0  # Epoch seconds when the token is expired. Default to some time in the past.


def get_authenticated_swagger_client(force=False):
    """Caches returned value and lazily re-authenticates as tokens expire."""
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
    try:
        return _query_metadata_api()
    except urllib2.URLError as e:
        logging.warning('Metadata API query failed: %s', e)
        return _run_print_access_token()


def _query_metadata_api():
    # Conservatively estimate token expiration time by recording token fetch time before initiating
    # the request.
    t = int(time.time())

    response = urllib2.urlopen(urllib2.Request(url=_METADATA_URL, headers=_METADATA_HEADER))
    response_json = json.loads(response.read())
    return response_json['access_token'], t + response_json['expires_in']


def _run_print_access_token():
    logging.warning('Falling back to print-access-token (should be used for debugging only).')
    token = subprocess.check_output(['gcloud', 'auth', 'print-access-token']).strip()

    # Get details about the token, including expiration time.
    t = time.time()
    response = urllib2.urlopen(_TOKEN_INFO_URL_T % token)
    token_info_json = json.loads(response.read())
    return token, t + token_info_json['expires_in']


def clear_cache():
    global _cached_client
    global _token_expiration
    _cached_client = None
    _token_expiration = 0


# Self-test / simple example.
if __name__ == '__main__':
    # Print tokens from each source.
    print 'Metadata API'
    try:
        print _query_metadata_api()
    except urllib2.URLError as e:
        print 'unavailable:', e
    print 'print-access-token'
    print _run_print_access_token()

    # Make an example API call.
    from swagger_client.apis.workspaces_api import WorkspacesApi
    client = WorkspacesApi(api_client=get_authenticated_swagger_client())
    workspace_list = client.get_workspaces()
    print 'Workspaces:'
    for ws in workspace_list.items:
        print '%s/%s\t%s\t%s' % (ws.namespace, ws.id, ws.name, ws.description)
