require "csv"
require "json"
require "tempfile"
require "net/http"
require "uri"
require "fileutils"
require "digest"
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

# Helper to read manifest from GCS or local file
def read_manifest_content_from_path(manifest_path, deploy_account, common, project = nil)
  if manifest_path.start_with?("gs://")
    args = project ? ["gsutil", "-u", project, "-i", deploy_account, "cat", manifest_path] : ["gsutil", "-i", deploy_account, "cat", manifest_path]
    common.capture_stdout(args)
  else
    IO.read(manifest_path)
  end
end

# Helper to read manifest from explicit path if provided
def read_manifest_from_explicit_path(delta_release_manifest_path, deploy_account, common)
  return "" if delta_release_manifest_path.nil?
  read_manifest_content_from_path(delta_release_manifest_path, deploy_account, common)
end

# Helper to construct and read manifest from delta release version
def read_manifest_from_delta_release(delta_release, dest_bucket, path_infix, project, deploy_account, common)
  return "" if delta_release.nil?

  manifest_path = "#{dest_bucket}/#{delta_release}/#{path_infix}/manifest.csv"
  read_manifest_content_from_path(manifest_path, deploy_account, common, project)
end

# Helper to validate that manifest was successfully read
def validate_manifest_read(prev_manifest, manifest_path, delta_release, delta_release_manifest_path)
  # Only validate if either delta_release or delta_release_manifest_path was provided
  return unless delta_release || delta_release_manifest_path
  return unless prev_manifest.empty?

  raise ArgumentError.new("failed to read previous manifest from #{manifest_path}, " +
                          "make sure to provide the correct previous release in the input manifest")
end

# Helper to convert manifest CSV to hash by person_id
def convert_manifest_to_hash(prev_manifest)
  return {} if prev_manifest.empty?

  prev_manifest_csv = CSV.parse(prev_manifest, headers: true)
  prev_manifest_hash = {}

  prev_manifest_csv.each do |manifest_row|
    rid = manifest_row["person_id"]
    prev_manifest_hash[rid] = manifest_row
  end

  prev_manifest_hash
end

# Helper to read previous manifest for delta releases
def _read_vwb_previous_manifest(project, dest_bucket, delta_release_manifest_path, delta_release, path_infix)
  common = Common.new
  env = ENVIRONMENTS[project]
  deploy_account = env.fetch(:publisher_account)

  # Try reading from explicit path first
  prev_manifest = read_manifest_from_explicit_path(delta_release_manifest_path, deploy_account, common)
  manifest_path = delta_release_manifest_path

  # If not found, try delta release version
  if prev_manifest.empty?
    prev_manifest = read_manifest_from_delta_release(delta_release, dest_bucket, path_infix, project, deploy_account, common)
    manifest_path = "#{dest_bucket}/#{delta_release}/#{path_infix}/manifest.csv" unless delta_release.nil?
  end

  # Validate that manifest was read successfully
  validate_manifest_read(prev_manifest, manifest_path, delta_release, delta_release_manifest_path)

  # Convert to hash
  convert_manifest_to_hash(prev_manifest)
end

# Helper to load previous manifest for AW4 delta releases
def load_aw4_previous_manifest(input_section, dest_bucket, project)
  return {} unless !input_section["deltaRelease"].nil? && !project.nil?

  _read_vwb_previous_manifest(
    project,
    dest_bucket,
    input_section["deltaReleaseManifestPath"],
    input_section["deltaRelease"],
    input_section["pooledDestPathInfix"]
  )
end

# Helper to check if RID exists in previous manifest
def rid_exists_in_previous_manifest?(rid, prev_manifest)
  !prev_manifest.empty? && prev_manifest.key?(rid)
end

# Helper to generate destination path for AW4 file
def generate_aw4_destination_path(source_path, input_section, rid, destination_base)
  source_name = File.basename(source_path)
  dest_name = apply_aw4_filename_replacement(source_name, input_section, rid)
  File.join(destination_base, dest_name)
end

# Helper to build output row for existing RID in delta release
def build_output_row_for_existing_rid(rid, aw4_row, input_section, destination_base)
  return nil unless input_section["outputManifestSpec"]

  output_row = {"person_id" => rid}

  input_section["outputManifestSpec"].each do |aw4_col, output_col|
    source_path = aw4_row[aw4_col]
    next if source_path.nil? || source_path.empty?

    destination_path = generate_aw4_destination_path(source_path, input_section, rid, destination_base)
    output_row[output_col] = destination_path
  end

  output_row
end

# Helper to process existing RID from previous manifest
def process_existing_rid_from_previous_manifest(rid, aw4_row, input_section, destination_base, output_rows_by_rid)
  output_row = build_output_row_for_existing_rid(rid, aw4_row, input_section, destination_base)
  output_rows_by_rid[rid] = output_row if output_row
end

# Helper to initialize output row for new RID
def initialize_output_row_if_needed(rid, input_section, output_rows_by_rid)
  return unless input_section["outputManifestSpec"]
  return if output_rows_by_rid.key?(rid)

  output_rows_by_rid[rid] = {"person_id" => rid}
end

# Helper to check if source path is valid
def valid_source_path?(source_path)
  !source_path.nil? && !source_path.empty?
end

# Helper to create copy manifest entry for AW4 file
def create_aw4_copy_manifest_entry(source_path, destination_path, dest_name, storage_class)
  {
    "source" => source_path,
    "destination" => destination_path,
    "outputFileName" => dest_name,
    "storageClass" => storage_class
  }
end

# Helper to map output column for AW4 file
def map_aw4_output_column(aw4_column, destination_path, input_section, rid, output_rows_by_rid)
  return unless input_section["outputManifestSpec"]

  input_section["outputManifestSpec"].each do |aw4_col, output_col|
    if aw4_col == aw4_column
      output_rows_by_rid[rid][output_col] = destination_path
    end
  end
end

