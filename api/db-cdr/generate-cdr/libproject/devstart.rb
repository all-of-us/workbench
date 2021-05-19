require_relative "../../../../aou-utils/serviceaccounts"
require_relative "../../../../aou-utils/utils/common"
require_relative "../../../libproject/wboptionsparser"
require "json"
require "set"
require "tempfile"

ENVIRONMENTS = {
  "all-of-us-workbench-test" => {
    :config_json => "config_test.json",
    :publisher_account => "circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :source_cdr_project => "all-of-us-ehr-dev",
        :source_cdr_wgs_project => "all-of-us-workbench-test",
        :ingest_cdr_project => "fc-aou-vpc-ingest-test",
        :dest_cdr_project => "fc-aou-cdr-synth-test",
        :auth_domain_group_email => "GROUP_all-of-us-registered-test@dev.test.firecloud.org",
      },
      "controlled" => {
        :source_cdr_project => "all-of-us-ehr-dev",
        :ingest_cdr_project => "fc-aou-cdr-ingest-test-2",
        :dest_cdr_project => "fc-aou-cdr-synth-test-2",
        :auth_domain_group_email => "all-of-us-test-prototype-3@dev.test.firecloud.org",
      }
    }
  },
  "all-of-us-rw-staging" => {
    :config_json => "config_staging.json",
    :publisher_account => "circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :source_cdr_project => "all-of-us-ehr-dev",
        :ingest_cdr_project => "fc-aou-vpc-ingest-staging",
        :dest_cdr_project => "fc-aou-cdr-synth-staging",
        :auth_domain_group_email => "GROUP_all-of-us-registered-staging@firecloud.org",
      },
      "controlled" => {
        :source_cdr_project => "all-of-us-ehr-dev",
        :ingest_cdr_project => "fc-aou-vpc-ingest-staging-ct",
        :dest_cdr_project => "fc-aou-cdr-staging-ct",
        :auth_domain_group_email => "all-of-us-controlled-staging@firecloud.org",
      }
    }
  },
  "all-of-us-rw-perf" => {
    :config_json => "config_perf.json",
    :publisher_account => "circle-deploy-account@all-of-us-workbench-test.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :source_cdr_project => "all-of-us-ehr-dev",
        :ingest_cdr_project => "fc-aou-vpc-ingest-perf",
        :dest_cdr_project => "fc-aou-cdr-perf",
        :auth_domain_group_email => "all-of-us-registered-perf@perf.test.firecloud.org",
      },
      "controlled" => {
        :source_cdr_project => "all-of-us-ehr-dev",
        :ingest_cdr_project => "fc-aou-vpc-ingest-perf-ct",
        :dest_cdr_project => "fc-aou-cdr-perf-ct",
        :auth_domain_group_email => "all-of-us-controlled-perf@perf.test.firecloud.org",
      }
    }
  },
  "all-of-us-rw-stable" => {
    :config_json => "config_stable.json",
    :publisher_account => "deploy@all-of-us-rw-stable.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :source_cdr_project => "all-of-us-ehr-dev",
        :source_cdr_wgs_project => "aou-genomics-curation-stable",
        :ingest_cdr_project => "fc-aou-vpc-ingest-stable",
        :dest_cdr_project => "fc-aou-cdr-synth-stable",
        :auth_domain_group_email => "GROUP_all-of-us-registered-stable@firecloud.org",
      },
    }
  },
  "all-of-us-rw-preprod" => {
    :config_json => "config_preprod.json",
    :publisher_account => "deploy@all-of-us-rw-preprod.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :source_cdr_project => "aou-res-curation-output-prod",
        :ingest_cdr_project => "fc-aou-vpc-ingest-preprod",
        :dest_cdr_project => "fc-aou-cdr-preprod",
        :auth_domain_group_email => "all-of-us-registered-preprod@firecloud.org",
      },
      "controlled" => {
        :source_cdr_project => "aou-res-curation-output-prod",
        :source_cdr_wgs_project => "aou-genomics-curation-prod",
        :ingest_cdr_project => "fc-aou-vpc-ingest-preprod-ct",
        :dest_cdr_project => "fc-aou-cdr-preprod-ct",
        :auth_domain_group_email => "all-of-us-controlled-preprod@firecloud.org",
      }
    }
  },
  "all-of-us-rw-prod" => {
    :config_json => "config_prod.json",
    :publisher_account => "deploy@all-of-us-rw-prod.iam.gserviceaccount.com",
    :accessTiers => {
      "registered" => {
        :source_cdr_project => "aou-res-curation-output-prod",
        :ingest_cdr_project => "fc-aou-vpc-ingest-prod",
        :dest_cdr_project => "fc-aou-cdr-prod",
        :auth_domain_group_email => "all-of-us-registered-prod@firecloud.org",
      },
    }
  }
}

