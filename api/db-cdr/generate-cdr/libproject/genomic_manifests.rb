require "csv"
require "date"
require "fileutils"
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
  :wgs_aw4_prefix => "gs://prod-drc-broad/AW4_wgs_manifest/AoU_DRCB_SEQ_"
}

FILE_ENVIRONMENTS = {
  "all-of-us-rw-preprod" => {
    :source_config => CURATION_PROD_SOURCE_CONFIG,
  },
  "all-of-us-rw-prod" => {
    :source_config => CURATION_PROD_SOURCE_CONFIG
  }
}

FILE_PREFIX_REPLACEMENTS = {
  :wgs_cram => {
    # Match/replace the GC filename; this works for both .cram and .cram.crai
    :matchSourceName => /^[^\/]+\.cram/,
    :destNameFn => ->(rid) { "wgs_#{rid}.cram" }
  }
}

GSUTIL_TASK_CONCURRENCY = 64

def _aw4_filename_to_datetime(aw4_prefix, f)
  common = Common.new

  date_match = f.delete_prefix(aw4_prefix).match(/([\d-]+)(_\d+)?.csv/)
  unless date_match
    common.warning "AW4 filename does not match expected date format, assuming old: #{f}"
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
    aw4_paths = common.capture_stdout(["gsutil", "ls", aw4_prefix +  "*"]).split("\n")
  else
    aw4_paths = Dir.glob(aw4_prefix + "*")
  end
  if aw4_paths.empty?
    raise ArgumentError.new("failed to find any matching aw4 manifests @ #{aw4_prefix}")
  end
  i = 0
  matching_aw4_rows = aw4_paths.flat_map do |f|
    common.status "processed #{i}/#{aw4_paths.length} AW4 manifests" if i % 10 == 0
    i+=1

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
    raise ArgumentError.new("AW4 manifests do not contain information for requested research IDs:\n" + missing_rids.join(","))
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

def read_all_wgs_aw4s(project, rids)
  _read_aw4_rows_for_rids(FILE_ENVIRONMENTS[project][:source_config][:wgs_aw4_prefix], rids)
end

def build_cram_manifest(project, ingest_bucket, dest_bucket, display_version_id, rids)
  raise ArgumentError.new("manifest generation is unconfigured for #{project}") unless FILE_ENVIRONMENTS.key? project

  wgs_aw4s = read_all_wgs_aw4s(project, rids)
  replace_config = FILE_PREFIX_REPLACEMENTS[:wgs_cram]

  # TODO(RW-8269): Support delta directories.
  path_prefix = "pooled/wgs/cram/#{display_version_id}_base"
  ingest_base_path = File.join(ingest_bucket, path_prefix)
  dest_base_path = File.join(dest_bucket, path_prefix)

  return wgs_aw4s.flat_map do |aw4_entry|
    [aw4_entry["cram_path"], aw4_entry["crai_path"]].map do |source_path|
      source_name = File.basename(source_path)
      dest_name = source_name.sub(replace_config[:matchSourceName], replace_config[:destNameFn].call(aw4_entry["research_id"]))

      if source_name == dest_name
        raise ArgumentError.new("filename replacement failed for #{source_name}")
      end
      {
        :source_path => source_path,
        :ingest_path => File.join(ingest_base_path, dest_name),
        :dest_path => File.join(dest_base_path, dest_name),
        :dest_storage_class => "nearline"
      }
    end
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
def _process_files_by_manifest(manifest_path, logs_dir, status_verb, num_workers)
  common = Common.new

  FileUtils.makedirs(logs_dir)
  all_tasks = CSV.read(manifest_path, headers: true, return_headers: false)

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
              fail_count+=1
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
               100*count.to_f/all_tasks.length, delta_min/60, delta_min%60)
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
  deploy_account = must_get_env_value(project, :publisher_account)

  _process_files_by_manifest(
      manifest_path, File.join([logs_dir, 'stage']), "Staged", concurrency) do |task, wout, werr|
    ingest_path = task["ingest_path"]
    [ingest_path, system(
       "gsutil",
       "-i",
       deploy_account,
       "cp",
       task["source_path"],
       ingest_path,
       :out => wout,
       :err => werr)]
  end

end

# Publish files to the CDR bucket as specified in the given manifest.
def publish_files_by_manifest(project, manifest_path, logs_dir, concurrency = GSUTIL_TASK_CONCURRENCY)
  deploy_account = must_get_env_value(project, :publisher_account)

  _process_files_by_manifest(
      manifest_path, File.join([logs_dir, 'publish']), "Published", concurrency) do |task, wout, werr|
    dest_path = task["dest_path"]
    [dest_path, system(
       "gsutil",
       "-i",
       deploy_account,
       "mv",
       "-s",
       task["dest_storage_class"],
       task["ingest_path"],
       dest_path,
       :out => wout,
       :err => werr)]
  end
end
