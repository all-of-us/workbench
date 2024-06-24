require_relative "genomic_manifests"
require_relative "../../../../aou-utils/serviceaccounts"
require_relative "../../../../aou-utils/utils/common"
require_relative "../../../libproject/affirm"
require_relative "../../../libproject/gcloudcontext"
require_relative "../../../libproject/environments"
require_relative "../../../libproject/wboptionsparser"
require "csv"
require "json"
require "set"
require "tempfile"

def prefixes_to_grep_filter(prefixes)
  return "^\\(#{prefixes.join("\\|")}\\)"
end

SHARED_TEST_CDR_PROJECTS = [
  TEST_PROJECT,
  "local"
]

WGS_TABLE_PREFIXES = [
  "alt_allele",
  "filter_set_",
  "pet_", # v1 schema only
  "ref_ranges_", #v2 schema only
  "sample_info",
  "vet_"
]
WGS_TABLE_FILTER = prefixes_to_grep_filter(WGS_TABLE_PREFIXES)

def must_get_wgs_proxy_group(project)
  v = get_config(project).fetch("wgsCohortExtraction", {}).fetch("serviceAccountTerraProxyGroup")
  raise ArgumentError.new("no WGS proxy group configured for env #{project}") unless v
  return v
end

def service_account_context_for_bq(project, account)
  common = Common.new

  original_account = get_active_gcloud_account()
  # TODO(RW-3208): Investigate using a temporary / impersonated SA credential instead of a key.
  key_file = Tempfile.new(["#{account}-key", ".json"], "/tmp")
  ServiceAccountContext.new(
    project, account, key_file.path).run do
    common.run_inline %W{gcloud auth activate-service-account -q --key-file #{key_file.path}}
    yield
  ensure
    common.status "restoring original gcloud account: #{original_account}"
    common.run_inline %{gcloud auth login #{original_account}}
  end
end

# By default, skip empty lines only.
def bq_ingest(tier, tier_name, source_project, source_dataset_name, dest_dataset_name, table_match_filter = "", table_skip_filter = "^$")
  common = Common.new
  source_fq_dataset = "#{source_project}:#{source_dataset_name}"
  ingest_fq_dataset = "#{tier.fetch(:ingest_cdr_project)}:#{dest_dataset_name}"
  dest_fq_dataset = "#{tier.fetch(:dest_cdr_project)}:#{dest_dataset_name}"
  common.status "Copying from '#{source_fq_dataset}' -> '#{ingest_fq_dataset}' -> '#{dest_fq_dataset}'"

  # If you receive an error from "bq" like "Invalid JWT Signature", you may
  # need to delete cached BigQuery creds on your local machine. Try running
  # bq init --delete_credentials as recommended in the output.

  # validate the CDR's tier label against the user-supplied tier, as a safety check

  cdr_metadata = JSON.parse(common.capture_stdout %{bq show --format=json #{source_fq_dataset}})
  dataset_tier = cdr_metadata.dig('labels', 'data_tier')

  if dataset_tier
    unless dataset_tier == tier_name
      raise ArgumentError.new("The dataset's access tier '#{dataset_tier}' differs from the requested '#{tier_name}'. Aborting.")
    end
  else
    common.warning "no data_tier label found for #{source_fq_dataset}"
  end

  # Copy through an intermediate project and delete after (include TTL in case later steps fail).
  # See https://docs.google.com/document/d/1EHw5nisXspJjA9yeZput3W4-vSIcuLBU5dPizTnk1i0/edit
  common.run_inline %W{bq mk -f --default_table_expiration 86400 --dataset #{ingest_fq_dataset}}
  ingest_args = %W{./copy-bq-dataset.sh
      #{source_fq_dataset} #{ingest_fq_dataset} #{source_project}
  #{table_match_filter} #{table_skip_filter}}
  ingest_dry_stdout = common.capture_stdout(ingest_args + %W{--dry-run}, nil)
  common.run_inline ingest_args

  common.run_inline %W{bq mk -f --dataset #{dest_fq_dataset}}
  publish_args = %W{./copy-bq-dataset.sh
      #{ingest_fq_dataset} #{dest_fq_dataset} #{tier.fetch(:ingest_cdr_project)}
  #{table_match_filter} #{table_skip_filter}}
  publish_dry_stdout = common.capture_stdout(publish_args + %W{--dry-run}, nil)

  unless ingest_dry_stdout.lines.length == publish_dry_stdout.lines.length
    raise RuntimeError.new(
      "mismatched line count between ingest and publish:\n" +
        "ingest:\n#{ingest_dry_stdout}\n" +
        "publish:\n#{publish_dry_stdout}")
  end

  common.run_inline publish_args

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
  op = WbOptionsParser.new(cmd_name, args)

  op.add_option(
    "--bq-dataset [dataset]",
    ->(opts, v) { opts.bq_dataset = v },
    "BigQuery dataset name for the CDR version (project not included), e.g. " +
      "'2019Q4R3'. Required."
  )
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v },
    "The Google Cloud project associated with this workbench environment, " +
      "e.g. all-of-us-rw-staging. Required."
  )
  op.opts.tier = "registered"
  op.add_option(
    "--tier [tier]",
    ->(opts, v) { opts.tier = v },
    "The access tier associated with this CDR, " +
      "e.g. registered. Default is registered."
  )
  op.add_option(
    "--table-prefixes [prefix1,prefix2,...]",
    ->(opts, v) { opts.table_prefixes = v },
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
  source_cdr_project = env.fetch(:source_cdr_project)
  dest_fq_dataset = "#{tier.fetch(:dest_cdr_project)}:#{op.opts.bq_dataset}"

  service_account_context_for_bq(op.opts.project, env.fetch(:publisher_account)) do
    bq_ingest(tier, op.opts.tier, source_cdr_project, op.opts.bq_dataset, op.opts.bq_dataset, table_match_filter, table_skip_filter)

    bq_update_acl(dest_fq_dataset) do |acl_json, existing_groups, existing_users|
      auth_domain_group_emails = [tier.fetch(:auth_domain_group_email)]
      if SHARED_TEST_CDR_PROJECTS.include?(op.opts.project)
        # While test/local do share GCP environments, we use separate auth domains
        # to avoid cross-contamination of registration states across environments.
        # As a special-case, add all auth domains to the shared test CDR.
        auth_domain_group_emails = SHARED_TEST_CDR_PROJECTS.map do |p|
          shared_tier = ENVIRONMENTS[p].fetch(:accessTiers)[op.opts.tier]
          shared_tier.fetch(:auth_domain_group_email)
        end
      end
      for group in auth_domain_group_emails do
        if existing_groups.include?(group)
          common.status "#{group} already in ACL, skipping..."
        else
          common.status "Adding #{group} as a READER..."
          acl_json["access"].push({
                                    "groupByEmail" => group,
                                    "role" => "READER"
                                  })
        end
      end

      app_sa = "#{op.opts.project}@appspot.gserviceaccount.com"
      if existing_users.include?(app_sa)
        common.status "#{app_sa} already in ACL, skipping..."
      else
        common.status "Adding #{app_sa} as a READER..."
        acl_json["access"].push({ "userByEmail" => app_sa, "role" => "READER" })
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
  op = WbOptionsParser.new(cmd_name, args)

  op.add_option(
    "--source-bq-dataset [dataset]",
    ->(opts, v) { opts.source_bq_dataset = v },
    "BigQuery source dataset name for the CDR version (project not included), e.g. " +
      "'2019Q4R3'. Required."
  )
  op.add_option(
    "--dest-bq-dataset [dataset]",
    ->(opts, v) { opts.dest_bq_dataset = v },
    "Destination BigQuery dataset name for the CDR version (project not included), e.g. " +
      "'2019Q4R3'. Defaults to --source-bq-dataset."
  )
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v },
    "The Google Cloud project associated with this workbench environment, " +
      "e.g. all-of-us-rw-staging. Required."
  )
  op.opts.tier = "controlled"
  op.add_option(
    "--tier [tier]",
    ->(opts, v) { opts.tier = v },
    "The access tier associated with this CDR, e.g. controlled." +
      "Default is controlled (WGS only exists in controlled tier, for the foreseeable future)."
  )
  op.add_option(
    "--table-prefixes [prefix1,prefix2,...]",
    ->(opts, v) { opts.table_prefixes = v },
    "Optional comma-delimited list of table prefixes to filter the publish " +
      "by, e.g. myfilter_,sample_info. This should only be used in special situations e.g. " +
      "if there was an issue with the publish. In general, CDRs should be treated as " +
      "immutable after the initial publish."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.source_bq_dataset and opts.project and opts.tier }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported project: #{opts.project}") unless ENVIRONMENTS.key? opts.project }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported tier: #{opts.tier}") unless ENVIRONMENTS[opts.project][:accessTiers].key? opts.tier }
  op.parse.validate

  # Allowing divergence lets us effectively rename the dataset, if desired.
  unless op.opts.dest_bq_dataset
    op.opts.dest_bq_dataset = op.opts.source_bq_dataset
  end

  common = Common.new
  env = ENVIRONMENTS[op.opts.project]
  tier = env.fetch(:accessTiers)[op.opts.tier]
  dest_fq_dataset = "#{tier.fetch(:dest_cdr_project)}:#{op.opts.dest_bq_dataset}"

  source_project = env.fetch(:source_cdr_wgs_project)
  unless source_project
    raise ArgumentError.new("missing WGS source project value for env #{op.opts.project}")
  end
  extraction_proxy_group = must_get_wgs_proxy_group(op.opts.project)

  table_match_filter = WGS_TABLE_FILTER
  if op.opts.table_prefixes
    prefixes = op.opts.table_prefixes.split(",")
    table_match_filter = "^\\(#{prefixes.join("\\|")}\\)"
  end

  service_account_context_for_bq(op.opts.project, env.fetch(:publisher_account)) do
    bq_ingest(tier, op.opts.tier, source_project, op.opts.source_bq_dataset, op.opts.dest_bq_dataset, table_match_filter)

    bq_update_acl(dest_fq_dataset) do |acl_json, existing_groups, _existing_users|
      if existing_groups.include?(extraction_proxy_group)
        common.status "#{extraction_proxy_group} already in ACL, skipping..."
      else
        common.status "Adding #{extraction_proxy_group} as a READER..."
        acl_json["access"].push({ "groupByEmail" => extraction_proxy_group, "role" => "READER" })
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

def publish_cdr_files(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)

  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v },
    "The Google Cloud project associated with this workbench environment, " +
      "e.g. all-of-us-rw-staging. Required."
  )
  op.add_option(
    "--input-manifest-file [file.yaml]",
    ->(opts, v) { opts.input_manifest_file = v },
    "The input manifest YAML file which describes a logical mapping of the files to be " +
      "published. For details on the YAML format see genomic_manifests.rb"
  )
  op.add_option(
    "--microarray-rids-file [file]",
    ->(opts, v) { opts.microarray_rids_file = v },
    "A file containing all research IDs for which Microarray data should be published. " +
      "Only applicable and required for task CREATE_COPY_MANIFESTS where " +
      "aw4MicroarraySources are specified."
  )
  op.add_option(
    "--wgs-rids-file [file]",
    ->(opts, v) { opts.wgs_rids_file = v },
    "A file containing all research IDs for which WGS data should be published. " +
      "Only applicable and required for task CREATE_COPY_MANIFESTS where " +
      "aw4WgsSources are specified."
  )
  op.add_option(
    "--working-dir [path]",
    ->(opts, v) { opts.working_dir = v },
    "Directory for intermediate manifest files and logs."
  )
  op.add_option(
    "--jira-ticket [RW-XXX]",
    ->(opts, v) { opts.jira_ticket = v },
    "Optional RW jira ticket associated with this publish; used for provenance."
  )
  op.add_option(
    "--display-version-id [version]",
    ->(opts, v) { opts.display_version_id = v },
    "A version 'id' suitable for display in the published GCS directory. Conventionally " +
      "this matches the CDR version display name in the product, e.g. 'v5'. This ID will be " +
      "included in the published file directory structure. " +
      "Only applicable and required for task CREATE_COPY_MANIFESTS."
  )
  supported_tasks = ["CREATE_COPY_MANIFESTS", "STAGE_INGEST", "PUBLISH"]
  op.opts.tasks = supported_tasks
  op.add_option(
    "--tasks [CREATE_COPY_MANIFESTS,STAGE_INGEST,PUBLISH]",
    ->(opts, v) { opts.tasks = v.split(",") },
    "Publishing tasks to execute; defaults to all tasks: #{supported_tasks}"
  )
  op.opts.tier = "controlled"
  op.add_option(
    "--tier [tier]",
    ->(opts, v) { opts.tier = v },
    "The access tier associated with this CDR, e.g. controlled." +
      "Default is controlled (WGS only exists in controlled tier, for the foreseeable future)."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.project }
  op.add_validator ->(opts) { raise ArgumentError.new("--input-manifest-file,--display-version-id are required for the CREATE_COPY_MANIFESTS task") if opts.tasks.include? "CREATE_COPY_MANIFESTS" and (not opts.display_version_id or not opts.input_manifest_file) }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported tasks: #{opts.tasks}") unless (opts.tasks - supported_tasks).empty? }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported project: #{opts.project}") unless ENVIRONMENTS.key? opts.project }
  op.add_validator ->(opts) { raise ArgumentError.new("unsupported tier: #{opts.tier}") unless ENVIRONMENTS[opts.project][:accessTiers].key? opts.tier }
  op.parse.validate

  env = ENVIRONMENTS[op.opts.project]
  tier = env.fetch(:accessTiers)[op.opts.tier]

  common = Common.new

  working_dir = op.opts.working_dir
  if working_dir.nil?
    working_dir = Dir.mktmpdir("cdr-file-publish")
  else
    FileUtils.makedirs(working_dir)
  end
  common.status("local working directory: '#{working_dir}'")

  copy_manifest_files = []
  if op.opts.tasks.include? "CREATE_COPY_MANIFESTS"
    common.status "Starting: copy manifest creation"
    copy_manifests = {}
    output_manifests = {}
    output_manifest_path = -> (source_name) { "#{working_dir}/#{source_name}_output_manifest.csv" }

    input_manifest = parse_input_manifest(op.opts.input_manifest_file)

    aw4_microarray_sources = input_manifest["aw4MicroarraySources"]
    unless aw4_microarray_sources.nil? or aw4_microarray_sources.empty?
      if op.opts.microarray_rids_file.to_s.empty?
        raise ArgumentError.new("--microarray-rids-file is required to generate copy manifests for AW4 microarray sources")
      end
      microarray_aw4_rows = read_all_microarray_aw4s(op.opts.project, read_research_ids_file(op.opts.microarray_rids_file))

      aw4_microarray_sources.each do |source_name, section|
        common.status("building manifest for '#{source_name}'")
        copy, output = build_manifests_for_aw4_section(
          op.opts.project, section, tier[:ingest_cdr_bucket], tier[:dest_cdr_bucket], op.opts.display_version_id, microarray_aw4_rows, output_manifest_path.call(source_name))
        key_name = "aw4_microarray_" + source_name
        copy_manifests[key_name] = copy
        unless output.nil?
          output_manifests[key_name] = output
        end
      end
    end

    aw4_wgs_sources = input_manifest["aw4WgsSources"]
    unless aw4_wgs_sources.nil? or aw4_wgs_sources.empty?
      if op.opts.wgs_rids_file.to_s.empty?
        raise ArgumentError.new("--wgs-rids-file is required to generate copy manifests for AW4 WGS sources")
      end
      wgs_aw4_rows = read_all_wgs_aw4s(op.opts.project, read_research_ids_file(op.opts.wgs_rids_file))

      aw4_wgs_sources.each do |source_name, section|
        common.status("building manifest for '#{source_name}'")
        copy, output = build_manifests_for_aw4_section(
          op.opts.project, section, tier[:ingest_cdr_bucket], tier[:dest_cdr_bucket], op.opts.display_version_id, wgs_aw4_rows, output_manifest_path.call(source_name))
        key_name = "aw4_wgs_" + source_name
        copy_manifests[key_name] = copy
        unless output.nil?
          output_manifests[key_name] = output
        end
      end
    end

    curation_sources = input_manifest["curationSources"]
    unless curation_sources.nil? or curation_sources.empty?
      curation_sources.each do |source_name, section|
        common.status("building manifest for '#{source_name}'")
        copy_manifests["curation_" + source_name] = build_copy_manifest_for_curation_section(section, tier[:ingest_cdr_bucket], tier[:dest_cdr_bucket], op.opts.display_version_id)
      end
    end

    preprod_sources = input_manifest["preprodCTSources"]
    unless preprod_sources.nil? or preprod_sources.empty?
      preprod_sources.each do |source_name, section|
        common.status("building manifest for '#{source_name}'")
        copy_manifests["preprod_" + source_name] = build_copy_manifest_for_preprod_section(section, tier[:ingest_cdr_bucket], tier[:dest_cdr_bucket], op.opts.display_version_id)
      end
    end

    if copy_manifests.empty?
      raise ArgumentError.new("CREATE_COPY_MANIFESTS was requested, but no copy manifests were created, input manifest may be empty")
    end

    copy_manifests.each do |source_name, copy_manifest|
      path = "#{working_dir}/#{source_name}_copy_manifest.csv"
      CSV.open(path, 'wb') do |f|
        f << copy_manifest.first.keys
        copy_manifest.each { |c| f << c.values }
      end
      copy_manifest_files.push(path)
    end
    output_manifests.each do |source_name, output_manifest|
      path = output_manifest_path.call(source_name)
      CSV.open(path, 'wb') do |f|
        f << output_manifest.first.keys
        output_manifest.each { |c| f << c.values }
      end
    end
    common.status "Finished: manifests created"
  end

  logs_dir = FileUtils.makedirs(File.join(working_dir, "logs"))
  common.status "Writing logs to #{logs_dir}"

  if copy_manifest_files.empty?
    copy_manifest_files = Dir.glob(working_dir + "/*_copy_manifest.csv")
    if copy_manifest_files.empty?
      raise ArgumentError.new(
        "no copy manifests generated or found in the working dir #{working_dir}; if you " +
          "are running PUBLISH without CREATE_COPY_MANIFESTS, be sure to " +
          "specify an existing --working-dir which contains copy manifest CSV files")
    end
  end
  if op.opts.tasks.include? "STAGE_INGEST"
    copy_manifest_tasks_by_path = {}
    all_tasks = []
    copy_manifest_files.each do |path|
      tasks = CSV.read(path, headers: true, return_headers: false)
      copy_manifest_tasks_by_path[path] = tasks
      tasks_a = tasks.each.to_a.shift
      all_tasks += tasks_a
    end

    maybe_grant_preprod_access(op.opts.project, all_tasks)
    copy_manifest_tasks_by_path.each do |path, tasks|
      common.status "Starting file staging for #{path}"
      stage_files_by_manifest(op.opts.project, tasks, File.join(logs_dir, File.basename(path, '.csv')))
    end
    maybe_revoke_preprod_access(op.opts.project, all_tasks)
    common.status "Finished: all file staging"
  end

  if op.opts.tasks.include? "PUBLISH"
    configs = build_publish_configs(copy_manifest_files)
    configs.each_index do |i|
      config = configs[i]
      with_storage_class = config[:storage_class].to_s.empty? ? "" : " (with storage class #{config[:storage_class]})"
      get_user_confirmation(
        "About to create transfer job#{with_storage_class}:\n" +
          "#{config[:source]} ->\n#{config[:dest]}\n\nAre you sure?")

      job_name = "publish-cdr-files_#{i + 1}-of-#{configs.length}"
      unless op.opts.jira_ticket.to_s.empty?
        job_name = "#{op.opts.jira_ticket}_#{job_name}"
      end
      publish(op.opts.project, config, job_name)
    end
    common.status "Finished: publishing"
  end
end

Common.register_command({
                          :invocation => "publish-cdr-files",
                          :description => "Copies and/or publishes CDR files to the researcher-accessible CDR bucket",
                          :fn => ->(*args) { publish_cdr_files("publish-cdr-files", args) }
                        })

def create_wgs_extraction_datasets(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)

  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v },
    "The Google Cloud project associated with this workbench environment, " +
      "e.g. all-of-us-rw-staging. Required."
  )
  op.opts.tier = "controlled"
  op.add_option(
    "--tier [tier]",
    ->(opts, v) { opts.tier = v },
    "The access tier associated with this CDR, " +
      "e.g. registered. Default is controlled."
  )
  op.opts.ttl = 60 * 60 * 24 * 7
  op.add_option(
    "--ttl [ttl]",
    ->(opts, v) { opts.ttl = v },
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
    "extractionDestinationDataset",
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
          acl_json["access"].push({ "groupByEmail" => proxy_group, "role" => "WRITER" })
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




def publish_other_preprod_datasets(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)

  op.add_option(
    "--bq-dataset [dataset]",
    ->(opts, v) { opts.bq_dataset = v },
    "BigQuery dataset name (project not included), e.g. " +
      "test_apple_healthkit_ingest. Required."
  )
  op.opts.project = "all-of-us-rw-preprod"

  op.opts.tier = "registered"

  op.add_option(
    "--table-prefixes [prefix1,prefix2,...]",
    ->(opts, v) { opts.table_prefixes = v },
    "Optional comma-delimited list of table prefixes to filter the publish " +
      "by, e.g. cb_,ds_. This should only be used in special situations e.g. " +
      "when the auxilliary cb_ or ds_ tables need to be updated, or if there " +
      "was an issue with the publish. In general, datasets should be treated as " +
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
  source_project = env.fetch(:source_other_datasets_project)
  dest_fq_dataset = "#{tier.fetch(:dest_other_datasets_project)}:#{op.opts.bq_dataset}"

  service_account_context_for_bq(op.opts.project, env.fetch(:publisher_account)) do
    bq_ingest_other_projects(tier, op.opts.tier, source_project, op.opts.bq_dataset, op.opts.bq_dataset, table_match_filter, table_skip_filter)

    bq_update_acl(dest_fq_dataset) do |acl_json, existing_groups, existing_users|
      auth_domain_group_emails = [tier.fetch(:auth_domain_other_datasets_email)]

      for group in auth_domain_group_emails do
        if existing_groups.include?(group)
          common.status "#{group} already in ACL, skipping..."
        else
          common.status "Adding #{group} as a READER..."
          acl_json["access"].push({
                                    "groupByEmail" => group,
                                    "role" => "READER"
                                  })
        end
      end

      app_sa = "#{op.opts.project}@appspot.gserviceaccount.com"
      if existing_users.include?(app_sa)
        common.status "#{app_sa} already in ACL, skipping..."
      else
        common.status "Adding #{app_sa} as a READER..."
        acl_json["access"].push({ "userByEmail" => app_sa, "role" => "READER" })
      end

      acl_json
    end
  end
end

Common.register_command({
                          :invocation => "publish_other_preprod_datasets",
                          :description => "Publishes a dataset by copying it into a Firecloud project and making it readable by registered users in the corresponding environment",
                          :fn => ->(*args) { publish_other_preprod_datasets("publish_other_preprod_datasets", args) }
                        })


def bq_ingest_other_projects(tier, tier_name, source_project, source_dataset_name, dest_dataset_name, table_match_filter = "", table_skip_filter = "^$")
  common = Common.new
  source_fq_dataset = "#{source_project}:#{source_dataset_name}"
  ingest_fq_dataset = "#{tier.fetch(:ingest_other_datasets_project)}:#{dest_dataset_name}"
  dest_fq_dataset = "#{tier.fetch(:dest_other_datasets_project)}:#{dest_dataset_name}"
  common.status "Copying from '#{source_fq_dataset}' -> '#{ingest_fq_dataset}' -> '#{dest_fq_dataset}'"

  # If you receive an error from "bq" like "Invalid JWT Signature", you may
  # need to delete cached BigQuery creds on your local machine. Try running
  # bq init --delete_credentials as recommended in the output.

  # Copy through an intermediate project and delete after (include TTL in case later steps fail).
  # See https://docs.google.com/document/d/1EHw5nisXspJjA9yeZput3W4-vSIcuLBU5dPizTnk1i0/edit
  common.run_inline %W{bq mk -f --default_table_expiration 86400 --dataset #{ingest_fq_dataset}}
  ingest_args = %W{./copy-bq-dataset.sh
      #{source_fq_dataset} #{ingest_fq_dataset} #{source_project}
  #{table_match_filter} #{table_skip_filter}}
  ingest_dry_stdout = common.capture_stdout(ingest_args + %W{--dry-run}, nil)
  common.run_inline ingest_args

  common.run_inline %W{bq mk -f --dataset #{dest_fq_dataset}}
  publish_args = %W{./copy-bq-dataset.sh
      #{ingest_fq_dataset} #{dest_fq_dataset} #{tier.fetch(:ingest_other_datasets_project)}
  #{table_match_filter} #{table_skip_filter}}
  publish_dry_stdout = common.capture_stdout(publish_args + %W{--dry-run}, nil)

  unless ingest_dry_stdout.lines.length == publish_dry_stdout.lines.length
    raise RuntimeError.new(
      "mismatched line count between ingest and publish:\n" +
        "ingest:\n#{ingest_dry_stdout}\n" +
        "publish:\n#{publish_dry_stdout}")
  end

  common.run_inline publish_args

  # Delete the intermediate dataset.
  common.run_inline %W{bq rm -r -f --dataset #{ingest_fq_dataset}}
end