# Helper to process single AW4 column
def process_aw4_column(aw4_column, aw4_row, rid, input_section, destination_base, copy_manifest, output_rows_by_rid)
  source_path = aw4_row[aw4_column]
  return unless valid_source_path?(source_path)

  # Build copy manifest entry
  source_name = File.basename(source_path)
  dest_name = apply_aw4_filename_replacement(source_name, input_section, rid)
  destination_path = File.join(destination_base, dest_name)
  storage_class = input_section.fetch("storageClass", "STANDARD")

  manifest_entry = create_aw4_copy_manifest_entry(source_path, destination_path, dest_name, storage_class)
  copy_manifest.push(manifest_entry)

  # Map to output manifest if specified
  map_aw4_output_column(aw4_column, destination_path, input_section, rid, output_rows_by_rid)
end

# Helper to process all AW4 columns for a single row
def process_aw4_columns_for_row(aw4_row, rid, input_section, destination_base, copy_manifest, output_rows_by_rid)
  input_section["aw4Columns"].each do |aw4_column|
    process_aw4_column(aw4_column, aw4_row, rid, input_section, destination_base, copy_manifest, output_rows_by_rid)
  end
end

# Helper to process single AW4 row
def process_single_aw4_row(aw4_row, input_section, destination_base, prev_manifest, copy_manifest, output_rows_by_rid)
  rid = aw4_row["research_id"]

  # Check if RID exists in previous manifest
  if rid_exists_in_previous_manifest?(rid, prev_manifest)
    process_existing_rid_from_previous_manifest(rid, aw4_row, input_section, destination_base, output_rows_by_rid)
    return # Skip copy manifest generation for existing research_ids
  end

  # Initialize output row for new RID
  initialize_output_row_if_needed(rid, input_section, output_rows_by_rid)

  # Process all columns
  process_aw4_columns_for_row(aw4_row, rid, input_section, destination_base, copy_manifest, output_rows_by_rid)
end

# Helper to convert output rows hash to array
def convert_output_rows_to_array(output_rows_by_rid)
  return [] if output_rows_by_rid.empty?
  output_rows_by_rid.values
end

# Build VWB copy manifest for AW4 sections (Microarray and WGS)
def build_vwb_copy_manifest_for_aw4_section(input_section, dest_bucket, display_version_id, aw4_rows, project = nil)
  # Calculate destination base path
  path_prefix = _get_vwb_pooled_path(input_section['pooledDestPathInfix'], display_version_id, input_section["deltaRelease"])
  destination_base = File.join(dest_bucket, path_prefix)

  # Load previous manifest for delta releases
  prev_manifest = load_aw4_previous_manifest(input_section, dest_bucket, project)

  # Initialize manifests
  copy_manifest = []
  output_rows_by_rid = {}

  # Process each AW4 row
  aw4_rows.each do |aw4_row|
    process_single_aw4_row(aw4_row, input_section, destination_base, prev_manifest, copy_manifest, output_rows_by_rid)
  end

  # Convert output rows to array
  output_manifest = convert_output_rows_to_array(output_rows_by_rid)

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

# Helper to check if this is a delta release
def is_delta_release?(input_section)
  !input_section["deltaRelease"].nil?
end

# Helper to determine destination base for curation section
def determine_curation_destination_base(input_section, dest_bucket, display_version_id)
  if is_delta_release?(input_section)
    path_infix = input_section['destination']
    path_prefix = _get_vwb_pooled_path(path_infix, display_version_id, input_section["deltaRelease"])
    File.join(dest_bucket, path_prefix)
  else
    determine_destination_base(input_section, dest_bucket, display_version_id)
  end
end

# Helper to get and validate source URIs
def get_source_uris_from_pattern(source_pattern, common)
  source_uris = common.capture_stdout(["gsutil", "ls", "-d", source_pattern]).split("\n")

  if source_uris.empty?
    raise ArgumentError.new("sourcePattern '#{source_pattern}' did not match any files")
  end

  source_uris.reject!(&:empty?)
  source_uris
end

# Helper to read previous manifest content for curation
def read_previous_curation_manifest(input_section, dest_bucket, project, common)
  prev_manifest_path = input_section["deltaReleaseManifestPath"]
  delta_release = input_section["deltaRelease"]

  env = ENVIRONMENTS[project]
  deploy_account = env.fetch(:publisher_account)

  if !prev_manifest_path.nil? && prev_manifest_path.start_with?("gs://")
    common.capture_stdout(["gsutil", "-i", deploy_account, "cat", prev_manifest_path])
  elsif !delta_release.nil?
    manifest_path = "#{dest_bucket}/#{delta_release}/#{input_section['destination']}/manifest.csv"
    common.capture_stdout(["gsutil", "-u", project, "-i", deploy_account, "cat", manifest_path])
  else
    ""
  end
end

# Helper to parse previous files from manifest content
def parse_previous_files_from_manifest(prev_manifest_content)
  prev_files = {}
  return prev_files if prev_manifest_content.nil? || prev_manifest_content.empty?

  CSV.parse(prev_manifest_content, headers: true).each do |row|
    prev_files[row["source"]] = true if row["source"]
  end

  prev_files
end

# Helper to load previous files for delta release
def load_previous_files_for_delta_release(input_section, dest_bucket, project, common)
  prev_files = {}
  return prev_files unless is_delta_release?(input_section) && !project.nil?

  begin
    prev_manifest_content = read_previous_curation_manifest(input_section, dest_bucket, project, common)
    prev_files = parse_previous_files_from_manifest(prev_manifest_content)
  rescue => e
    common.warning "Could not read previous manifest for delta release: #{e.message}"
  end

  prev_files
end

# Helper to check if source should be skipped
def should_skip_source?(source_path, prev_files)
  !prev_files.empty? && prev_files.key?(source_path)
end

# Helper to process single source URI
def process_source_uri(source_path, destination_base, storage_class, input_section)
  if source_path.end_with?("/")
    process_folder_source(source_path, destination_base, storage_class)
  else
    process_file_source(source_path, destination_base, storage_class, input_section)
  end
end

# Helper to build copy manifest from source URIs
def build_copy_manifest_from_sources(source_uris, prev_files, destination_base, storage_class, input_section)
  copy_manifest = []

  source_uris.each do |source_path|
    next if should_skip_source?(source_path, prev_files)

    manifest_entry = process_source_uri(source_path, destination_base, storage_class, input_section)
    copy_manifest.push(manifest_entry)
  end

  copy_manifest
end

