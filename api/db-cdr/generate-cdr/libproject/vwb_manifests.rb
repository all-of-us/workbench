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

# Helper to apply filename replacement for AW4 files
def apply_aw4_filename_replacement(source_name, input_section, rid)
  return source_name unless input_section["filenameReplace"]

  _apply_vwb_filename_replacement(
    source_name,
    input_section["filenameMatch"],
    input_section["filenameReplace"],
    rid
  )
end

# Helper to get pooled path with delta/base suffix
def _get_vwb_pooled_path(path_infix, display_version_id, is_delta_release)
  version_postfix = "base"

  unless is_delta_release.nil?
    version_postfix = "delta"
  end

  return "pooled/#{path_infix}/#{display_version_id}_#{version_postfix}"
end

# Helper to read previous manifest for delta releases
def _read_vwb_previous_manifest(project, dest_bucket, delta_release_manifest_path, delta_release, path_infix)
  common = Common.new
  prev_manifest = ""

  # Get the deploy account for impersonation
  env = ENVIRONMENTS[project]
  deploy_account = env.fetch(:publisher_account)

  manifest_path = delta_release_manifest_path

  # If deltaReleaseManifestPath is specified, try to use it.
  unless delta_release_manifest_path.nil?
    if delta_release_manifest_path.start_with?("gs://")
      prev_manifest = common.capture_stdout(["gsutil", "-i", deploy_account, "cat", delta_release_manifest_path])
    else
      prev_manifest = IO.read(delta_release_manifest_path)
    end
  end

  # Try to find the manifest given the deltaRelease field.
  if !delta_release.nil? && prev_manifest.empty?
    manifest_path = "#{dest_bucket}/#{delta_release}/#{path_infix}/manifest.csv"
    prev_manifest = common.capture_stdout(["gsutil", "-u", project, "-i", deploy_account, "cat", manifest_path])
  end

  # If the manifest still cannot be read then throw an error because config is not correct.
  unless delta_release.nil? && delta_release_manifest_path.nil?
    if prev_manifest.empty?
      raise ArgumentError.new("failed to read previous manifest from #{manifest_path}, " +
        "make sure to provide the correct previous release in the input manifest")
    end
  end

  # Convert the prev_manifest to dict of research ID -> CSV::Row
  return {} if prev_manifest.empty?

  prev_manifest_csv = CSV.parse(prev_manifest, headers: true)
  prev_manifest_hash = {}
  prev_manifest_csv.each do |manifest_row|
    # Use person_id to match genomic_manifests logic
    rid = manifest_row["person_id"]
    prev_manifest_hash[rid] = manifest_row
  end

  return prev_manifest_hash
end

# Build VWB copy manifest for AW4 sections (Microarray and WGS)
def build_vwb_copy_manifest_for_aw4_section(input_section, dest_bucket, display_version_id, aw4_rows, output_manifest_path, project = nil)
  copy_manifest = []
  output_manifest = []

  # Use pooled path for delta releases
  use_pooled = !input_section["deltaRelease"].nil?

  if use_pooled
    path_prefix = _get_vwb_pooled_path(input_section['pooledDestPathInfix'], display_version_id, input_section["deltaRelease"])
  else
    path_prefix = "#{display_version_id}/#{input_section['pooledDestPathInfix']}"
  end

  destination_base = File.join(dest_bucket, path_prefix)

  # Read previous manifest for delta releases
  prev_manifest = {}
  if !input_section["deltaRelease"].nil? && !project.nil?
    prev_manifest = _read_vwb_previous_manifest(
      project,
      dest_bucket,
      input_section["deltaReleaseManifestPath"],
      input_section["deltaRelease"],
      input_section["pooledDestPathInfix"]
    )
  end

  # Track output rows by research_id to accumulate all columns
  output_rows_by_rid = {}

  aw4_rows.each do |aw4_row|
    rid = aw4_row["research_id"]

    # Check if this research_id exists in previous manifest
    if !prev_manifest.empty? && prev_manifest.key?(rid)
      # For delta releases, if research_id exists in previous manifest,
      # include it in output manifest with previous values but skip copy manifest
      if input_section["outputManifestSpec"]
        output_rows_by_rid[rid] = prev_manifest[rid].to_h
      end
      next  # Skip copy manifest generation for existing research_ids
    end

    # Initialize output row for this research_id if needed
    if input_section["outputManifestSpec"] && !output_rows_by_rid.key?(rid)
      # Use person_id to match genomic_manifests field naming
      output_rows_by_rid[rid] = {"person_id" => rid}
    end

    input_section["aw4Columns"].each do |aw4_column|
      source_path = aw4_row[aw4_column]
      next if source_path.nil? || source_path.empty?

      # Build copy manifest entry
      source_name = File.basename(source_path)
      dest_name = apply_aw4_filename_replacement(source_name, input_section, rid)
      destination_path = File.join(destination_base, dest_name)
      storage_class = input_section.fetch("storageClass", "STANDARD")

      manifest_entry = {
        "source" => source_path,
        "destination" => destination_path,
        "outputFileName" => dest_name,
        "storageClass" => storage_class
      }
      copy_manifest.push(manifest_entry)

      # Add to output manifest if specified
      if input_section["outputManifestSpec"]
        input_section["outputManifestSpec"].each do |aw4_col, output_col|
          if aw4_col == aw4_column
            output_rows_by_rid[rid][output_col] = destination_path
          end
        end
      end
    end
  end

  # Convert output rows hash to array
  output_manifest = output_rows_by_rid.values unless output_rows_by_rid.empty?

  return copy_manifest, output_manifest
