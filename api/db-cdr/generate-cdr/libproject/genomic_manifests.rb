require "csv"
require "date"
require "fileutils"
require "yaml"
require_relative "../../../../aou-utils/utils/common"
require_relative "../../../libproject/environments"

# Utilities for generating, managing, publishing genomic data files. At a high
# level, this facilitiates the data flow:
#
#   Genome Centers / Broad genomic curation -> Researcher Workbench environment
#
# Inputs:
#
# Our primary inputs to this process are AW4 manifests. These are CSV files passed
# from the Broad genomic curation team to the DRC for data handoff. A single manifest
# corresponds to a batch of data which was processed, so you should expect to see many
# manifests. In some cases, data will also be processed multiple times, in which case
# the latest should be taken. Note that WGS and Microarray have different specs.
#
# In the Workbench, we are primarily using AW4 manifests as a lookup between research ID and
# curated data file path, though it also contains some logging metadata such as QC status and
# research inclusion. See the CDR playbook for more documentation on AW4s:
# https://docs.google.com/document/d/1St6pG_EUFB9oRQUQaOSO7a9UPxPkQ5n4qAVyKF9j9tk/edit#heading=h.xt7avgt1nsoh
#
# Intermediates:
#
# We generate one or more manifests of source/desintation GCS file paths, which
# describe how data will be copied.
#
# TODO(RW-8266): details on the publishing/copy process, details on manifest provenance
#
# See the CDR playbook for more context on the overall publishing process:
# https://docs.google.com/document/d/1St6pG_EUFB9oRQUQaOSO7a9UPxPkQ5n4qAVyKF9j9tk/edit#heading=h.xt7avgt1nsoh

CURATION_PROD_SOURCE_CONFIG = {
  # Optional: to speed up iteration with this script, download and run against a local directory instead.
  :wgs_aw4_prefix => "gs://prod-drc-broad/AW4_wgs_manifest/AoU_DRCB_SEQ_",
  # The array_old_egt_files should be removed, likely in late 2022. This contains older
  # AW4 manifests before reprocessing.
  :microarray_aw4_prefix => "gs://prod-drc-broad/array_old_egt_files/AW4_array_manifest/AoU_DRCB_GEN_"
}

FILE_ENVIRONMENTS = {
  "all-of-us-rw-preprod" => {
    :source_config => CURATION_PROD_SOURCE_CONFIG,
  },
  "all-of-us-rw-prod" => {
    :source_config => CURATION_PROD_SOURCE_CONFIG
  }
}

GSUTIL_TASK_CONCURRENCY = 64

AW4_INPUT_SECTION_SCHEMA = {
  :required => {
    "aw4Columns" => Array,
    "pooledDestPathInfix" => String,
  },
  :optional => {
    "filenameMatch" => String,
    "filenameReplace" => String,
    "storageClass" => String,
  },
}

INPUT_SCHEMAS = {
  "aw4MicroarraySources" => AW4_INPUT_SECTION_SCHEMA,
  "aw4WgsSources" => AW4_INPUT_SECTION_SCHEMA,
  "curationSources" => {
    :required => {
      "sourcePattern" => String,
      "destination" => String,
    },
    :optional => {
      "filenameMatch" => String,
      "filenameReplace" => String,
    }
  },
  "preprodCTSources" => {
    :required => {
      "preprodCtTerraProject" => String,
      "sourcePattern" => String,
      "destination" => String,
    },
    :optional => {
      "filenameMatch" => String,
      "filenameReplace" => String,
    }
  },
}