def prefixes_to_grep_filter(prefixes)
  return "^\\(#{prefixes.join("\\|")}\\)"
end

WGS_TABLE_PREFIXES = [
  "alt_allele",
  "filter_set_",
  "pet_",
  "sample_info",
  "vet_"
]
WGS_TABLE_FILTER = prefixes_to_grep_filter(WGS_TABLE_PREFIXES)


# TODO: RW-6426. Refactor ENVIRONMENTS object in this file and api/libproject/devstart.rb
# Share ENVIRONMENT related code and config parsing
def must_get_env_value(env, key)
  unless ENVIRONMENTS.fetch(env, {}).has_key?(key)
    raise ArgumentError.new("env '#{env}' lacks key #{key}")
  end
  return ENVIRONMENTS[env][key]
end

def get_config_file(project)
  config_json = must_get_env_value(project, :config_json)
  return "../../config/#{config_json}"
end

def get_config(project)
  return JSON.parse(File.read(get_config_file(project)))
end

def must_get_wgs_proxy_group(project)
  v = get_config(project).fetch("wgsCohortExtraction", {}).fetch("serviceAccountTerraProxyGroup")
  raise ArgumentError.new("no WGS proxy group configured for env #{project}") unless v
  return v
end

def ensure_docker(cmd_name, args=nil)
  args = (args or [])
  unless Workbench.in_docker?
    exec(*(%W{docker-compose run --rm cdr-scripts ./generate-cdr/project.rb #{cmd_name}} + args))
  end
end

def service_account_context_for_bq(project, account)
  common = Common.new
  # TODO(RW-3208): Investigate using a temporary / impersonated SA credential instead of a key.
  key_file = Tempfile.new(["#{account}-key", ".json"], "/tmp")
  ServiceAccountContext.new(
    project, account, key_file.path).run do
    # TODO(RW-3768): This currently leaves the user session with an activated service
    # account user. Ideally the activation would be hermetic within the docker
    # session, or else we would revert the active account after running.
    common.run_inline %W{gcloud auth activate-service-account -q --key-file #{key_file.path}}
    yield
  end
end

# By default, skip empty lines only.
def bq_ingest(tier, source_project, dataset_name, table_match_filter="", table_skip_filter="^$")
  common = Common.new
  source_fq_dataset = "#{source_project}:#{dataset_name}"
  ingest_fq_dataset = "#{tier.fetch(:ingest_cdr_project)}:#{dataset_name}"
  dest_fq_dataset = "#{tier.fetch(:dest_cdr_project)}:#{dataset_name}"
  common.status "Copying from '#{source_fq_dataset}' -> '#{ingest_fq_dataset}' -> '#{dest_fq_dataset}'"

  # If you receive an error from "bq" like "Invalid JWT Signature", you may
  # need to delete cached BigQuery creds on your local machine. Try running
  # bq init --delete_credentials as recommended in the output.
  # TODO(RW-3768): Find a better solution for Google credentials in docker.

  # Copy through an intermediate project and delete after (include TTL in case later steps fail).
  # See https://docs.google.com/document/d/1EHw5nisXspJjA9yeZput3W4-vSIcuLBU5dPizTnk1i0/edit
  common.run_inline %W{bq mk -f --default_table_expiration 7200 --dataset #{ingest_fq_dataset}}
  common.run_inline %W{./copy-bq-dataset.sh
      #{source_fq_dataset} #{ingest_fq_dataset} #{source_project}
      #{table_match_filter} #{table_skip_filter}}

  common.run_inline %W{bq mk -f --dataset #{dest_fq_dataset}}
  common.run_inline %W{./copy-bq-dataset.sh
      #{ingest_fq_dataset} #{dest_fq_dataset} #{tier.fetch(:ingest_cdr_project)}
      #{table_match_filter} #{table_skip_filter}}

  # Delete the intermediate dataset.
  common.run_inline %W{bq rm -r -f --dataset #{ingest_fq_dataset}}
end

def bq_update_acl(fq_dataset)
  common = Common.new

  config_file = Tempfile.new("bq-acls.json")
  begin
    json = JSON.parse(
      common.capture_stdout %{bq show --format=prettyjson #{fq_dataset}})
    existing_groups = Set[]
    existing_users = Set[]
    for entry in json["access"]
      if entry.key?("groupByEmail")
        existing_groups.add(entry["groupByEmail"])
      end
      if entry.key?("userByEmail")
        existing_users.add(entry["userByEmail"])
      end
    end

    json = yield(json, existing_groups, existing_users)
    File.open(config_file.path, "w") do |f|
      f.write(JSON.pretty_generate(json))
    end
    common.run_inline %W{bq update --source #{config_file.path} #{fq_dataset}}
  ensure
    config_file.unlink
  end
end

def publish_cdr(cmd_name, args)
  ensure_docker cmd_name, args

  op = WbOptionsParser.new(cmd_name, args)

  op.add_option(
    "--bq-dataset [dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BigQuery dataset name for the CDR version (project not included), e.g. " +
    "'2019Q4R3'. Required."
  )
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "The Google Cloud project associated with this workbench environment, " +
    "e.g. all-of-us-rw-staging. Required."
  )
  op.opts.tier = "registered"
  op.add_option(
     "--tier [tier]",
     ->(opts, v) { opts.tier = v},
     "The access tier associated with this CDR, " +
     "e.g. registered. Default is registered."
   )
  op.add_option(
    "--table-prefixes [prefix1,prefix2,...]",
    ->(opts, v) { opts.table_prefixes = v},
    "Optional comma-delimited list of table prefixes to filter the publish " +
    "by, e.g. cb_,ds_. This should only be used in special situations e.g. " +
    "when the auxilliary cb_ or ds_ tables need to be updated, or if there " +
    "was an issue with the publish. In general, CDRs should be treated as " +
    "immutable after the initial publish."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_dataset and opts.project and opts.tier }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported project: #{opts.project}") unless ENVIRONMENTS.key? opts.project }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported tier: #{opts.tier}") unless ENVIRONMENTS[opts.project][:accessTiers].key? opts.tier }
  op.parse.validate

  # This is a grep filter. It matches all tables, by default.
  table_match_filter = ""
  if op.opts.table_prefixes
    prefixes = op.opts.table_prefixes.split(",")
    table_match_filter = "^\\(#{prefixes.join("\\|")}\\)"
  end

  # This is a grep -v filter. It skips cohort builder build-only tables, which
  # follow the convention of having the prefix prep_. See RW-4863.
  table_skip_filter = "^prep_"

  common = Common.new
  env = ENVIRONMENTS[op.opts.project]
  tier = env.fetch(:accessTiers)[op.opts.tier]
  source_cdr_project = tier.fetch(:source_cdr_project)
  dest_fq_dataset = "#{tier.fetch(:dest_cdr_project)}:#{op.opts.bq_dataset}"

  service_account_context_for_bq(op.opts.project, env.fetch(:publisher_account)) do
    bq_ingest(tier, source_cdr_project, op.opts.bq_dataset, table_match_filter, table_skip_filter)

    bq_update_acl(dest_fq_dataset) do |acl_json, existing_groups, existing_users|
      auth_domain_group_email = tier.fetch(:auth_domain_group_email)
      if existing_groups.include?(auth_domain_group_email)
        common.status "#{auth_domain_group_email} already in ACL, skipping..."
      else
        common.status "Adding #{auth_domain_group_email} as a READER..."
        acl_json["access"].push({
          "groupByEmail" => auth_domain_group_email,
          "role" => "READER"
        })
      end

      app_sa = "#{op.opts.project}@appspot.gserviceaccount.com"
      if existing_users.include?(app_sa)
        common.status "#{app_sa} already in ACL, skipping..."
      else
        common.status "Adding #{app_sa} as a READER..."
        acl_json["access"].push({ "userByEmail" => app_sa, "role" => "READER"})
      end

      acl_json
    end
  end
end

Common.register_command({
  :invocation => "publish-cdr",
  :description => "Publishes a CDR dataset by copying it into a Firecloud CDR project and making it readable by registered users in the corresponding environment",
  :fn => ->(*args) { publish_cdr("publish-cdr", args) }
})

def publish_cdr_wgs(cmd_name, args)
  ensure_docker cmd_name, args

  op = WbOptionsParser.new(cmd_name, args)

  op.add_option(
    "--bq-dataset [dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BigQuery dataset name for the CDR version (project not included), e.g. " +
    "'2019Q4R3'. Required."
  )
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "The Google Cloud project associated with this workbench environment, " +
    "e.g. all-of-us-rw-staging. Required."
  )
  op.opts.tier = "controlled"
  op.add_option(
     "--tier [tier]",
     ->(opts, v) { opts.tier = v},
     "The access tier associated with this CDR, e.g. controlled." +
     "Default is controlled (WGS only exists in controlled tier, for the foreseeable future)."
   )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_dataset and opts.project and opts.tier }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported project: #{opts.project}") unless ENVIRONMENTS.key? opts.project }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported tier: #{opts.tier}") unless ENVIRONMENTS[opts.project][:accessTiers].key? opts.tier }
  op.parse.validate

  common = Common.new
  env = ENVIRONMENTS[op.opts.project]
  tier = env.fetch(:accessTiers)[op.opts.tier]
  dest_fq_dataset = "#{tier.fetch(:dest_cdr_project)}:#{op.opts.bq_dataset}"

  source_project = tier.fetch(:source_cdr_wgs_project)
  unless source_project
    raise ArgumentError.new("missing WGS source project value for env #{op.opts.project}")
  end
  extraction_proxy_group = must_get_wgs_proxy_group(op.opts.project)

  service_account_context_for_bq(op.opts.project, env.fetch(:publisher_account)) do
    bq_ingest(tier, source_project, op.opts.bq_dataset, WGS_TABLE_FILTER)

    bq_update_acl(dest_fq_dataset) do |acl_json, existing_groups, _existing_users|
      if existing_groups.include?(extraction_proxy_group)
        common.status "#{extraction_proxy_group} already in ACL, skipping..."
      else
        common.status "Adding #{extraction_proxy_group} as a READER..."
        acl_json["access"].push({"groupByEmail" => extraction_proxy_group, "role" => "READER"})
      end
      acl_json
    end
  end
end

Common.register_command({
  :invocation => "publish-cdr-wgs",
  :description => "Publishes a WGS CDR dataset by copying it into a Firecloud CDR project and making it readable by AoU service accounts",
  :fn => ->(*args) { publish_cdr_wgs("publish-cdr-wgs", args) }
})

def create_wgs_extraction_datasets(cmd_name, args)
  ensure_docker cmd_name, args

  op = WbOptionsParser.new(cmd_name, args)

  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "The Google Cloud project associated with this workbench environment, " +
      "e.g. all-of-us-rw-staging. Required."
  )
  op.opts.tier = "controlled"
  op.add_option(
    "--tier [tier]",
    ->(opts, v) { opts.tier = v},
    "The access tier associated with this CDR, " +
      "e.g. registered. Default is controlled."
  )
  op.opts.ttl = 60 * 60 * 24 * 7
  op.add_option(
    "--ttl [ttl]",
    ->(opts, v) { opts.ttl = v},
    "Add default ttl to dataset tables. Given in seconds.")
  op.add_validator ->(opts) { raise ArgumentError unless opts.project and opts.tier and opts.ttl }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported project: #{opts.project}") unless ENVIRONMENTS.key? opts.project }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported tier: #{opts.tier}") unless ENVIRONMENTS[opts.project][:accessTiers].key? opts.tier }
  op.parse.validate

  common = Common.new
  env = ENVIRONMENTS[op.opts.project]
  proxy_group = must_get_wgs_proxy_group(op.opts.project)
  cdr_project = env.fetch(:accessTiers)[op.opts.tier].fetch(:dest_cdr_project)

  extract_config = get_config(op.opts.project).fetch("wgsCohortExtraction", {})
  fq_datasets = [
    "extractionCohortsDataset",
    "extractionDestinationDataset",
    "extractionTempTablesDataset"
  ].map do |key|
    fq_ds = extract_config.fetch(key)
    raise ArgumentError.new("missing config value for #{key} in project #{op.opts.project}") unless fq_ds
    raise ArgumentError.new("config value (#{fq_ds}) doesn't match expected CDR project for tier (#{cdr_project})") unless fq_ds.start_with? "#{cdr_project}."
    fq_ds.sub("\.", ":")
  end

  service_account_context_for_bq(op.opts.project, env.fetch(:publisher_account)) do
    for fq_dataset in fq_datasets do
      common.run_inline %W{bq mk -f --default_table_expiration #{op.opts.ttl}  --dataset #{fq_dataset}}

      bq_update_acl(fq_dataset) do |acl_json, existing_groups, _existing_users|
        if existing_groups.include?(proxy_group)
          common.status "#{proxy_group} already in ACL, skipping..."
        else
          common.status "Adding #{proxy_group} as a WRITER..."
          acl_json["access"].push({"groupByEmail" => proxy_group, "role" => "WRITER"})
        end

        acl_json
      end
    end
  end
end

Common.register_command({
  :invocation => "create-wgs-extraction-datasets",
  :description => "Create datasets with TTL tables for WGS cohort extraction",
  :fn => ->(*args) { create_wgs_extraction_datasets("create-wgs-extraction-datasets", args) }
})