end

# Helper to determine destination base path
def determine_destination_base(input_section, dest_bucket, display_version_id)
  if input_section['destination'].start_with?("gs://")
    return input_section['destination']
  else
    path_prefix = "#{display_version_id}/#{input_section['destination']}"
    return File.join(dest_bucket, path_prefix)
  end
end

# Helper to process folder source for VWB copy manifest
def process_folder_source(source_path, destination_base, storage_class)
  return {
    "source" => source_path,
    "destination" => destination_base.end_with?("/") ? destination_base : "#{destination_base}/",
    "outputFileName" => nil,
    "storageClass" => storage_class
  }
end

# Helper to process file source for VWB copy manifest
def process_file_source(source_path, destination_base, storage_class, input_section)
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

  return {
    "source" => source_path,
    "destination" => destination_path,
    "outputFileName" => dest_name,
    "storageClass" => storage_class
  }
end

# Build VWB copy manifest for curation sections
def build_vwb_copy_manifest_for_curation_section(input_section, dest_bucket, display_version_id, project = nil)
  common = Common.new
  copy_manifest = []

  # Check if this is a delta release
  use_pooled = !input_section["deltaRelease"].nil?

  # Determine destination base path
  if use_pooled
    # For delta releases with pooled path
    path_infix = input_section['destination']
    path_prefix = _get_vwb_pooled_path(path_infix, display_version_id, input_section["deltaRelease"])
    destination_base = File.join(dest_bucket, path_prefix)
  else
    # Use normal path determination
    destination_base = determine_destination_base(input_section, dest_bucket, display_version_id)
  end

  # Get source files matching the pattern
  source_uris = common.capture_stdout(["gsutil", "ls", "-d", input_section["sourcePattern"]]).split("\n")
  if source_uris.empty?
    raise ArgumentError.new("sourcePattern '#{input_section["sourcePattern"]}' did not match any files")
  end
  source_uris.reject!(&:empty?)

  storage_class = input_section.fetch("storageClass", "STANDARD")

  # For delta releases, read previous manifest to check which files already exist
  prev_files = {}
  if !input_section["deltaRelease"].nil? && !project.nil?
    prev_manifest_path = input_section["deltaReleaseManifestPath"]
    delta_release = input_section["deltaRelease"]

    # Try to read previous manifest
    begin
      env = ENVIRONMENTS[project]
      deploy_account = env.fetch(:publisher_account)

      if !prev_manifest_path.nil? && prev_manifest_path.start_with?("gs://")
        prev_manifest_content = common.capture_stdout(["gsutil", "-i", deploy_account, "cat", prev_manifest_path])
      elsif !delta_release.nil?
        # Try to find manifest based on delta release version
        manifest_path = "#{dest_bucket}/#{delta_release}/#{input_section['destination']}/manifest.csv"
        prev_manifest_content = common.capture_stdout(["gsutil", "-u", project, "-i", deploy_account, "cat", manifest_path])
      end

      unless prev_manifest_content.nil? || prev_manifest_content.empty?
        CSV.parse(prev_manifest_content, headers: true).each do |row|
          # Store the source file path as key
          prev_files[row["source"]] = true if row["source"]
        end
      end
    rescue => e
      common.warning "Could not read previous manifest for delta release: #{e.message}"
    end
  end

  source_uris.each do |source_path|
    # Skip if file already exists in previous release (for delta releases)
    next if !prev_files.empty? && prev_files.key?(source_path)

    if source_path.end_with?("/")
      # Process folder
      manifest_entry = process_folder_source(source_path, destination_base, storage_class)
    else
      # Process file
      manifest_entry = process_file_source(source_path, destination_base, storage_class, input_section)
    end
    copy_manifest.push(manifest_entry)
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