def parse_input_manifest(filename)
  manifest = YAML.load_file(filename)
  manifest.each do |k, v|
    raise ArgumentError.new("unknown input manifest key '#{k}'") unless INPUT_SCHEMAS.key? k
    section_schema = INPUT_SCHEMAS[k]
    v.each do |section_key, section_value|
      missing_keys = section_schema[:required].keys.to_set - section_value.keys.to_set
      extra_keys = section_value.keys.to_set - (section_schema[:required].keys + section_schema[:optional].keys).to_set

      unless missing_keys.empty? and extra_keys.empty?
        raise ArgumentError.new(
          "bad section definition '#{section_key}'\n" +
            "missing required keys: #{missing_keys.join(',')}\n" +
            "had unknown keys: #{extra_keys.join(',')}")
      end

      section_value.each do |inner_k, inner_v|
        want_type = section_schema[:required][inner_k]
        if want_type.nil?
          want_type = section_schema[:optional][inner_k]
        end
        unless inner_v.instance_of? want_type
          raise ArgumentError.new("'#{k}.#{inner_k}' is the wrong type, got value '#{inner_v}', wanted type #{want_type}")
        end
      end
    end
  end

  return manifest
end

def _aw4_filename_to_datetime(aw4_prefix, f)
  common = Common.new

  date_match = f.delete_prefix(aw4_prefix).match(/([\d-]+)(_\d+)?.csv/)
  unless date_match
    common.warning " AW4 filename does not match expected date format, assuming old: #{f}"
    return DateTime.new(1970)
  end
  date_part = date_match[1]
  begin
    return DateTime.strptime(date_part, "%Y-%m-%d-%H-%M-%S")
  rescue Date::Error
    common.warning "Found date-part in AW4 filename but cannot parse it, assuming old: #{date_part}"
    return DateTime.new(1970)
  end
end

def _read_aw4_rows_for_rids(aw4_prefix, rids)
  common = Common.new

  ridset = rids.to_set

  # Read all AW4s into a CSV::Row[]
  aw4_paths = []
  if aw4_prefix.start_with?("gs://")
    aw4_paths = common.capture_stdout(["gsutil", "ls", aw4_prefix + "*"]).split("\n")
  else
    aw4_paths = Dir.glob(aw4_prefix + "*")
  end
  if aw4_paths.empty?
    raise ArgumentError.new("failed to find any matching aw4 manifests @ #{aw4_prefix}")
  end
  i = 0
  matching_aw4_rows = aw4_paths.flat_map do |f|
    common.status "processed #{i}/#{aw4_paths.length} AW4 manifests" if i % 10 == 0
    i += 1

    filename_date = _aw4_filename_to_datetime(aw4_prefix, f)

    aw4_str = ""
    if aw4_prefix.start_with?("gs://")
      aw4_str = common.capture_stdout(["gsutil", "cat", f])
    else
      aw4_str = IO.read(f)
    end
    if aw4_str.empty?
      raise ArgumentError.new("failed to read content from #{f}")
    end
    aw4_rows = CSV.parse(aw4_str, headers: true).filter do |row|
      # Filter out rids which weren't requested for publishing.
      not row.header_row? and ridset.include? row["research_id"]
    end

    aw4_rows.each do |row|
      # Parse the filename for the effective datetime for downstream disambiguation.
      row["aw4_datetime"] = filename_date
    end
  end

  # Transform into a dict of research ID -> CSV::Row to dedupe
  by_rid = {}
  matching_aw4_rows.each do |aw4_row|
    # Prefer the most recent row for each
    rid = aw4_row["research_id"]
    aw4_time = aw4_row["aw4_datetime"]
    if by_rid.key? rid
      common.warning "found multiple AW4 rows for #{rid}, using the most recent"
    end
    if not by_rid.key? rid or aw4_time > by_rid[rid]["aw4_datetime"]
      by_rid[rid] = aw4_row
    end
  end

  missing_rids = ridset - by_rid.keys
  unless missing_rids.empty?
    missing_str = missing_rids.join(",")
    if missing_rids.length > 100
      missing_str = missing_rids.to_a[0..50].join(",") + "..."
    end
    raise ArgumentError.new("AW4 manifests do not contain information for #{missing_rids.length} requested research IDs (of #{by_rid.length} AW4 rows):\n" + missing_str)
  end

  # The input research ID set should already account for these properties, these
  # warnings indicate an upstream data issue or an issue with the AW4.
  by_rid.each_value do |aw4_row|
    rid = aw4_row["research_id"]
    qc = aw4_row["qc_status"]
    unless qc == "PASS"
      common.warning "given research ID #{rid} most recent AW4 has qc status: #{qc}"
    end

    # Early AW4s don't have this column. Skip in this case.
    pass_to_research = aw4_row["pass_to_research_pipeline"]
    unless pass_to_research.nil? or pass_to_research == "True"
      common.warning "given research ID #{rid} most recent AW4 has pass_to_research_pipeline: #{pass_to_research}"
    end
  end

  # Maintain the original requested order.
  rids.map { |rid| by_rid[rid] }
