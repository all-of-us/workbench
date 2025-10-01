require "csv"
require "json"
require "tempfile"
require "net/http"
require "uri"
require "fileutils"
require_relative "../../../../aou-utils/utils/common"
require_relative "../../../libproject/environments"
require_relative "../../../libproject/affirm"

# VWB-specific manifest building and publishing utilities
# This module handles the data flow for VWB environments without an ingest step:
#   Source (Genome Centers / Broad genomic curation) -> VWB CDR bucket

# Build VWB copy manifest for AW4 sections (Microarray and WGS)
def build_vwb_copy_manifest_for_aw4_section(input_section, dest_bucket, display_version_id, aw4_rows, output_manifest_path)
  common = Common.new
  copy_manifest = []
  output_manifest = []

  path_prefix = "#{display_version_id}/#{input_section['pooledDestPathInfix']}"
  destination_base = File.join(dest_bucket, path_prefix)

  aw4_rows.each do |aw4_row|
    input_section["aw4Columns"].each do |aw4_column|
      source_path = aw4_row[aw4_column]
      next if source_path.nil? or source_path.empty?

      # Extract filename and apply replacements if specified
      source_name = File.basename(source_path)
      dest_name = source_name

      if input_section["filenameReplace"]
        rid = aw4_row["research_id"]
        dest_name = _apply_vwb_filename_replacement(
          source_name,
          input_section["filenameMatch"],
          input_section["filenameReplace"],
          rid
        )
      end

      destination_path = File.join(destination_base, dest_name)
      storage_class = input_section.fetch("storageClass", "STANDARD")

      copy_manifest.push({
        "source" => source_path,
        "destination" => destination_path,
        "outputFileName" => dest_name,
        "storageClass" => storage_class
      })

      # Build output manifest if specified
      if input_section["outputManifestSpec"]
        output_row = {"research_id" => aw4_row["research_id"]}
        input_section["outputManifestSpec"].each do |aw4_col, output_col|
          if aw4_col == aw4_column
            output_row[output_col] = destination_path
          end
        end
        output_manifest.push(output_row) unless output_row.keys.size == 1
      end
    end
  end

  return copy_manifest, output_manifest
end

# Build VWB copy manifest for curation sections
def build_vwb_copy_manifest_for_curation_section(input_section, dest_bucket, display_version_id)
  common = Common.new
  copy_manifest = []

  # Handle absolute or relative destination paths
  if input_section['destination'].start_with?("gs://")
    destination_base = input_section['destination']
  else
    path_prefix = "#{display_version_id}/#{input_section['destination']}"
    destination_base = File.join(dest_bucket, path_prefix)
  end

  # Get source files matching the pattern
  source_uris = common.capture_stdout(["gsutil", "ls", "-d", input_section["sourcePattern"]]).split("\n")
  if source_uris.empty?
    raise ArgumentError.new("sourcePattern '#{input_section["sourcePattern"]}' did not match any files")
  end
  source_uris.reject!(&:empty?)

  storage_class = input_section.fetch("storageClass", "STANDARD")

  source_uris.each do |source_path|
    # Check if source is a folder (ends with /)
    if source_path.end_with?("/")
      # For folders, copy the folder contents directly to the destination
      # without appending the source folder name
      copy_manifest.push({
        "source" => source_path,
        "destination" => destination_base.end_with?("/") ? destination_base : "#{destination_base}/",
        "outputFileName" => nil,
        "storageClass" => storage_class
      })
    else
      # For files, handle normally
      source_name = File.basename(source_path)
      dest_name = source_name

      # Apply filename replacements if specified
      if input_section["filenameReplace"]
        dest_name = _apply_vwb_filename_replacement(
          source_name,
          input_section["filenameMatch"],
          input_section["filenameReplace"],
          nil
        )
      end

      destination_path = File.join(destination_base, dest_name)

      copy_manifest.push({
        "source" => source_path,
        "destination" => destination_path,
        "outputFileName" => dest_name,
        "storageClass" => storage_class
      })
    end
  end

  return copy_manifest
end

