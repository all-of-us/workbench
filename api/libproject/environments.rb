TEST_PROJECT = "all-of-us-workbench-test"

def make_gae_vars(min_idle_instances = 0, max_instances = 10, instance_class = 'F1')
  {
    "GAE_MIN_IDLE_INSTANCES" => min_idle_instances.to_s,
    "GAE_MAX_INSTANCES" => max_instances.to_s,
    'GAE_INSTANCE_CLASS' => instance_class
  }
end

def env_with_defaults(env, config)
  {
    :env_name => env,
    :config_json => "config_#{env}.json",
    :cdr_config_json => "cdr_config_#{env}.json",
    :featured_workspaces_json => "featured_workspaces_#{env}.json",
    :gae_vars => make_gae_vars,
  }.merge(config)
end

# TODO: Make environment/project flags consistent across commands, consider
# using environment keywords as dict keys here, e.g. :test, :staging, etc.
ENVIRONMENTS = {
  "local" => env_with_defaults("local", {
    :api_endpoint_host => "localhost:8081",
    :cdr_sql_instance => "workbench",
    :source_cdr_project => "all-of-us-ehr-dev",
    :source_cdr_wgs_project => "all-of-us-workbench-test",
    :accessTiers => {
      "registered" => {
        :ingest_cdr_project => "fc-aou-vpc-ingest-test",
        :dest_cdr_project => "fc-aou-cdr-synth-test",
        :auth_domain_group_email => "all-of-us-registered-local@dev.test.firecloud.org",
      },
      "controlled" => {
        :ingest_cdr_project => "fc-aou-cdr-ingest-test-2",
        :ingest_cdr_bucket => "gs://fc-aou-test-datasets-controlled-ingest",
        :dest_cdr_project => "fc-aou-cdr-synth-test-2",
        :auth_domain_group_email => "all-of-us-controlled-local@dev.test.firecloud.org"
      }
    }
  }),
  "all-of-us-workbench-test" => env_with_defaults("test", {
    :api_endpoint_host => "api-dot-#{TEST_PROJECT}.appspot.com",
    :cdr_sql_instance => "#{TEST_PROJECT}:us-central1:workbenchmaindb",
    :gae_vars => make_gae_vars(0, 10, 'F4_1G'),
    :publisher_account => "circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com",
    :source_cdr_project => "all-of-us-ehr-dev",
    :source_cdr_wgs_project => "all-of-us-workbench-test",
    :accessTiers => {
      "registered" => {
        :ingest_cdr_project => "fc-aou-vpc-ingest-test",
        :dest_cdr_project => "fc-aou-cdr-synth-test",
        :auth_domain_group_email => "GROUP_all-of-us-registered-test@dev.test.firecloud.org",
      },
      "controlled" => {
        :ingest_cdr_project => "fc-aou-cdr-ingest-test-2",
        :ingest_cdr_bucket => "gs://fc-aou-test-datasets-controlled-ingest",
        :dest_cdr_project => "fc-aou-cdr-synth-test-2",
        :dest_cdr_bucket => "gs://fc-aou-test-datasets-controlled",
        :auth_domain_group_email => "all-of-us-test-prototype-3@dev.test.firecloud.org",
      }
    }
  }),
  "all-of-us-rw-staging" => env_with_defaults("staging", {
    :api_endpoint_host => "api.staging.fake-research-aou.org",
    :cdr_sql_instance => "#{TEST_PROJECT}:us-central1:workbenchmaindb",
    :gae_vars => make_gae_vars(0, 10, 'F4'),
    :source_cdr_project => "all-of-us-ehr-dev",
    :source_cdr_wgs_project => "all-of-us-workbench-test",
    :publisher_account => "circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :ingest_cdr_project => "fc-aou-vpc-ingest-staging",
        :dest_cdr_project => "fc-aou-cdr-synth-staging",
        :auth_domain_group_email => "GROUP_all-of-us-registered-staging@firecloud.org",
      },
      "controlled" => {
        :ingest_cdr_project => "fc-aou-vpc-ingest-staging-ct",
        :dest_cdr_project => "fc-aou-cdr-staging-ct",
        :auth_domain_group_email => "all-of-us-controlled-staging@firecloud.org",
      }
    }
  }),
  "all-of-us-rw-stable" => env_with_defaults("stable", {
    :api_endpoint_host => "api.stable.fake-research-aou.org",
    :cdr_sql_instance => "#{TEST_PROJECT}:us-central1:workbenchmaindb",
    :gae_vars => make_gae_vars(0, 10, 'F4'),
    :source_cdr_project => "all-of-us-ehr-dev",
    :source_cdr_wgs_project => "all-of-us-workbench-test",
    :publisher_account => "deploy@all-of-us-rw-stable.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :ingest_cdr_project => "fc-aou-vpc-ingest-stable",
        :dest_cdr_project => "fc-aou-cdr-synth-stable",
        :auth_domain_group_email => "GROUP_all-of-us-registered-stable@firecloud.org",
      },
      "controlled" => {
        :ingest_cdr_project => "fc-aou-vpc-ingest-stable-ct",
        :dest_cdr_project => "fc-aou-cdr-stable-ct",
        :auth_domain_group_email => "all-of-us-controlled-stable@firecloud.org",
      }
    }
  }),
  "all-of-us-rw-preprod" => env_with_defaults("preprod", {
    :api_endpoint_host => "api.preprod-workbench.researchallofus.org",
    :gae_vars => make_gae_vars(0, 10, 'F4'),
    :cdr_sql_instance => "all-of-us-rw-preprod:us-central1:workbenchmaindb",
    :source_cdr_project => "aou-res-curation-output-prod",
    :source_cdr_wgs_project => "aou-genomics-curation-prod",
    :source_other_datasets_project => "aou-res-curation-prod",
    :publisher_account => "deploy@all-of-us-rw-preprod.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :ingest_cdr_project => "fc-aou-vpc-ingest-preprod",
        :dest_cdr_project => "fc-aou-cdr-preprod",
        :auth_domain_group_email => "all-of-us-registered-preprod@firecloud.org",
        :ingest_other_datasets_project => "fc-aou-vpc-ingest-preprod-prev",
        :dest_other_datasets_project => "fc-aou-cdr-preprod-preview",
        :auth_domain_other_datasets_email => "aou-prev-data@firecloud.org",
      },
      "controlled" => {
        :ingest_cdr_project => "fc-aou-vpc-ingest-preprod-ct",
        :dest_cdr_project => "fc-aou-cdr-preprod-ct",
        :ingest_cdr_bucket => "gs://fc-aou-preprod-datasets-controlled-ingest",
        :dest_cdr_bucket => "gs://fc-aou-preprod-datasets-controlled",
        :auth_domain_group_email => "all-of-us-controlled-preprod@firecloud.org",
      }
    }
  }),
  "all-of-us-rw-prod" => env_with_defaults("prod", {
    :api_endpoint_host => "api.workbench.researchallofus.org",
    :cdr_sql_instance => "all-of-us-rw-prod:us-central1:workbenchmaindb",
    :gae_vars => make_gae_vars(8, 64, 'F4_1G'),
    :source_cdr_project => "aou-res-curation-output-prod",
    :source_cdr_wgs_project => "aou-genomics-curation-prod",
    :publisher_account => "deploy@all-of-us-rw-prod.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :ingest_cdr_project => "fc-aou-vpc-ingest-prod",
        :dest_cdr_project => "fc-aou-cdr-prod",
        :auth_domain_group_email => "all-of-us-registered-prod@firecloud.org",
      },
      "controlled" => {
        :ingest_cdr_project => "fc-aou-vpc-ingest-prod-ct",
        :dest_cdr_project => "fc-aou-cdr-prod-ct",
        :ingest_cdr_bucket => "gs://fc-aou-datasets-controlled-ingest",
        :dest_cdr_bucket => "gs://fc-aou-datasets-controlled",
        :auth_domain_group_email => "all-of-us-controlled-prod@firecloud.org",
      }
    }
  })
}

def must_get_env_value(env, key)
  unless ENVIRONMENTS.fetch(env, {}).has_key?(key)
    raise ArgumentError.new("env '#{env}' lacks key #{key}")
  end
  return ENVIRONMENTS[env][key]
end

def get_config(project)
  config_json = must_get_env_value(project, :config_json)
  path = File.join(File.dirname(__FILE__), "../config/#{config_json}")
  return JSON.parse(File.read(path))
end

def get_cdr_config(project)
  cdr_config_json = must_get_env_value(project, :cdr_config_json)
  path = File.join(File.dirname(__FILE__), "../config/#{cdr_config_json}")
  return JSON.parse(File.read(path))
end