end

def read_research_ids_file(filename)
  IO.readlines(filename, chomp: true).filter do |line|
    if line.to_i() == 0
      Common.new.warning "skipping non-numeric research ID line: #{line}"
      false
    else
      true
    end
  end
end

def read_all_wgs_aw4s(project, rids)
  raise ArgumentError.new("manifest generation is unconfigured for #{project}") unless FILE_ENVIRONMENTS.key? project
  _read_aw4_rows_for_rids(FILE_ENVIRONMENTS[project][:source_config][:wgs_aw4_prefix], rids)
end

def read_all_microarray_aw4s(project, rids)
  raise ArgumentError.new("manifest generation is unconfigured for #{project}") unless FILE_ENVIRONMENTS.key? project
  _read_aw4_rows_for_rids(FILE_ENVIRONMENTS[project][:source_config][:microarray_aw4_prefix], rids)
end

def _apply_filename_replacement(source_name, match, replace_tmpl, rid=nil)
  common = Common.new

  replace = replace_tmpl
  unless rid.nil?
    replace = replace_tmpl.sub("{RID}", rid)
  end
  if match.nil?
    match = ".*"
  end
  return source_name.sub(Regexp.new(match), replace)
end

def _build_copy_manifest_row(source_path, ingest_base_path, destination, input_section, rid=nil, preprod_source_ingest_base_path=nil)
  source_name = File.basename(source_path)
  dest_name = source_name
  replace = input_section["filenameReplace"]
  unless replace.nil?
    dest_name = _apply_filename_replacement(
      source_name, input_section["filenameMatch"], replace, rid)
    if source_name == dest_name
      raise ArgumentError.new("filename replacement failed for '#{source_name}'")
    end
  end
  preprod_source_ingest_path = nil
  unless preprod_source_ingest_base_path.nil?
    preprod_source_ingest_path = File.join(preprod_source_ingest_base_path, dest_name)
  end
  {
    :source_path => source_path,
    :preprod_source_ingest_path => preprod_source_ingest_path,
    :ingest_path => File.join(ingest_base_path, dest_name),
    :destination_dir => destination,
    :dest_storage_class => input_section.fetch("storageClass", "STANDARD")
  }
end

def build_copy_manifest_for_aw4_section(input_section, ingest_bucket, dest_bucket, display_version_id, aw4_rows)
  # TODO(RW-8269): handle delta directories
  path_prefix = "pooled/#{input_section["pooledDestPathInfix"]}/#{display_version_id}_base"
  ingest_base_path = File.join(ingest_bucket, path_prefix)
  destination = File.join(dest_bucket, path_prefix)

  return aw4_rows.flat_map do |aw4_entry|
    source_paths = input_section["aw4Columns"].map { |k| aw4_entry[k] }
    source_paths.map do |source_path|
      _build_copy_manifest_row(source_path, ingest_base_path, destination, input_section, aw4_entry["research_id"])
    end
  end
end