# Helper function to get OAuth token for the current user1
def get_token()
  common = Common.new
  # Use gcloud to get an access token for the current logged-in user
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


# Helper to process folder transfers and generate job configs
def process_folder_transfers(configs, project, jira_ticket, service_account, job_index)
  job_configs = []

  configs[:folder_transfers].each do |transfer|
    job_name = generate_job_name("vwb-folder-transfer", job_index, jira_ticket)

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

  return job_configs, job_index
end

# Helper to process file list transfers and generate job configs
def process_file_list_transfers(configs, project, jira_ticket, service_account, job_index, sts_manifests_dir, common)
  job_configs = []

  configs[:file_transfer_groups].each do |file_group|
    next if file_group[:files].empty?

    job_name = generate_job_name("vwb-files-transfer", job_index, jira_ticket)

    # Create the manifest file locally for review
    local_manifest_path = create_local_manifest_file(file_group, job_name, sts_manifests_dir, common)

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
      "config" => config,
      "local_manifest_path" => local_manifest_path
    })
    job_index += 1
  end

  return job_configs, job_index
end

# Helper to generate job name with optional jira ticket prefix
def generate_job_name(base_name, index, jira_ticket = nil)
  job_name = "#{base_name}-#{index}"
  jira_ticket ? "#{jira_ticket}-#{job_name}" : job_name
end