# Build VWB copy manifest for curation sections
def build_vwb_copy_manifest_for_curation_section(input_section, dest_bucket, display_version_id, project = nil)
  common = Common.new

  # Determine destination base path
  destination_base = determine_curation_destination_base(input_section, dest_bucket, display_version_id)

  # Get source files matching the pattern
  source_uris = get_source_uris_from_pattern(input_section["sourcePattern"], common)

  # Get storage class
  storage_class = input_section.fetch("storageClass", "STANDARD")

  # Load previous files for delta releases
  prev_files = load_previous_files_for_delta_release(input_section, dest_bucket, project, common)

  # Build and return copy manifest
  build_copy_manifest_from_sources(source_uris, prev_files, destination_base, storage_class, input_section)
end

# Helper method for VWB filename replacement
def _apply_vwb_filename_replacement(source_name, match_pattern, replace_pattern, rid)
  return source_name if replace_pattern.nil?

  dest_name = source_name
  begin
    # Replace {RID} placeholder with actual research ID if present
    actual_replace = replace_pattern.dup
    actual_replace.gsub!("{RID}", rid) if rid

    # If no match pattern is specified, match the entire filename once
    # Use .+ instead of .* to avoid matching empty strings
    effective_match_pattern = match_pattern.nil? ? ".+" : match_pattern

    # Apply regex replacement (use sub for single replacement when no pattern specified)
    if match_pattern.nil?
      dest_name = source_name.sub(Regexp.new(effective_match_pattern), actual_replace)
    else
      dest_name = source_name.gsub(Regexp.new(effective_match_pattern), actual_replace)
    end
  rescue => e
    Common.new.warning "Failed to apply filename replacement: #{e.message}"
  end

  return dest_name
end

# Helper to get AW4 cache directory
def get_aw4_cache_dir(working_dir)
  cache_dir = File.join(working_dir, "aw4_cache")
  FileUtils.mkdir_p(cache_dir) unless File.directory?(cache_dir)
  return cache_dir
end

# Helper to generate cache key from AW4 prefix and RIDs
def generate_aw4_cache_key(aw4_prefix, rids)
  # Create a hash from the prefix and sorted RIDs to ensure consistent cache keys
  prefix_hash = Digest::SHA256.hexdigest(aw4_prefix)[0..15]
  rids_hash = Digest::SHA256.hexdigest(rids.sort.join(","))[0..15]
  return "aw4_#{prefix_hash}_rids_#{rids_hash}"
end

# Helper to read cached AW4 rows
def read_cached_aw4_rows(cache_dir, cache_key, common)
  cache_file = File.join(cache_dir, "#{cache_key}.json")

  unless File.exist?(cache_file)
    return nil
  end

  begin
    common.status "Reading AW4 rows from cache: #{File.basename(cache_file)}"
    cached_data = JSON.parse(File.read(cache_file))

    # Convert back to CSV::Row objects
    cached_rows = cached_data.map do |row_hash|
      CSV::Row.new(row_hash.keys, row_hash.values)
    end

    common.status "Loaded #{cached_rows.length} AW4 rows from cache"
    return cached_rows
  rescue => e
    common.warning "Failed to read cache file: #{e.message}"
    return nil
  end
end

# Helper to write AW4 rows to cache
def write_aw4_rows_to_cache(cache_dir, cache_key, aw4_rows, common)
  cache_file = File.join(cache_dir, "#{cache_key}.json")

  begin
    # Convert CSV::Row objects to hashes for JSON serialization
    rows_as_hashes = aw4_rows.map(&:to_h)

    File.open(cache_file, 'w') do |f|
      f.write(JSON.pretty_generate(rows_as_hashes))
    end

    common.status "Cached #{aw4_rows.length} AW4 rows to: #{File.basename(cache_file)}"
  rescue => e
    common.warning "Failed to write cache file: #{e.message}"
  end
end

# Helper to clear AW4 cache
def clear_aw4_cache(working_dir, common)
  cache_dir = get_aw4_cache_dir(working_dir)

  if File.directory?(cache_dir)
    cache_files = Dir.glob(File.join(cache_dir, "aw4_*.json"))

    if cache_files.empty?
      common.status "No AW4 cache files to clear"
    else
      cache_files.each { |f| File.delete(f) }
      common.status "Cleared #{cache_files.length} AW4 cache file(s)"
    end
  else
    common.status "No AW4 cache directory found"
  end
end

# Cached version of read_all_microarray_aw4s
def read_all_microarray_aw4s_cached(project, rids, working_dir, clear_cache = false)
  common = Common.new
  cache_dir = get_aw4_cache_dir(working_dir)

  # Get AW4 prefix from genomic_manifests
  require_relative 'genomic_manifests'
  raise ArgumentError.new("manifest generation is unconfigured for #{project}") unless FILE_ENVIRONMENTS.key? project
  aw4_prefix = FILE_ENVIRONMENTS[project][:source_config][:microarray_aw4_prefix]

  # Generate cache key
  cache_key = generate_aw4_cache_key(aw4_prefix, rids)

  # Try to read from cache unless clearing
  unless clear_cache
    cached_rows = read_cached_aw4_rows(cache_dir, cache_key, common)
    return cached_rows if cached_rows
  end

  # Cache miss or clear requested - read from source
  common.status "Reading microarray AW4 manifests from source..."
  aw4_rows = read_all_microarray_aw4s(project, rids)

  # Write to cache
  write_aw4_rows_to_cache(cache_dir, cache_key, aw4_rows, common)

  return aw4_rows
end