def build_copy_manifest_for_curation_section(input_section, ingest_bucket, dest_bucket, display_version_id)
  path_prefix = "#{display_version_id}/#{input_section['destination']}"
  ingest_base_path = File.join(ingest_bucket, path_prefix)
  destination = File.join(dest_bucket, path_prefix)

  # -d allows the input manifest to specify subdirectories to copy in-place
  source_uris = Common.new.capture_stdout(["gsutil", "ls", "-d", input_section["sourcePattern"]]).split("\n")
  if source_uris.empty?
    raise ArgumentError.new("sourcePattern '#{input_section["sourcePattern"]}' did not match any files")
  end
  return source_uris.map do |source_path|
    _build_copy_manifest_row(source_path, ingest_base_path, destination, input_section)
  end
end

def build_copy_manifest_for_preprod_section(input_section, ingest_bucket, dest_bucket, display_version_id)
  path_prefix = "#{display_version_id}/#{input_section['destination']}"
  ingest_base_path = File.join(ingest_bucket, path_prefix)

  preprod_ingest_bucket = must_get_env_value("all-of-us-rw-preprod", :accessTiers)["controlled"][:ingest_cdr_bucket]
  preprod_ingest_base_path = ingest_base_path.sub(ingest_bucket, preprod_ingest_bucket)

  destination = File.join(dest_bucket, path_prefix)

  source_uris = Common.new.capture_stdout(["gsutil", "-i", "all-of-us-rw-preprod@appspot.gserviceaccount.com", "ls", "-d", input_section["sourcePattern"]]).split("\n")
  if source_uris.empty?
    raise ArgumentError.new("sourcePattern '#{input_section["sourcePattern"]}' did not match any files")
  end
  return source_uris.map do |source_path|
    _build_copy_manifest_row(source_path, ingest_base_path, destination, input_section, nil, preprod_ingest_base_path)
  end
end

# Concurrently run the given process block over all rows in the provided manifest.
# The block should have the following signature:
#
#   |manifest_row: CSV::Row, io_out: IO, io_err: IO| => [identifier: str, ok: boolean]
#
# where
# - identifier is a unique output identifier for this invocation, e.g. the
#   destination URI for a copy
# - ok is true if the invocation succeeded, false otherwise
#
# If ok=false is returned too many times, this function fails.
#
# Under logs_dir, this function generates the following outputs:
# - done.txt: newline-delimited output identifiers for all successful processes
# - stdout0.txt, ..., stdout{num_workers-1}.txt: stdout for each worker thread
# - stderr0.txt, ..., stderr{num_workers-1}.txt: stderr for each worker thread
def _process_files_by_manifest(all_tasks, logs_dir, status_verb, num_workers)
  common = Common.new

  FileUtils.makedirs(logs_dir)

  num_workers = [all_tasks.length, num_workers].min

  done = Queue.new
  error = Queue.new

  # Spawn a fixed set of threads, assign exclusive work to each
  fail_limit = 10
  workers = num_workers.times.map do |i|
    t = Thread.new {
      File.open(File.join(logs_dir, "stdout#{i}.txt"), "w") do |wout|
        File.open(File.join(logs_dir, "stderr#{i}.txt"), "w") do |werr|
          fail_count = 0

          # Process every num_workers'th task, starting at i
          j = i
          while j < all_tasks.length
            task = all_tasks[j]
            output, ok = yield task, wout, werr
            if ok
              done << output
            else
              error << output
              fail_count += 1
              if fail_count >= fail_limit
                raise IOError.new("failed to process #{fail_count} manifest rows in a single worker")
              end
            end
            j += num_workers
          end
        end
      end
    }
    t.abort_on_exception = true
    t
  end

  # In a separate thread, await worker completion and close channels.
  Thread.new {
    workers.each { |w| w.join }
    [done, error].each { |q| q.close }
  }

  # In a separate thread, consume errors off the queue
  errors = []
  monitor_error = Thread.new {
    until error.closed?
      error_out = error.pop
      errors.push(error_out) unless error_out.nil?
    end
  }

  # In the main thread, watch the done channel, write finished rows and log progress.
  File.open(File.join(logs_dir, "done.txt"), "w") do |w|
    start = Time.now.to_i
    count = 0
    until done.closed?
      value = done.pop
      unless value.nil?
        w.write(value + "\n")
        count += 1
      end
      # Log every 100 and at the end (when the queue is closed, i.e. value=nil)
      if count % 100 == 0 or value.nil?
        delta_min = (Time.now.to_i - start) / 60
        # Carriage return here to keep the progress on the same terminal line
        printf("\r%s %d/%d files [%.02f%%] (running for %dh%dm)",
               status_verb, count, all_tasks.length,
               100 * count.to_f / all_tasks.length, delta_min / 60, delta_min % 60)
      end
    end
  end
  # Force a newline to reset the carriage return interactions above.
  puts ""

  # Ensure the error monitor has closed.
  monitor_error.join
  unless errors.empty?
    common.error "failed to process the following paths:\n#{errors.join("\n")}"
  end