# Helper to create local manifest file for file list transfers
def create_local_manifest_file(file_group, job_name, sts_manifests_dir, common)
  manifest_filename = "#{job_name}-manifest.txt"
  local_manifest_path = File.join(sts_manifests_dir, manifest_filename)

  # Write source file paths to manifest
  File.open(local_manifest_path, 'w') do |f|
    file_group[:files].each do |file|
      # Extract object path from full GCS URL
      object_path = file[:source].gsub(/^gs:\/\/[^\/]+\//, "")
      f.puts(object_path)
    end
  end

  common.status "Created STS manifest: #{manifest_filename} (#{file_group[:files].length} files)"
  return local_manifest_path
end

# Generate Storage Transfer Service job configurations from manifest files
def generate_sts_job_configs(project, manifest_files, working_dir, jira_ticket = nil, service_account = nil)
  common = Common.new
  common.status "Generating Storage Transfer Service job configurations..."

  configs = build_vwb_publish_configs(manifest_files)
  job_configs = []
  job_index = 1

  # Create directory for STS manifests
  sts_manifests_dir = File.join(working_dir, "sts_manifests")
  FileUtils.mkdir_p(sts_manifests_dir)

  # Process folder transfers
  folder_configs, job_index = process_folder_transfers(configs, project, jira_ticket, service_account, job_index)
  job_configs.concat(folder_configs)

  # Process file list transfers
  file_configs, _ = process_file_list_transfers(configs, project, jira_ticket, service_account, job_index, sts_manifests_dir, common)
  job_configs.concat(file_configs)

  common.status "Created #{configs[:file_transfer_groups].count { |g| !g[:files].empty? }} STS manifest files in #{sts_manifests_dir}"
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

# Helper to display dry run summary for STS jobs
def display_sts_dry_run_summary(common, job_configs)
  total_jobs = job_configs.length

  common.status "="*60
  common.status "DRY RUN SUMMARY: Would create #{total_jobs} Storage Transfer Service job(s)"

  job_configs.each_with_index do |job, index|
    common.status "\n[Transfer Job #{index + 1}/#{total_jobs}]"
    common.status "Job Name: #{job['job_name']}"
    common.status "Job Type: #{job['job_type']}"

    display_job_details(common, job)
  end

  common.status "\n" + "="*60
  common.status "DRY RUN COMPLETE: No transfer jobs were created"
end

# Helper to display job details based on type
def display_job_details(common, job)
  if job['job_type'] == 'folder_transfer'
    source = "gs://#{job['config']['transferSpec']['gcsDataSource']['bucketName']}/#{job['config']['transferSpec']['gcsDataSource']['path']}"
    dest = "gs://#{job['config']['transferSpec']['gcsDataSink']['bucketName']}/#{job['config']['transferSpec']['gcsDataSink']['path']}"
    common.status "  Source: #{source}"
    common.status "  Destination: #{dest}"
  elsif job['job_type'] == 'file_list_transfer' && job['file_group']
    common.status "  Number of files: #{job['file_group']['files'].length}"
  end
end

# Helper to execute a single STS job
def execute_single_sts_job(project, job, index, total_jobs, common)
  common.status "\n[#{index + 1}/#{total_jobs}] Creating transfer job: #{job['job_name']}"

  if job['job_type'] == 'file_list_transfer' && job['file_group']
    create_and_execute_file_list_transfer(project, job)
  else
    create_transfer_job_via_api(project, job['config'])
  end
end

# Helper to execute all STS jobs with confirmation
def execute_sts_jobs_with_confirmation(project, job_configs, common)
  total_jobs = job_configs.length

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
      execute_single_sts_job(project, job, index, total_jobs, common)
      successful_jobs += 1
    rescue => e
      common.error "Failed to create job #{job['job_name']}: #{e.message}"
      failed_jobs += 1
    end
  end

  # Display summary
  common.status "\n" + "="*60
  common.status "Transfer job creation complete:"
  common.status "  Successful: #{successful_jobs} job(s)"
  common.status "  Failed: #{failed_jobs} job(s)" if failed_jobs > 0
  common.status "="*60
end

# Execute STS job configurations
def execute_sts_configs(project, job_configs, dry_run = false)
  common = Common.new

  if dry_run
    display_sts_dry_run_summary(common, job_configs)
  else
    execute_sts_jobs_with_confirmation(project, job_configs, common)
  end
end

# Helper to get impersonation arguments for service account
def get_impersonation_args(service_account)
  service_account ? ["-i", service_account] : []
end

# Helper to upload pre-created manifest to GCS
def upload_pre_created_manifest(common, local_path, gcs_path, impersonate_args)
  common.status "Uploading pre-created manifest to GCS: #{File.basename(local_path)}"
  common.run_inline(["gsutil"] + impersonate_args + ["cp", local_path, gcs_path])
end

# Helper to create and upload manifest on the fly
def create_and_upload_manifest_on_fly(common, job, manifest_path, impersonate_args)
  common.warning "No pre-created manifest found, generating on the fly"
  file_group = job['file_group']

  # Create a temporary manifest file
  manifest_file = Tempfile.new(["vwb-transfer-manifest-", ".txt"])

  begin
    # Write source file paths to manifest
    file_group['files'].each do |file|
      object_path = file['source'].gsub(/^gs:\/\/[^\/]+\//, "")
      manifest_file.puts(object_path)
    end
    manifest_file.close

    # Upload manifest to GCS using SA impersonation
    common.run_inline(["gsutil"] + impersonate_args + ["cp", manifest_file.path, manifest_path])
  ensure
    manifest_file.unlink if manifest_file
  end
end

# Helper to execute transfer job and cleanup
def execute_transfer_and_cleanup(common, project, job_config, manifest_path, impersonate_args)
  begin
    # Create the transfer job
    create_transfer_job_via_api(project, job_config)

    # Clean up temporary manifest from GCS using SA impersonation
    common.status "Cleaning up manifest from GCS"
    common.run_inline(["gsutil"] + impersonate_args + ["rm", manifest_path])
  rescue => e
    # If job creation fails, still try to clean up the manifest
    common.run_inline(["gsutil"] + impersonate_args + ["rm", manifest_path]) rescue nil
    raise e
  end
end

# Helper to create manifest and execute file list transfer
def create_and_execute_file_list_transfer(project, job)
  common = Common.new

  # Get service account and impersonation args
  service_account = job['config']['serviceAccount']
  impersonate_args = get_impersonation_args(service_account)

  # Get the manifest path from config
  manifest_path = job['config']['transferSpec']['transferManifest']['location']

  # Upload manifest to GCS
  if job['local_manifest_path'] && File.exist?(job['local_manifest_path'])
    upload_pre_created_manifest(common, job['local_manifest_path'], manifest_path, impersonate_args)
  else
    create_and_upload_manifest_on_fly(common, job, manifest_path, impersonate_args)
  end

  # Execute transfer job and cleanup
  execute_transfer_and_cleanup(common, project, job['config'], manifest_path, impersonate_args)
end

# Helper to process AW4 Microarray sources
def process_aw4_microarray_sources(input_manifest, opts, tier, common, copy_manifests, output_manifests, output_manifest_path)
  aw4_microarray_sources = input_manifest["aw4MicroarraySources"]
  return if aw4_microarray_sources.nil? || aw4_microarray_sources.empty?

  if opts.microarray_rids_file.to_s.empty?
    raise ArgumentError.new("--microarray-rids-file is required to generate copy manifests for AW4 microarray sources")
  end

  microarray_aw4_rows = read_all_microarray_aw4s(opts.project, read_research_ids_file(opts.microarray_rids_file))

  aw4_microarray_sources.each do |source_name, section|
    common.status("building VWB manifest for '#{source_name}'")
    copy, output = build_vwb_copy_manifest_for_aw4_section(
      section, tier[:dest_cdr_bucket], opts.display_version_id, microarray_aw4_rows, output_manifest_path.call(source_name), opts.project)
    key_name = "aw4_microarray_" + source_name
    copy_manifests[key_name] = copy
    output_manifests[key_name] = output unless output.nil?
  end
end

# Helper to process AW4 WGS sources
def process_aw4_wgs_sources(input_manifest, opts, tier, common, copy_manifests, output_manifests, output_manifest_path)
  aw4_wgs_sources = input_manifest["aw4WgsSources"]
  return if aw4_wgs_sources.nil? || aw4_wgs_sources.empty?

  if opts.wgs_rids_file.to_s.empty?
    raise ArgumentError.new("--wgs-rids-file is required to generate copy manifests for AW4 WGS sources")
  end

  wgs_aw4_rows = read_all_wgs_aw4s(opts.project, read_research_ids_file(opts.wgs_rids_file))

  aw4_wgs_sources.each do |source_name, section|
    common.status("building VWB manifest for '#{source_name}'")
    copy, output = build_vwb_copy_manifest_for_aw4_section(
      section, tier[:dest_cdr_bucket], opts.display_version_id, wgs_aw4_rows, output_manifest_path.call(source_name), opts.project)
    key_name = "aw4_wgs_" + source_name
    copy_manifests[key_name] = copy
    output_manifests[key_name] = output unless output.nil?
  end
end

# Helper to process curation sources
def process_curation_sources(input_manifest, tier, opts, common, copy_manifests)
  curation_sources = input_manifest["curationSources"]
  return if curation_sources.nil? || curation_sources.empty?

  curation_sources.each do |source_name, section|
    common.status("building VWB manifest for '#{source_name}'")
    copy_manifests["curation_" + source_name] = build_vwb_copy_manifest_for_curation_section(
      section, tier[:dest_cdr_bucket], opts.display_version_id, opts.project)
  end
end

# Helper to write manifests to files
def write_manifests_to_files(copy_manifests, output_manifests, working_dir, output_manifest_path)
  copy_manifest_files = []

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

  return copy_manifest_files
end

# Helper to generate and save STS configurations
def generate_and_save_sts_configs(opts, working_dir, copy_manifest_files, common)
  common.status "Generating Storage Transfer Service job configurations..."

  # Get the environment configuration
  env = ENVIRONMENTS[opts.project]
  service_account = env.fetch(:publisher_account)
  publishing_project = env.fetch(:publishing_project, opts.project)

  # Generate STS job configs from the manifest files
  job_configs = generate_sts_job_configs(
    publishing_project,
    copy_manifest_files,
    working_dir,
    opts.jira_ticket,
    service_account
  )

  # Save the STS configs to files
  config_dir = save_sts_configs(working_dir, job_configs)
  common.status "STS job configurations saved to: #{config_dir}"

  # Count file list transfers for summary
  file_list_jobs = job_configs.select { |j| j["job_type"] == "file_list_transfer" }
  if file_list_jobs.length > 0
    sts_manifests_dir = File.join(working_dir, "sts_manifests")
    common.status "STS file manifests saved to: #{sts_manifests_dir}"
  end

  common.status "You can review and modify these configurations before running PUBLISH"
end

# Post-process VWB files to rename them if needed
# Helper method to create VWB copy manifests
def vwb_create_copy_manifests_mode(opts, tier, working_dir)
  common = Common.new
  common.status "Starting: VWB copy manifest creation"

  copy_manifests = {}
  output_manifests = {}
  output_manifest_path = -> (source_name) { "#{working_dir}/#{source_name}_output_manifest.csv" }

  input_manifest = parse_input_manifest(opts.input_manifest_file)

  # Process different source types
  process_aw4_microarray_sources(input_manifest, opts, tier, common, copy_manifests, output_manifests, output_manifest_path)
  process_aw4_wgs_sources(input_manifest, opts, tier, common, copy_manifests, output_manifests, output_manifest_path)
  process_curation_sources(input_manifest, tier, opts, common, copy_manifests)

  if copy_manifests.empty?
    raise ArgumentError.new("CREATE_COPY_MANIFESTS was requested, but no copy manifests were created, input manifest may be empty")
  end

  # Write manifests to files
  copy_manifest_files = write_manifests_to_files(copy_manifests, output_manifests, working_dir, output_manifest_path)

  common.status "Finished: VWB manifests created"

  # Generate and save STS configurations
  generate_and_save_sts_configs(opts, working_dir, copy_manifest_files, common)

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

# Helper to collect files that need renaming from manifest files
def collect_files_to_rename(manifest_files)
  files_to_rename = []

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
      end
    end
  end

  return files_to_rename
end

# Helper to display dry run summary for file renaming
def display_rename_dry_run(common, files_to_rename)
  total_files = files_to_rename.length

  common.status "="*60
  common.status "DRY RUN: Would rename #{total_files} file(s)"
  common.status "="*60
  common.status ""

  # Show sample of files to be renamed
  sample_size = [5, total_files].min
  common.status "Sample files to be renamed (showing #{sample_size} of #{total_files}):"

  files_to_rename.take(sample_size).each do |file|
    common.status "  #{file[:source_name]} -> #{file[:dest_name]}"
    common.status "    at: #{file[:destination_path]}"
  end

  if total_files > sample_size
    common.status "  ... and #{total_files - sample_size} more file(s)"
  end

  common.status ""
  common.status "DRY RUN COMPLETE: No files were renamed"
end

# Helper to perform actual file renaming
def perform_file_renaming(common, files_to_rename, deploy_account)
  total_files = files_to_rename.length
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
      common.status "[#{index + 1}/#{total_files}] Renaming: #{file[:source_name]} -> #{file[:dest_name]}"

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

  return successful_renames, failed_renames
end

def post_process_vwb_files(project, manifest_files, dry_run = false)
  common = Common.new

  # Get the publisher account for impersonation
  env = ENVIRONMENTS[project]
  deploy_account = env.fetch(:publisher_account)

  common.status dry_run ? "Starting VWB post-processing (DRY RUN)..." : "Starting VWB post-processing..."

  # Collect files that need renaming
  files_to_rename = collect_files_to_rename(manifest_files)
  total_files_to_rename = files_to_rename.length

  if total_files_to_rename == 0
    common.status "No files need renaming. Post-processing complete."
    return
  end

  if dry_run
    display_rename_dry_run(common, files_to_rename)
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

  # Perform the actual renaming
  successful_renames, failed_renames = perform_file_renaming(common, files_to_rename, deploy_account)

  # Display summary
  common.status ""
  common.status "="*60
  common.status "Post-processing complete:"
  common.status "  Successfully renamed: #{successful_renames} file(s)"
  common.status "  Failed to rename: #{failed_renames} file(s)" if failed_renames > 0
  common.status "="*60
end