# Helper method for VWB filename replacement
def _apply_vwb_filename_replacement(source_name, match_pattern, replace_pattern, rid)
  return source_name if match_pattern.nil? || replace_pattern.nil?

  dest_name = source_name
  begin
    # Replace {RID} placeholder with actual research ID if present
    actual_replace = replace_pattern.dup
    actual_replace.gsub!("{RID}", rid) if rid

    # Apply regex replacement
    dest_name = source_name.gsub(Regexp.new(match_pattern), actual_replace)
  rescue => e
    Common.new.warning "Failed to apply filename replacement: #{e.message}"
  end

  return dest_name
end

# Build VWB publish configurations from manifest files
# Groups transfers into folder transfers and file transfers for Storage Transfer Service
def build_vwb_publish_configs(manifest_files)
  common = Common.new
  folder_transfers = []
  file_transfers = []

  manifest_files.each do |manifest_file|
    CSV.foreach(manifest_file, headers: true) do |row|
      source = row["source"]
      destination = row["destination"]
      storage_class = row["storageClass"] || "STANDARD"

      # Check if source is a folder (ends with /)
      if source.end_with?("/")
        # Add as a folder transfer
        folder_transfers.push({
          :source => source,
          :destination => destination,
          :storage_class => storage_class
        })
      else
        # Add to file transfers list
        file_transfers.push({
          :source => source,
          :destination => destination,
          :storage_class => storage_class
        })
      end
    end
  end

  # Group file transfers by source bucket, storage class and destination directory
  grouped_file_transfers = {}
  file_transfers.each do |transfer|
    # Extract source bucket from the GCS path
    source_bucket = transfer[:source].gsub("gs://", "").split("/")[0]
    dest_dir = File.dirname(transfer[:destination])
    storage_class = transfer[:storage_class]

    # Create a key based on source bucket, destination directory and storage class
    key = "#{source_bucket}_#{dest_dir}_#{storage_class}"

    if grouped_file_transfers[key].nil?
      grouped_file_transfers[key] = {
        :files => [],
        :source_bucket => source_bucket,
        :dest_dir => dest_dir,
        :storage_class => storage_class
      }
    end

    grouped_file_transfers[key][:files].push(transfer)
  end

  return {
    :folder_transfers => folder_transfers,
    :file_transfer_groups => grouped_file_transfers.values
  }
end

# Helper function to get OAuth token for the current user
def get_token(service_account)
  common = Common.new
  # Use gcloud to get an access token for the current logged-in user
  # (ignoring the service_account parameter, but keeping it for compatibility)
  token = common.capture_stdout([
    "gcloud", "auth", "print-access-token"
  ]).strip



  return token
end

# Helper function to create Storage Transfer job via REST API
def create_transfer_job_via_api(project, job_config, service_account = nil)
  common = Common.new

  # Get access token for the service account
  access_token = get_token(service_account)

  # API endpoint
  uri = URI("https://storagetransfer.googleapis.com/v1/transferJobs")

  # Create HTTP client
  http = Net::HTTP.new(uri.host, uri.port)
  http.use_ssl = true

  # Create request
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{access_token}"
  request["Content-Type"] = "application/json"
  request["X-Goog-User-Project"] = project

  # Add project to the job config
  job_config["projectId"] = project

  # Set the request body
  request.body = JSON.generate(job_config)

  # Debug logging
  common.status "="*60
  common.status "DEBUG: Storage Transfer Service API Request"
  common.status "="*60
  common.status "URL: #{uri}"
  common.status "Project: #{project}"
  common.status "Service Account: #{service_account}"
  common.status "Request Body:"
  common.status JSON.pretty_generate(job_config)
  common.status "="*60

  # Send request
  response = http.request(request)

  # Check response
  if response.code.to_i >= 200 && response.code.to_i < 300
    result = JSON.parse(response.body)
    common.status "Transfer job created successfully: #{result['name']}"
    return result
  else
    error_details = JSON.parse(response.body) rescue response.body
    raise "Failed to create transfer job: #{response.code} - #{error_details}"
  end
end


