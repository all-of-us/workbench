"""Reads configuration settings from disk.
"""

import json

# Constants for keys in the configuration
ALL_OF_US_CONFIG_FILE = 'all_of_us_config.json'
WORKSPACE_NAMESPACE_KEY = 'WORKSPACE_NAMESPACE'
WORKSPACE_ID_KEY = 'WORKSPACE_ID'
API_HOST_KEY = 'API_HOST'
BUCKET_NAME_KEY = 'BUCKET_NAME'

class AllOfUsConfig(object):
    def __init__(self):
        with open(ALL_OF_US_CONFIG_FILE) as config_file:
            self.config_json = json.load(config_file)
            self.workspace_namespace = self.config_json[WORKSPACE_NAMESPACE_KEY]
            self.workspace_id = self.config_json[WORKSPACE_ID_KEY]
            self.api_host = self.config_json[API_HOST_KEY]
            self.bucket_name = self.config_json[BUCKET_NAME_KEY]

all_of_us_config = AllOfUsConfig()
