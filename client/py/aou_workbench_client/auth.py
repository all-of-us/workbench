"""OAuth against the AoU Workbench API using bearer tokens provided by gcloud."""

import json
import logging
import subprocess
import time
import urllib2

from swagger_client.api_client import ApiClient


_METADATA_URL = (
    'http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token')
_METADATA_HEADER = {'Metadata-Flavor': 'Google'}
_TOKEN_INFO_URL_T = 'https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=%s'


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
  if force or (time.time() >= _token_expiration_time):
    token, _token_expiration = _get_bearer_token_and_expiration()
    _cached_client.set_default_header(name, value)
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

  respones = urllib2.urlopen(urllib2.Request(url=_METADATA_URL, headers=_METADATA_HEADER))
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