# Generate Storage Transfer Service job configurations from manifest files
def generate_sts_job_configs(project, manifest_files, jira_ticket = nil, service_account = nil)
  common = Common.new
  common.status "Generating Storage Transfer Service job configurations..."

  configs = build_vwb_publish_configs(manifest_files)
  job_configs = []
  job_index = 1

  # Process folder transfers
  configs[:folder_transfers].each do |transfer|
    job_name = "vwb-folder-transfer-#{job_index}"
    job_name = "#{jira_ticket}-#{job_name}" if jira_ticket

    config = generate_folder_transfer_config(
      project,
      transfer[:source],
      transfer[:destination],
      transfer[:storage_class],
      job_name,
      service_account
    )

    job_configs.push({
      "job_name" => job_name,
      "job_type" => "folder_transfer",
      "config" => config
    })
    job_index += 1
  end

  # Process grouped file transfers
  configs[:file_transfer_groups].each do |file_group|
    next if file_group[:files].empty?

    job_name = "vwb-files-transfer-#{job_index}"
    job_name = "#{jira_ticket}-#{job_name}" if jira_ticket

    config = generate_file_list_transfer_config(
      project,
      file_group,
      job_name,
      service_account
    )

    job_configs.push({
      "job_name" => job_name,
      "job_type" => "file_list_transfer",
      "file_group" => file_group,
      "config" => config
    })
    job_index += 1
  end

  return job_configs
end

