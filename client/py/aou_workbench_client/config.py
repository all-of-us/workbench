"""Reads configuration settings from disk.
"""

import json

# Constants for keys in the configuration
ALL_OF_US_CONFIG_FILE = '~/config/all_of_us_config.json'
WORKSPACE_NAMESPACE_KEY = 'WORKSPACE_NAMESPACE'
WORKSPACE_ID_KEY = 'WORKSPACE_ID'
API_HOST_KEY = 'API_HOST'
BUCKET_NAME_KEY = 'BUCKET_NAME'
CDR_VERSION_CLOUD_PROJECT = 'CDR_VERSION_CLOUD_PROJECT'
CDR_VERSION_BIGQUERY_DATASET = 'CDR_VERSION_BIGQUERY_DATASET'

class AllOfUsConfig(object):
    def __init__(self):
        with open(ALL_OF_US_CONFIG_FILE) as config_file:
            self.config_json = json.load(config_file)
            self.workspace_namespace = self.config_json[WORKSPACE_NAMESPACE_KEY]
            self.workspace_id = self.config_json[WORKSPACE_ID_KEY]
            self.api_host = self.config_json[API_HOST_KEY]
            self.bucket_name = self.config_json[BUCKET_NAME_KEY]
            self.cdr_version_cloud_project = self.config_json[CDR_VERSION_CLOUD_PROJECT]
            self.cdr_version_bigquery_dataset = self.config_json[CDR_VERSION_BIGQUERY_DATASET]

all_of_us_config = AllOfUsConfig()

def cdr_table(table_name):
    return '`%s.%s.%s`' % (all_of_us_config.cdr_version_cloud_project,
                           all_of_us_config.cdr_version_bigquery_dataset,
                           table_name)