end

# Stage files as specified in the given manifest. This copies files into the VPC-SC
# ingest bucket to prepare them for publishing. This intermediate step is required
# for publishing. For details, see:
# https://docs.google.com/document/d/1EHw5nisXspJjA9yeZput3W4-vSIcuLBU5dPizTnk1i0/edit
def stage_files_by_manifest(project, manifest_path, logs_dir, concurrency = GSUTIL_TASK_CONCURRENCY)
  common = Common.new
  deploy_account = must_get_env_value(project, :publisher_account)

  all_tasks = CSV.read(manifest_path, headers: true, return_headers: false)

  # For now, we support pulling specifically from a ct preprod workspace project as a source.
  # This scenario is special-cased, since it's where we're doing operational prep of CDR assets.
  # For publishing in lower environments, i.e. from an arbitrary bucket, just use a normal curationSource.
  preprod_deploy_account = must_get_env_value("all-of-us-rw-preprod", :publisher_account)
  preprod_ingest_bucket = must_get_env_value("all-of-us-rw-preprod", :accessTiers)["controlled"][:ingest_cdr_bucket]

  needs_preprod_grant = false
  if all_tasks.any? { |task| not task["preprod_source_ingest_path"].to_s.empty? }
    unless ["all-of-us-rw-prod", "all-of-us-rw-preprod"].include? project
      raise ArgumentError.new("specified a preprod source with project #{project}, this is not allowed")
    end
    # preprod -> prod requires a temporary access grant
    needs_preprod_grant = project == "all-of-us-rw-prod"
  end

  if needs_preprod_grant
    common.run_inline(["gsutil", "-i", preprod_deploy_account, "iam", "ch", "serviceAccount:#{deploy_account}:storageAdmin", preprod_ingest_bucket])
  end

  _process_files_by_manifest(
    all_tasks, File.join([logs_dir, "stage", File.basename(manifest_path, ".csv")]), "Staged", concurrency) do |task, wout, werr|
    ingest_path = task["ingest_path"]

    unless task["preprod_source_ingest_path"].to_s.empty?
      # preprod workspace -> (cp) -> preprod ingest -> (mv) -> prod ingest
      unless system(
          "gsutil",
          "-i",
          preprod_deploy_account,
          "cp",
          task["source_path"],
          task["preprod_source_ingest_path"],
          :out => wout,
          :err => werr)
        next [ingest_path, false]
      end
      next [ingest_path, system(
         "gsutil",
         "-i",
         deploy_account,
         "mv",
         task["preprod_source_ingest_path"],
         ingest_path,
         :out => wout,
         :err => werr)]
    end
    next [ingest_path, system(
      "gsutil",
      "-i",
      deploy_account,
      "cp",
      task["source_path"],
      ingest_path,
      :out => wout,
      :err => werr)]
  end

  if needs_preprod_grant
    common.run_inline(["gsutil", "-i", preprod_deploy_account, "iam", "ch", "-d", "serviceAccount:#{deploy_account}:storageAdmin", preprod_ingest_bucket])
  end
end

# Publish files to the CDR bucket as specified in the given manifests.
def publish_files_by_manifests(project, manifest_paths)
  # TODO(RW-8266): Implement.
  raise NotImplementedError.new()
end