# Cached version of read_all_wgs_aw4s
def read_all_wgs_aw4s_cached(project, rids, working_dir, clear_cache = false)
  common = Common.new
  cache_dir = get_aw4_cache_dir(working_dir)

  # Get AW4 prefix from genomic_manifests
  require_relative 'genomic_manifests'
  raise ArgumentError.new("manifest generation is unconfigured for #{project}") unless FILE_ENVIRONMENTS.key? project
  aw4_prefix = FILE_ENVIRONMENTS[project][:source_config][:wgs_aw4_prefix]

  # Generate cache key
  cache_key = generate_aw4_cache_key(aw4_prefix, rids)

  # Try to read from cache unless clearing
  unless clear_cache
    cached_rows = read_cached_aw4_rows(cache_dir, cache_key, common)
    return cached_rows if cached_rows
  end

  # Cache miss or clear requested - read from source
  common.status "Reading WGS AW4 manifests from source..."
  aw4_rows = read_all_wgs_aw4s(project, rids)

  # Write to cache
  write_aw4_rows_to_cache(cache_dir, cache_key, aw4_rows, common)

  return aw4_rows
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

  # Group file transfers by source bucket, source folder, storage class and destination directory
  grouped_file_transfers = {}
  file_transfers.each do |transfer|
    # Extract source bucket from the GCS path
    source_bucket = transfer[:source].gsub("gs://", "").split("/")[0]
    # Extract source folder (directory containing the file)
    source_folder = File.dirname(transfer[:source].gsub(/^gs:\/\/[^\/]+\//, ""))
    dest_dir = File.dirname(transfer[:destination])
    storage_class = transfer[:storage_class]

    # Create a key based on source bucket, source folder, destination directory and storage class
    key = "#{source_bucket}_#{source_folder}_#{dest_dir}_#{storage_class}"

    if grouped_file_transfers[key].nil?
      grouped_file_transfers[key] = {
        :files => [],
        :source_bucket => source_bucket,
        :source_folder => source_folder,
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

# Helper function to get OAuth token for the current user or service account
def get_token(service_account = nil)
  common = Common.new

  # Build the gcloud command based on whether we have a service account
  if service_account
    # Get access token for the service account via impersonation
    token = common.capture_stdout([
                                    "gcloud", "auth", "print-access-token",
                                    "--impersonate-service-account", service_account
                                  ]).strip
  else
    # Use gcloud to get an access token for the current logged-in user
    token = common.capture_stdout([
                                    "gcloud", "auth", "print-access-token"
                                  ]).strip
  end

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
def process_file_list_transfers(configs, project, jira_ticket, service_account, job_index, sts_manifests_dir, common, use_v2 = false)
  job_configs = []

  configs[:file_transfer_groups].each do |file_group|
    next if file_group[:files].empty?

    job_name = generate_job_name("vwb-files-transfer", job_index, jira_ticket)

    # Create the manifest file locally for review
    local_manifest_path = create_local_manifest_file(file_group, job_name, sts_manifests_dir, common, use_v2)

    config = generate_file_list_transfer_config(
      project,
      file_group,
      job_name,
      service_account,
      use_v2
    )

    job_configs.push({
                       "job_name" => job_name,
                       "job_type" => "file_list_transfer",
                       "file_group" => file_group,
                       "config" => config,
                       "local_manifest_path" => local_manifest_path,
                       "use_v2" => use_v2
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
def create_local_manifest_file(file_group, job_name, sts_manifests_dir, common, use_v2 = false)
  manifest_filename = "#{job_name}-manifest.txt"
  local_manifest_path = File.join(sts_manifests_dir, manifest_filename)

  # Write source file paths to manifest
  File.open(local_manifest_path, 'w') do |f|
    file_group[:files].each do |file|
      # Extract object path from full GCS URL
      object_path = file[:source].gsub(/^gs:\/\/[^\/]+\//, "")

      # In V2 mode, paths in manifest should be relative to the source folder
      # because gcsDataSource.path is set to the source folder
      if use_v2 && file_group[:source_folder]
        source_folder = file_group[:source_folder]
        # Remove the source folder prefix to get relative path
        source_folder_with_slash = source_folder.end_with?("/") ? source_folder : "#{source_folder}/"
        if object_path.start_with?(source_folder_with_slash)
          object_path = object_path.sub(source_folder_with_slash, "")
        elsif object_path.start_with?(source_folder)
          object_path = object_path.sub("#{source_folder}/", "")
        end
      end

      f.puts(object_path)
    end
  end

  common.status "Created STS manifest: #{manifest_filename} (#{file_group[:files].length} files)"
  return local_manifest_path
end

# Generate Storage Transfer Service job configurations from manifest files
def generate_sts_job_configs(project, manifest_files, working_dir, jira_ticket = nil, service_account = nil, use_v2 = false)
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

  # Process file list transfers with v2 flag
  file_configs, _ = process_file_list_transfers(configs, project, jira_ticket, service_account, job_index, sts_manifests_dir, common, use_v2)
  job_configs.concat(file_configs)

  common.status "Created #{configs[:file_transfer_groups].count { |g| !g[:files].empty? }} STS manifest files in #{sts_manifests_dir}"
  return job_configs
end

# Helper to ensure path ends with slash for folder transfers
def ensure_path_ends_with_slash(path)
  path.end_with?("/") ? path : "#{path}/"
end

# Helper to build gcsDataSink for folder transfer
def build_gcs_data_sink_for_folder(dest_bucket, dest_path_with_slash, storage_class)
  gcs_data_sink = {
    "bucketName" => dest_bucket,
    "path" => dest_path_with_slash
  }

  # Add object conditions for storage class if not STANDARD
  if storage_class && storage_class != "STANDARD"
    gcs_data_sink["objectConditions"] = {
      "minTimeElapsedSinceLastModification" => "0s"
    }
  end

  gcs_data_sink
end

# Helper to build transfer options for folder transfer
def build_transfer_options_for_folder(storage_class)
  transfer_options = {
    "overwriteObjectsAlreadyExistingInSink" => false
  }

  # Add metadata options for storage class if not STANDARD
  if storage_class && storage_class != "STANDARD"
    transfer_options["metadataOptions"] = {
      "storageClass" => "STORAGE_CLASS_#{storage_class}"
    }
  end

  transfer_options
end

# Generate folder transfer configuration (without creating the job)
def generate_folder_transfer_config(project, source, destination, storage_class, job_name, service_account = nil)
  # Extract bucket and path components
  source_bucket = source.gsub("gs://", "").split("/")[0]
  source_path = source.gsub(/^gs:\/\/[^\/]+\//, "")

  dest_bucket = destination.gsub("gs://", "").split("/")[0]
  dest_path = destination.gsub(/^gs:\/\/[^\/]+\//, "")

  # Ensure paths end with / for folder transfers
  source_path_with_slash = ensure_path_ends_with_slash(source_path)
  dest_path_with_slash = ensure_path_ends_with_slash(dest_path)

  # Build config components
  gcs_data_sink = build_gcs_data_sink_for_folder(dest_bucket, dest_path_with_slash, storage_class)
  transfer_options = build_transfer_options_for_folder(storage_class)

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
      "gcsDataSink" => gcs_data_sink,
      "transferOptions" => transfer_options
    },
    "projectId" => project
  }

  # Add the service account at the root level if provided
  add_service_account_to_job_config(job_config, service_account)
end

# Helper to build gcsDataSource for file list transfer
def build_gcs_data_source_for_file_list(source_bucket, file_group, use_v2)
  gcs_data_source = {
    "bucketName" => source_bucket
  }

  # V2: Add source folder path to gcsDataSource so STS knows which folder to read from
  # V1: No path specified, STS reads files from their full paths
  if use_v2 && file_group[:source_folder]
    source_folder = file_group[:source_folder]
    source_folder_with_slash = source_folder.end_with?("/") ? source_folder : "#{source_folder}/"
    gcs_data_source["path"] = source_folder_with_slash
  end

  gcs_data_source
end

# Helper to build gcsDataSink for file list transfer
def build_gcs_data_sink_for_file_list(dest_bucket, first_file, use_v2)
  gcs_data_sink = {
    "bucketName" => dest_bucket
  }

  # V2: Add destination path to gcsDataSink so files go to correct directory
  # V1: No path specified, STS preserves source directory structure
  if use_v2
    dest_path = File.dirname(first_file[:destination].gsub(/^gs:\/\/[^\/]+\//, ""))
    dest_path_with_slash = dest_path.end_with?("/") ? dest_path : "#{dest_path}/"
    gcs_data_sink["path"] = dest_path_with_slash
  end

  gcs_data_sink
end

# Helper to build transfer options with storage class
def build_transfer_options_with_storage_class(storage_class)
  transfer_options = {
    "overwriteObjectsAlreadyExistingInSink" => false
  }

  # Add metadata options for storage class if not STANDARD
  if storage_class && storage_class != "STANDARD"
    transfer_options["metadataOptions"] = {
      "storageClass" => "STORAGE_CLASS_#{storage_class}"
    }
  end

  transfer_options
end

# Helper to add service account to job config
def add_service_account_to_job_config(job_config, service_account)
  return job_config unless service_account
  job_config["serviceAccount"] = service_account
  job_config
end

# Generate file list transfer configuration (without creating the job)
def generate_file_list_transfer_config(project, file_group, job_name, service_account = nil, use_v2 = false)
  # Get source and destination buckets
  first_file = file_group[:files].first
  source_bucket = first_file[:source].gsub("gs://", "").split("/")[0]
  dest_bucket = first_file[:destination].gsub("gs://", "").split("/")[0]

  # The manifest path will be created during publish
  temp_manifest_path = "gs://#{dest_bucket}/temp-manifests/#{job_name}-manifest.txt"

  # Build config components
  gcs_data_source = build_gcs_data_source_for_file_list(source_bucket, file_group, use_v2)
  gcs_data_sink = build_gcs_data_sink_for_file_list(dest_bucket, first_file, use_v2)
  transfer_options = build_transfer_options_with_storage_class(file_group[:storage_class])

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
      "gcsDataSource" => gcs_data_source,
      "gcsDataSink" => gcs_data_sink,
      "transferManifest" => {
        "location" => temp_manifest_path
      },
      "transferOptions" => transfer_options
    },
    "projectId" => project
  }

  # Add the service account at the root level if provided
  add_service_account_to_job_config(job_config, service_account)
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

    # Display the full job configuration JSON
    common.status "\nJob Configuration JSON:"
    common.status JSON.pretty_generate(job['config'])
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

# Helper to validate microarray RIDs file is provided
def validate_microarray_rids_file(opts)
  return unless opts.microarray_rids_file.to_s.empty?
  raise ArgumentError.new("--microarray-rids-file is required to generate copy manifests for AW4 microarray sources")
end

# Helper to load microarray AW4 rows
def load_microarray_aw4_rows(opts, working_dir)
  clear_cache = determine_cache_clearing_flag(opts)
  read_all_microarray_aw4s_cached(opts.project, read_research_ids_file(opts.microarray_rids_file), working_dir, clear_cache)
end

# Helper to process single microarray source
def process_single_microarray_source(source_name, section, tier, opts, microarray_aw4_rows, output_manifest_path, common, copy_manifests, output_manifests)
  common.status("building VWB manifest for '#{source_name}'")
  copy, output = build_vwb_copy_manifest_for_aw4_section(
    section, tier[:dest_cdr_bucket], opts.display_version_id, microarray_aw4_rows, output_manifest_path.call(source_name), opts.project)

  key_name = "aw4_microarray_" + source_name
  store_copy_manifest_if_present(copy_manifests, key_name, copy)
  store_copy_manifest_if_present(output_manifests, key_name, output)
end

# Helper to process all microarray sources
def process_all_microarray_sources(aw4_microarray_sources, tier, opts, microarray_aw4_rows, output_manifest_path, common, copy_manifests, output_manifests)
  aw4_microarray_sources.each do |source_name, section|
    process_single_microarray_source(source_name, section, tier, opts, microarray_aw4_rows, output_manifest_path, common, copy_manifests, output_manifests)
  end
end

# Helper to process AW4 Microarray sources
def process_aw4_microarray_sources(input_manifest, opts, tier, common, copy_manifests, output_manifests, output_manifest_path, working_dir)
  aw4_microarray_sources = input_manifest["aw4MicroarraySources"]
  return if aw4_microarray_sources.nil? || aw4_microarray_sources.empty?

  validate_microarray_rids_file(opts)
  microarray_aw4_rows = load_microarray_aw4_rows(opts, working_dir)
  process_all_microarray_sources(aw4_microarray_sources, tier, opts, microarray_aw4_rows, output_manifest_path, common, copy_manifests, output_manifests)
end

# Helper to determine if cache should be cleared
def determine_cache_clearing_flag(opts)
  opts.respond_to?(:clear_cache) && opts.clear_cache
end

# Helper to merge a single output row into combined manifest
def merge_output_row_into_combined(combined_output_rows, row)
  rid = row["person_id"]

  if combined_output_rows[rid].nil?
    combined_output_rows[rid] = row
  else
    merge_row_columns(combined_output_rows[rid], row)
  end
end

# Helper to merge columns from one row into another
def merge_row_columns(existing_row, new_row)
  new_row.each do |k, v|
    existing_row[k] = v unless k == "person_id"
  end
end

# Helper to store copy manifest if not empty
def store_copy_manifest_if_present(copy_manifests, key_name, copy)
  return if copy.nil? || copy.empty?
  copy_manifests[key_name] = copy
end

# Helper to merge output rows into combined manifest
def merge_output_rows(combined_output_rows, output)
  return if output.nil? || output.empty?

  output.each do |row|
    merge_output_row_into_combined(combined_output_rows, row)
  end
end

# Helper to process a single WGS source
def process_single_wgs_source(source_name, section, tier, opts, wgs_aw4_rows, output_manifest_path, common, copy_manifests, combined_output_rows)
  common.status("building VWB manifest for '#{source_name}'")
  copy, output = build_vwb_copy_manifest_for_aw4_section(
    section, tier[:dest_cdr_bucket], opts.display_version_id, wgs_aw4_rows, output_manifest_path.call(source_name), opts.project)

  key_name = "aw4_wgs_" + source_name
  store_copy_manifest_if_present(copy_manifests, key_name, copy)
  merge_output_rows(combined_output_rows, output)
end

# Helper to store combined output manifest if not empty
def store_combined_output_manifest(output_manifests, combined_output_rows)
  combined_output = combined_output_rows.values
  return if combined_output.empty?
  output_manifests["aw4_wgs_combined"] = combined_output
end

# Helper to validate WGS RIDs file is provided
def validate_wgs_rids_file(opts)
  return unless opts.wgs_rids_file.to_s.empty?
  raise ArgumentError.new("--wgs-rids-file is required to generate copy manifests for AW4 WGS sources")
end

# Helper to process AW4 WGS sources with combined output manifest
def process_aw4_wgs_sources(input_manifest, opts, tier, common, copy_manifests, output_manifests, output_manifest_path, working_dir)
  aw4_wgs_sources = input_manifest["aw4WgsSources"]
  return if aw4_wgs_sources.nil? || aw4_wgs_sources.empty?

  validate_wgs_rids_file(opts)

  clear_cache = determine_cache_clearing_flag(opts)
  wgs_aw4_rows = read_all_wgs_aw4s_cached(opts.project, read_research_ids_file(opts.wgs_rids_file), working_dir, clear_cache)

  combined_output_rows = {}

  aw4_wgs_sources.each do |source_name, section|
    process_single_wgs_source(source_name, section, tier, opts, wgs_aw4_rows, output_manifest_path, common, copy_manifests, combined_output_rows)
  end

  store_combined_output_manifest(output_manifests, combined_output_rows)
end

# Helper to process curation sources
def process_curation_sources(input_manifest, tier, opts, common, copy_manifests)
  curation_sources = input_manifest["curationSources"]
  return if curation_sources.nil? || curation_sources.empty?

  curation_sources.each do |source_name, section|
    common.status("building VWB manifest for '#{source_name}'")
    copy = build_vwb_copy_manifest_for_curation_section(
      section, tier[:dest_cdr_bucket], opts.display_version_id, opts.project)
    copy_manifests["curation_" + source_name] = copy unless copy.nil? || copy.empty?
  end
end

# Helper to check if manifest is empty
def manifest_empty?(manifest)
  manifest.nil? || manifest.empty?
end

# Helper to write manifest rows to CSV file
def write_manifest_rows_to_csv(path, manifest)
  CSV.open(path, 'wb') do |f|
    f << manifest.first.keys
    manifest.each { |c| f << c.values }
  end
end

# Helper to write single copy manifest to file
def write_single_copy_manifest(source_name, copy_manifest, working_dir, copy_manifest_files)
  return if manifest_empty?(copy_manifest)

  path = "#{working_dir}/#{source_name}_copy_manifest.csv"
  write_manifest_rows_to_csv(path, copy_manifest)
  copy_manifest_files.push(path)
end

# Helper to write all copy manifests to files
def write_copy_manifests_to_files(copy_manifests, working_dir)
  copy_manifest_files = []

  copy_manifests.each do |source_name, copy_manifest|
    write_single_copy_manifest(source_name, copy_manifest, working_dir, copy_manifest_files)
  end

  copy_manifest_files
end

# Helper to write single output manifest to file
def write_single_output_manifest(source_name, output_manifest, output_manifest_path)
  return if manifest_empty?(output_manifest)

  path = output_manifest_path.call(source_name)
  write_manifest_rows_to_csv(path, output_manifest)
end

# Helper to write all output manifests to files
def write_output_manifests_to_files(output_manifests, output_manifest_path)
  output_manifests.each do |source_name, output_manifest|
    write_single_output_manifest(source_name, output_manifest, output_manifest_path)
  end
end

# Helper to write manifests to files
def write_manifests_to_files(copy_manifests, output_manifests, working_dir, output_manifest_path)
  # Write copy manifests and collect file paths
  copy_manifest_files = write_copy_manifests_to_files(copy_manifests, working_dir)

  # Write output manifests
  write_output_manifests_to_files(output_manifests, output_manifest_path)

  copy_manifest_files
end

# Helper to generate and save STS configurations
def generate_and_save_sts_configs(opts, working_dir, copy_manifest_files, common, use_v2 = false)
  common.status "Generating Storage Transfer Service job configurations..."

  # Get the environment configuration
  env = ENVIRONMENTS[opts.project]
  service_account = env.fetch(:publisher_account)
  publishing_project = env.fetch(:publishing_project, opts.project)

  # Generate STS job configs from the manifest files with v2 flag
  job_configs = generate_sts_job_configs(
    publishing_project,
    copy_manifest_files,
    working_dir,
    opts.jira_ticket,
    service_account,
    use_v2
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

  # Handle --clear-cache flag if specified
  if opts.respond_to?(:clear_cache) && opts.clear_cache
    common.status "Clearing AW4 cache..."
    clear_aw4_cache(working_dir, common)
  end

  # Process different source types
  process_aw4_microarray_sources(input_manifest, opts, tier, common, copy_manifests, output_manifests, output_manifest_path, working_dir)
  process_aw4_wgs_sources(input_manifest, opts, tier, common, copy_manifests, output_manifests, output_manifest_path, working_dir)
  process_curation_sources(input_manifest, tier, opts, common, copy_manifests)

  if copy_manifests.empty?
    raise ArgumentError.new("CREATE_COPY_MANIFESTS was requested, but no copy manifests were created, input manifest may be empty")
  end

  # Write manifests to files
  copy_manifest_files = write_manifests_to_files(copy_manifests, output_manifests, working_dir, output_manifest_path)

  common.status "Finished: VWB manifests created"

  # Get use_v2 flag from opts (defaults to false if not set)
  use_v2 = opts.respond_to?(:use_v2) && opts.use_v2 ? true : false

  # Generate and save STS configurations with v2 flag
  generate_and_save_sts_configs(opts, working_dir, copy_manifest_files, common, use_v2)

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

# Helper to get starting status message for post-processing
def get_post_process_start_message(dry_run)
  dry_run ? "Starting: VWB post-processing phase (DRY RUN)" : "Starting: VWB post-processing phase"
end

# Helper to get finishing status message for post-processing
def get_post_process_finish_message(dry_run)
  dry_run ? "Finished: VWB post-processing dry run" : "Finished: VWB post-processing"
end

# Helper to ensure manifest files exist
def ensure_manifest_files_exist(copy_manifest_files, working_dir)
  return copy_manifest_files unless copy_manifest_files.empty?

  found_files = Dir.glob(working_dir + "/*_copy_manifest.csv")
  if found_files.empty?
    raise ArgumentError.new(
      "no copy manifests found in the working dir #{working_dir}; " +
      "POST_PROCESS requires manifest files from CREATE_COPY_MANIFESTS")
  end

  found_files
end

# Helper to check if v2 mode is enabled via command line
def check_v2_from_command_line(opts)
  opts.respond_to?(:use_v2) && opts.use_v2
end

# Helper to detect v2 mode from saved STS configs
def detect_v2_from_saved_configs(config_dir, common)
  return false unless File.directory?(config_dir)

  begin
    job_configs = load_sts_configs(config_dir)
    job_configs.any? { |job| job["use_v2"] == true }
  rescue => e
    common.warning "Could not load STS configs to detect V2 mode: #{e.message}"
    common.warning "Defaulting to V1 mode (move and rename)"
    false
  end
end

# Helper to log v2 mode detection result
def log_v2_mode_detection(use_v2, from_command_line, common)
  if from_command_line
    common.status "Using V2 mode from command line flag: Files are in destination directories, renaming in place"
  elsif use_v2
    common.status "Detected V2 mode from saved configs: Files are in destination directories, renaming in place"
  else
    common.status "Detected V1 mode from saved configs: Files preserve source structure, moving and renaming"
  end
end

# Helper to determine v2 mode from opts and configs
def determine_v2_mode(opts, working_dir, common)
  # Check command line flag first
  from_command_line = check_v2_from_command_line(opts)
  return true if from_command_line

  # Otherwise detect from saved configs
  config_dir = File.join(working_dir, "storage_transfer_configs")
  detect_v2_from_saved_configs(config_dir, common)
end

# Helper method to post-process VWB files
def vwb_post_process_mode(opts, copy_manifest_files, working_dir)
  common = Common.new
  common.status get_post_process_start_message(opts.dry_run)

  copy_manifest_files = ensure_manifest_files_exist(copy_manifest_files, working_dir)

  from_command_line = check_v2_from_command_line(opts)
  use_v2 = from_command_line || determine_v2_mode(opts, working_dir, common)
  log_v2_mode_detection(use_v2, from_command_line, common)

  post_process_vwb_files(opts.project, copy_manifest_files, opts.dry_run, use_v2)

  common.status get_post_process_finish_message(opts.dry_run)
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

      # Check if renaming or moving is needed
      # STS preserves the source path structure in the destination bucket
      # So we need to handle both rename and move operations
      files_to_rename.push({
                             :source_path => source,
                             :source_name => source_filename,
                             :dest_name => dest_filename,
                             :destination_path => destination
                           })
    end
  end

  return files_to_rename
end

# Helper to display dry run summary for file renaming
def display_rename_dry_run(common, files_to_rename, use_v2 = false)
  total_files = files_to_rename.length

  common.status "="*60
  common.status "DRY RUN: Would move/rename #{total_files} file(s)"
  common.status "="*60
  common.status ""

  # Show sample of files to be renamed
  sample_size = [5, total_files].min
  common.status "Sample files to be moved/renamed (showing #{sample_size} of #{total_files}):"

  files_to_rename.take(sample_size).each do |file|
    # Calculate where STS actually copied the file based on v1 or v2 behavior
    if use_v2
      # V2: Files are already in the correct destination directory with original names
      dest_bucket = file[:destination_path].gsub("gs://", "").split("/")[0]
      dest_path = File.dirname(file[:destination_path].gsub(/^gs:\/\/[^\/]+\//, ""))
      original_filename = file[:source_name]
      current_path = "gs://#{dest_bucket}/#{dest_path}/#{original_filename}"
    else
      # V1: STS preserves the source directory structure in the destination bucket
      dest_bucket = file[:destination_path].gsub("gs://", "").split("/")[0]
      source_object_path = file[:source_path].gsub(/^gs:\/\/[^\/]+\//, "")
      current_path = "gs://#{dest_bucket}/#{source_object_path}"
    end

    common.status "\n  File: #{file[:source_name]} -> #{file[:dest_name]}"
    common.status "    From: #{current_path}"
    common.status "    To:   #{file[:destination_path]}"
  end

  if total_files > sample_size
    common.status "\n  ... and #{total_files - sample_size} more file(s)"
  end

  common.status ""
  common.status "DRY RUN COMPLETE: No files were moved/renamed"
end

# Helper to execute file renaming with log files using block version
def execute_with_log_files(failure_log_path)
  if failure_log_path
    File.open(failure_log_path, 'w') do |failure_log|
      failure_log.puts("timestamp,source_path,destination_path,error_type,error_message")

      success_log_path = failure_log_path.sub('.csv', '_success.csv')
      File.open(success_log_path, 'w') do |success_log|
        success_log.puts("timestamp,source_path,destination_path")

        yield(failure_log, success_log)
      end
    end
  else
    yield(nil, nil)
  end
end

# Helper to calculate current file path based on v1/v2 mode
def calculate_current_file_path(file, use_v2)
  dest_bucket = file[:destination_path].gsub("gs://", "").split("/")[0]

  if use_v2
    # V2: Files are in destination directory with original names
    dest_path = File.dirname(file[:destination_path].gsub(/^gs:\/\/[^\/]+\//, ""))
    original_filename = file[:source_name]
    "gs://#{dest_bucket}/#{dest_path}/#{original_filename}"
  else
    # V1: STS preserves source directory structure
    source_object_path = file[:source_path].gsub(/^gs:\/\/[^\/]+\//, "")
    "gs://#{dest_bucket}/#{source_object_path}"
  end
end

# Helper to classify error type from error message
def classify_rename_error(error_msg)
  if error_msg.include?("NotFound") || error_msg.include?("No such") || error_msg.include?("404") || error_msg.include?("matched no objects")
    "NOT_FOUND"
  else
    "ERROR"
  end
end

# Helper to log error to console
def log_error_to_console(common, error_type, current_path, error_msg)
  if error_type == "NOT_FOUND"
    common.warning "File not found (skipping): #{current_path}"
  else
    common.error "Failed to move/rename #{current_path}: #{error_msg}"
  end
end

# Helper to log error to file
def log_error_to_file(failure_log, current_path, new_path, error_type, error_msg)
  return unless failure_log

  timestamp = Time.now.strftime("%Y-%m-%d %H:%M:%S")
  escaped_msg = error_msg.gsub('"', '""')
  failure_log.puts("\"#{timestamp}\",\"#{current_path}\",\"#{new_path}\",\"#{error_type}\",\"#{escaped_msg}\"")
  failure_log.flush
end

# Helper to handle rename failure
def handle_rename_failure(stdout, stderr, current_path, new_path, failure_log, common)
  error_msg = (stderr + stdout).strip
  error_type = classify_rename_error(error_msg)
  log_error_to_console(common, error_type, current_path, error_msg)
  log_error_to_file(failure_log, current_path, new_path, error_type, error_msg)
end

# Helper to execute file rename command
def execute_file_rename(current_path, new_path, deploy_account)
  require 'open3'
  cmd = ["gcloud", "storage", "mv", "--impersonate-service-account", deploy_account, current_path, new_path]
  Open3.capture3(*cmd)
end

# Helper to handle already-correct file
def handle_already_correct_file(mutex, processed_count, total_files, successful_renames, common, file)
  mutex.synchronize do
    processed_count[0] += 1
    successful_renames[0] += 1
    common.status "[#{processed_count[0]}/#{total_files}] Skipping (already correct): #{file[:dest_name]}"
  end
end

# Helper to log rename progress
def log_rename_progress(mutex, processed_count, total_files, common, current_path, new_path)
  mutex.synchronize do
    processed_count[0] += 1
    common.status "[#{processed_count[0]}/#{total_files}] Moving/renaming:"
    common.status "  From: #{current_path}"
    common.status "  To:   #{new_path}"
  end
end

# Helper to process single file rename
def process_single_file_rename(file, use_v2, deploy_account, mutex, processed_count, total_files, successful_renames, failed_renames, failure_log, common)
  current_path = calculate_current_file_path(file, use_v2)
  new_path = file[:destination_path]

  # Skip if already in correct location
  if current_path == new_path
    handle_already_correct_file(mutex, processed_count, total_files, successful_renames, common, file)
    return
  end

  begin
    log_rename_progress(mutex, processed_count, total_files, common, current_path, new_path)
    stdout, stderr, status = execute_file_rename(current_path, new_path, deploy_account)

    if status.success?
      mutex.synchronize { successful_renames[0] += 1 }
    else
      mutex.synchronize do
        handle_rename_failure(stdout, stderr, current_path, new_path, failure_log, common)
        failed_renames[0] += 1
      end
    end
  rescue => e
    mutex.synchronize do
      common.error "Unexpected error processing #{current_path}: #{e.message}"
      failed_renames[0] += 1
    end
  end
end

# Helper to create worker thread for file renaming
def create_rename_worker_thread(file_queue, use_v2, deploy_account, mutex, counters, failure_log, common)
  processed_count, successful_renames, failed_renames, total_files = counters

  Thread.new do
    loop do
      file = file_queue.pop(true) rescue break
      process_single_file_rename(file, use_v2, deploy_account, mutex, processed_count, total_files, successful_renames, failed_renames, failure_log, common)
    end
  end
end

# Helper to perform actual file renaming with parallel processing
def perform_file_renaming(common, files_to_rename, deploy_account, num_threads = 32, failure_log_path = nil, use_v2 = false)
  total_files = files_to_rename.length
  mutex = Mutex.new

  # Use arrays for thread-safe counters
  processed_count = [0]
  successful_renames = [0]
  failed_renames = [0]

  # Execute with log files using block version for automatic closing
  execute_with_log_files(failure_log_path) do |failure_log|
    # Create file queue
    file_queue = Queue.new
    files_to_rename.each { |file| file_queue.push(file) }

    # Create worker threads
    counters = [processed_count, successful_renames, failed_renames, total_files]
    threads = num_threads.times.map do
      create_rename_worker_thread(file_queue, use_v2, deploy_account, mutex, counters, failure_log, common)
    end

    # Wait for completion
    threads.each(&:join)

    # Log completion message if failure log was created
    if failure_log_path
      common.status "Failure log written to: #{failure_log_path}"
    end
  end

  return successful_renames[0], failed_renames[0]
end

def post_process_vwb_files(project, manifest_files, dry_run = false, use_v2 = false)
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
    display_rename_dry_run(common, files_to_rename, use_v2)
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

  # Create failure log path in the same directory as manifest files
  working_dir = File.dirname(manifest_files.first)
  failure_log_path = File.join(working_dir, "post_process_failures.csv")

  # Perform the actual renaming with failure logging and v2 flag
  successful_renames, failed_renames = perform_file_renaming(common, files_to_rename, deploy_account, 32, failure_log_path, use_v2)

  # Display summary
  common.status ""
  common.status "="*60
  common.status "Post-processing complete:"
  common.status "  Successfully renamed: #{successful_renames} file(s)"
  common.status "  Failed to rename: #{failed_renames} file(s)" if failed_renames > 0
  common.status "="*60
end