# Generate folder transfer configuration (without creating the job)
def generate_folder_transfer_config(project, source, destination, storage_class, job_name, service_account = nil)
  # Extract bucket and path components
  source_bucket = source.gsub("gs://", "").split("/")[0]
  source_path = source.gsub(/^gs:\/\/[^\/]+\//, "")

  dest_bucket = destination.gsub("gs://", "").split("/")[0]
  dest_path = destination.gsub(/^gs:\/\/[^\/]+\//, "")

  # Ensure paths end with / for folder transfers
  source_path_with_slash = source_path.end_with?("/") ? source_path : "#{source_path}/"
  dest_path_with_slash = dest_path.end_with?("/") ? dest_path : "#{dest_path}/"

  # Build transfer job configuration
  job_config = {
    "description" => "VWB folder transfer: #{source_path} to #{dest_path} (#{job_name})",
    "status" => "ENABLED",
    "schedule" => {
      "scheduleStartDate" => {
        "year" => Time.now.year,
        "month" => Time.now.month,
        "day" => Time.now.day
      },
      "scheduleEndDate" => {
        "year" => Time.now.year,
        "month" => Time.now.month,
        "day" => Time.now.day
      }
    },
    "transferSpec" => {
      "gcsDataSource" => {
        "bucketName" => source_bucket,
        "path" => source_path_with_slash
      },
      "gcsDataSink" => {
        "bucketName" => dest_bucket,
        "path" => dest_path_with_slash
      },
      "transferOptions" => {
        "overwriteObjectsAlreadyExistingInSink" => true
      }
    },
    "projectId" => project
  }

  # Add the service account at the root level if provided
  if service_account
    job_config["serviceAccount"] = service_account
  end

  # Add storage class if not STANDARD
  if storage_class && storage_class != "STANDARD"
    job_config["transferSpec"]["objectConditions"] = {
      "storageClass" => storage_class
    }
  end

  return job_config
end

# Generate file list transfer configuration (without creating the job)
def generate_file_list_transfer_config(project, file_group, job_name, service_account = nil)
  # Get source and destination buckets
  first_file = file_group[:files].first
  source_bucket = first_file[:source].gsub("gs://", "").split("/")[0]
  dest_bucket = first_file[:destination].gsub("gs://", "").split("/")[0]

  # The manifest path will be created during publish
  temp_manifest_path = "gs://#{dest_bucket}/temp-manifests/#{job_name}-manifest.txt"

  # Build transfer job configuration
  job_config = {
    "description" => "VWB file list transfer: #{file_group[:files].length} files (#{job_name})",
    "status" => "ENABLED",
    "schedule" => {
      "scheduleStartDate" => {
        "year" => Time.now.year,
        "month" => Time.now.month,
        "day" => Time.now.day
      },
      "scheduleEndDate" => {
        "year" => Time.now.year,
        "month" => Time.now.month,
        "day" => Time.now.day
      }
    },
    "transferSpec" => {
      "gcsDataSource" => {
        "bucketName" => source_bucket
      },
      "gcsDataSink" => {
        "bucketName" => dest_bucket
      },
      "transferManifest" => {
        "location" => temp_manifest_path
      },
      "transferOptions" => {
        "overwriteObjectsAlreadyExistingInSink" => true
      }
    },
    "projectId" => project
  }

  # Add the service account at the root level if provided
  if service_account
    job_config["serviceAccount"] = service_account
  end

  # Add storage class if not STANDARD
  if file_group[:storage_class] && file_group[:storage_class] != "STANDARD"
    job_config["transferSpec"]["objectConditions"] = {
      "storageClass" => file_group[:storage_class]
    }
  end

  return job_config
end

# Save STS job configurations to files
def save_sts_configs(working_dir, job_configs)
  common = Common.new
  config_dir = File.join(working_dir, "storage_transfer_configs")

  # Create directory if it doesn't exist
  FileUtils.mkdir_p(config_dir)

  job_configs.each_with_index do |job, index|
    filename = sprintf("%03d_%s.json", index + 1, job["job_name"])
    filepath = File.join(config_dir, filename)

    File.open(filepath, 'w') do |f|
      f.write(JSON.pretty_generate(job))
    end

    common.status "Saved STS config: #{filename}"
  end

  common.status "Saved #{job_configs.length} STS job configurations to #{config_dir}"
  return config_dir
end

# Load STS job configurations from files
def load_sts_configs(config_dir)
  common = Common.new

  unless File.directory?(config_dir)
    raise ArgumentError.new("STS config directory not found: #{config_dir}")
  end

  config_files = Dir.glob(File.join(config_dir, "*.json")).sort

  if config_files.empty?
    raise ArgumentError.new("No STS config files found in #{config_dir}")
  end

  job_configs = []
  config_files.each do |filepath|
    config_data = JSON.parse(File.read(filepath))
    job_configs.push(config_data)
    common.status "Loaded STS config: #{File.basename(filepath)}"
  end

  common.status "Loaded #{job_configs.length} STS job configurations"
  return job_configs
end

# Execute STS job configurations
def execute_sts_configs(project, job_configs, dry_run = false)
  common = Common.new

  total_jobs = job_configs.length

  if dry_run
    common.status "="*60
    common.status "DRY RUN SUMMARY: Would create #{total_jobs} Storage Transfer Service job(s)"

    job_configs.each_with_index do |job, index|
      common.status "\n[Transfer Job #{index + 1}/#{total_jobs}]"
      common.status "Job Name: #{job['job_name']}"
      common.status "Job Type: #{job['job_type']}"

      if job['job_type'] == 'folder_transfer'
        source = "gs://#{job['config']['transferSpec']['gcsDataSource']['bucketName']}/#{job['config']['transferSpec']['gcsDataSource']['path']}"
        dest = "gs://#{job['config']['transferSpec']['gcsDataSink']['bucketName']}/#{job['config']['transferSpec']['gcsDataSink']['path']}"
        common.status "  Source: #{source}"
        common.status "  Destination: #{dest}"
      elsif job['job_type'] == 'file_list_transfer'
        common.status "  Number of files: #{job['file_group']['files'].length}" if job['file_group']
      end
    end

    common.status "\n" + "="*60
    common.status "DRY RUN COMPLETE: No transfer jobs were created"
    return
  end

  # Get user confirmation
  common.status "="*60
  common.status "TRANSFER SUMMARY: About to create #{total_jobs} Storage Transfer Service job(s)"
  common.status "="*60

  get_user_confirmation(
    "\nAbout to create #{total_jobs} VWB Storage Transfer Service job(s).\n" +
    "These transfers will copy data to the VWB CDR bucket.\n\n" +
    "Are you sure you want to proceed?"
  )

  # Execute each job configuration
  successful_jobs = 0
  failed_jobs = 0

  job_configs.each_with_index do |job, index|
    begin
      common.status "\n[#{index + 1}/#{total_jobs}] Creating transfer job: #{job['job_name']}"

      if job['job_type'] == 'file_list_transfer' && job['file_group']
        # For file list transfers, we need to create the manifest first
        create_and_execute_file_list_transfer(project, job)
      else
        # For folder transfers, just execute the config
        create_transfer_job_via_api(project, job['config'])
      end

      successful_jobs += 1
    rescue => e
      common.error "Failed to create job #{job['job_name']}: #{e.message}"
      failed_jobs += 1
    end
  end

  common.status "\n" + "="*60
  common.status "Transfer job creation complete:"
  common.status "  Successful: #{successful_jobs} job(s)"
  common.status "  Failed: #{failed_jobs} job(s)" if failed_jobs > 0
  common.status "="*60
end

# Helper to create manifest and execute file list transfer
def create_and_execute_file_list_transfer(project, job)
  common = Common.new
  file_group = job['file_group']

  # Get service account from the job config for impersonation
  service_account = job['config']['serviceAccount']
  impersonate_args = service_account ? ["-i", service_account] : []

  # Create a temporary manifest file
  manifest_file = Tempfile.new(["vwb-transfer-manifest-", ".txt"])

  begin
    # Write source file paths to manifest
    file_group['files'].each do |file|
      object_path = file['source'].gsub(/^gs:\/\/[^\/]+\//, "")
      manifest_file.puts(object_path)
    end
    manifest_file.close

    # Upload manifest to the location specified in config using SA impersonation
    manifest_path = job['config']['transferSpec']['transferManifest']['location']
    common.run_inline(["gsutil"] + impersonate_args + ["cp", manifest_file.path, manifest_path])

    # Create the transfer job
    create_transfer_job_via_api(project, job['config'])

    # Clean up temporary manifest using SA impersonation
    common.run_inline(["gsutil"] + impersonate_args + ["rm", manifest_path])
  ensure
    manifest_file.unlink if manifest_file
  end
end

# Post-process VWB files to rename them if needed
# Helper method to create VWB copy manifests
def vwb_create_copy_manifests_mode(opts, tier, working_dir)
  common = Common.new
  common.status "Starting: VWB copy manifest creation"

  copy_manifests = {}
  output_manifests = {}
  output_manifest_path = -> (source_name) { "#{working_dir}/#{source_name}_output_manifest.csv" }
  copy_manifest_files = []

  input_manifest = parse_input_manifest(opts.input_manifest_file)

  # Process AW4 Microarray sources
  aw4_microarray_sources = input_manifest["aw4MicroarraySources"]
  unless aw4_microarray_sources.nil? or aw4_microarray_sources.empty?
    if opts.microarray_rids_file.to_s.empty?
      raise ArgumentError.new("--microarray-rids-file is required to generate copy manifests for AW4 microarray sources")
    end
    microarray_aw4_rows = read_all_microarray_aw4s(opts.project, read_research_ids_file(opts.microarray_rids_file))

    aw4_microarray_sources.each do |source_name, section|
      common.status("building VWB manifest for '#{source_name}'")
      copy, output = build_vwb_copy_manifest_for_aw4_section(
        section, tier[:dest_cdr_bucket], opts.display_version_id, microarray_aw4_rows, output_manifest_path.call(source_name))
      key_name = "aw4_microarray_" + source_name
      copy_manifests[key_name] = copy
      unless output.nil?
        output_manifests[key_name] = output
      end
    end
  end

  # Process AW4 WGS sources
  aw4_wgs_sources = input_manifest["aw4WgsSources"]
  unless aw4_wgs_sources.nil? or aw4_wgs_sources.empty?
    if opts.wgs_rids_file.to_s.empty?
      raise ArgumentError.new("--wgs-rids-file is required to generate copy manifests for AW4 WGS sources")
    end
    wgs_aw4_rows = read_all_wgs_aw4s(opts.project, read_research_ids_file(opts.wgs_rids_file))

    aw4_wgs_sources.each do |source_name, section|
      common.status("building VWB manifest for '#{source_name}'")
      copy, output = build_vwb_copy_manifest_for_aw4_section(
        section, tier[:dest_cdr_bucket], opts.display_version_id, wgs_aw4_rows, output_manifest_path.call(source_name))
      key_name = "aw4_wgs_" + source_name
      copy_manifests[key_name] = copy
      unless output.nil?
        output_manifests[key_name] = output
      end
    end
  end

  # Process curation sources
  curation_sources = input_manifest["curationSources"]
  unless curation_sources.nil? or curation_sources.empty?
    curation_sources.each do |source_name, section|
      common.status("building VWB manifest for '#{source_name}'")
      copy_manifests["curation_" + source_name] = build_vwb_copy_manifest_for_curation_section(
        section, tier[:dest_cdr_bucket], opts.display_version_id)
    end
  end

  if copy_manifests.empty?
    raise ArgumentError.new("CREATE_COPY_MANIFESTS was requested, but no copy manifests were created, input manifest may be empty")
  end

  # Write copy manifests to files
  copy_manifests.each do |source_name, copy_manifest|
    path = "#{working_dir}/#{source_name}_copy_manifest.csv"
    CSV.open(path, 'wb') do |f|
      f << copy_manifest.first.keys
      copy_manifest.each { |c| f << c.values }
    end
    copy_manifest_files.push(path)
  end

  # Write output manifests to files
  output_manifests.each do |source_name, output_manifest|
    path = output_manifest_path.call(source_name)
    CSV.open(path, 'wb') do |f|
      f << output_manifest.first.keys
      output_manifest.each { |c| f << c.values }
    end
  end

  common.status "Finished: VWB manifests created"

  # Generate and save Storage Transfer Service job configurations
  common.status "Generating Storage Transfer Service job configurations..."

  # Get the environment configuration
  env = ENVIRONMENTS[opts.project]
  service_account = env.fetch(:publisher_account)
  publishing_project = env.fetch(:publishing_project, opts.project)

  # Generate STS job configs from the manifest files
  job_configs = generate_sts_job_configs(
    publishing_project,
    copy_manifest_files,
    opts.jira_ticket,
    service_account
  )

  # Save the STS configs to files
  config_dir = save_sts_configs(working_dir, job_configs)
  common.status "STS job configurations saved to: #{config_dir}"
  common.status "You can review and modify these configurations before running PUBLISH"

  copy_manifest_files
end

# Helper method to publish VWB manifests
def vwb_publish_mode(opts, copy_manifest_files, working_dir)
  common = Common.new

  if opts.dry_run
    common.status "Starting: VWB publishing phase (DRY RUN - no transfers will be created)"
  else
    common.status "Starting: VWB publishing phase using Storage Transfer Service"
  end

  # Load STS job configurations from the storage_transfer_configs directory
  config_dir = File.join(working_dir, "storage_transfer_configs")

  unless File.directory?(config_dir)
    raise ArgumentError.new(
      "STS config directory not found: #{config_dir}\n" +
      "Please run with CREATE_COPY_MANIFESTS task first to generate the configurations.")
  end

  job_configs = load_sts_configs(config_dir)

  # Get the publishing project for execution
  env = ENVIRONMENTS[opts.project]
  publishing_project = env.fetch(:publishing_project, opts.project)

  # Execute the STS job configurations
  execute_sts_configs(publishing_project, job_configs, opts.dry_run)

  if opts.dry_run
    common.status "Finished: VWB publishing dry run"
  else
    common.status "Finished: VWB publishing"
  end

  copy_manifest_files
end

# Helper method to post-process VWB files
def vwb_post_process_mode(opts, copy_manifest_files, working_dir)
  common = Common.new

  if opts.dry_run
    common.status "Starting: VWB post-processing phase (DRY RUN)"
  else
    common.status "Starting: VWB post-processing phase"
  end

  # Ensure we have manifest files to process
  if copy_manifest_files.empty?
    copy_manifest_files = Dir.glob(working_dir + "/*_copy_manifest.csv")
    if copy_manifest_files.empty?
      raise ArgumentError.new(
        "no copy manifests found in the working dir #{working_dir}; " +
        "POST_PROCESS requires manifest files from CREATE_COPY_MANIFESTS")
    end
  end

  # Post-process files to rename them if needed
  post_process_vwb_files(opts.project, copy_manifest_files, opts.dry_run)

  if opts.dry_run
    common.status "Finished: VWB post-processing dry run"
  else
    common.status "Finished: VWB post-processing"
  end
end

def post_process_vwb_files(project, manifest_files, dry_run = false)
  common = Common.new

  # Get the publisher account for impersonation
  env = ENVIRONMENTS[project]
  deploy_account = env.fetch(:publisher_account)

  if dry_run
    common.status "Starting VWB post-processing (DRY RUN)..."
  else
    common.status "Starting VWB post-processing..."
  end

  total_files_to_rename = 0
  files_to_rename = []

  # Read all manifest files and identify files that need renaming
  manifest_files.each do |manifest_file|
    CSV.foreach(manifest_file, headers: true) do |row|
      source = row["source"]
      destination = row["destination"]
      output_filename = row["outputFileName"]

      # Skip if it's a folder
      next if source.end_with?("/")

      # Extract filenames
      source_filename = File.basename(source)
      dest_filename = output_filename || File.basename(destination)

      # Check if renaming is needed
      if source_filename != dest_filename
        files_to_rename.push({
          :source_name => source_filename,
          :dest_name => dest_filename,
          :destination_path => destination
        })
        total_files_to_rename += 1
      end
    end
  end

  if total_files_to_rename == 0
    common.status "No files need renaming. Post-processing complete."
    return
  end

  if dry_run
    common.status "="*60
    common.status "DRY RUN: Would rename #{total_files_to_rename} file(s)"
    common.status "="*60
    common.status ""

    # Show sample of files to be renamed
    sample_size = [5, total_files_to_rename].min
    common.status "Sample files to be renamed (showing #{sample_size} of #{total_files_to_rename}):"
    files_to_rename.take(sample_size).each do |file|
      common.status "  #{file[:source_name]} -> #{file[:dest_name]}"
      common.status "    at: #{file[:destination_path]}"
    end

    if total_files_to_rename > sample_size
      common.status "  ... and #{total_files_to_rename - sample_size} more file(s)"
    end

    common.status ""
    common.status "DRY RUN COMPLETE: No files were renamed"
    return
  end

  # Get user confirmation for actual renaming
  common.status "="*60
  common.status "POST-PROCESSING: About to rename #{total_files_to_rename} file(s)"
  common.status "="*60

  get_user_confirmation(
    "\nAbout to rename #{total_files_to_rename} file(s) in the VWB CDR bucket.\n" +
    "This will modify file names in place.\n\n" +
    "Are you sure you want to proceed?"
  )

  common.status ""
  common.status "Renaming files..."

  successful_renames = 0
  failed_renames = 0

  files_to_rename.each_with_index do |file, index|
    # Get the directory path from the destination
    dest_dir = File.dirname(file[:destination_path])

    # Construct the current path (where the file was transferred with original name)
    current_path = File.join(dest_dir, file[:source_name])

    # The destination path already has the correct name
    new_path = file[:destination_path]

    begin
      common.status "[#{index + 1}/#{total_files_to_rename}] Renaming: #{file[:source_name]} -> #{file[:dest_name]}"

      # Use gcloud storage mv with impersonation to rename the file
      cmd = [
        "gcloud", "storage", "mv",
        "--impersonate-service-account", deploy_account,
        current_path,
        new_path
      ]

      common.run_inline(cmd)
      successful_renames += 1
    rescue => e
      common.error "Failed to rename #{current_path}: #{e.message}"
      failed_renames += 1
    end
  end

  common.status ""
  common.status "="*60
  common.status "Post-processing complete:"
  common.status "  Successfully renamed: #{successful_renames} file(s)"
  common.status "  Failed to rename: #{failed_renames} file(s)" if failed_renames > 0
  common.status "="*60
end
