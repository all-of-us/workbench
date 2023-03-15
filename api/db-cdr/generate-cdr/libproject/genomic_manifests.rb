require "csv"
require "date"
require "fileutils"
require "yaml"
require_relative "../../../../aou-utils/utils/common"
require_relative "../../../libproject/environments"

# Utilities for generating, managing, publishing genomic data files. At a high
# level, this facilitates the data flow:
#
#   Genome Centers / Broad genomic curation / preprod
#    -> CDR ingest bucket
#    -> Researcher Workbench CDR bucket
#
# Inputs:
#
# 1. The input manifest yaml file. This describes a mapping of upstream data sources
#    to the CDR bucket. See INPUT_SCHEMAS below for the file schema / documentation.
# 2. The microarray and/or WGS research ID lists.
# 3. AW4 manifests. These are CSV files passed from the Broad genomic curation
#    team to the DRC for data handoff. A single manifest corresponds to a batch
#    of data which was processed, so you should expect to see many manifests. In
#    some cases, data will also be processed multiple times, in which case
#    the latest should be taken. Note that WGS and Microarray have different specs.
#
#    In the Workbench, we are primarily using AW4 manifests as a lookup between research ID and
#    curated data file path, though it also contains some logging metadata such as QC status and
#    research inclusion. See the CDR playbook for more documentation on AW4s:
#    https://docs.google.com/document/d/1St6pG_EUFB9oRQUQaOSO7a9UPxPkQ5n4qAVyKF9j9tk/edit#heading=h.xt7avgt1nsoh
#
# Intermediates:
#
# We generate one or more manifests of source/destination GCS file paths, which
# describe how data will be copied.
#
# See the CDR playbook for more context on the overall publishing process:
# https://docs.google.com/document/d/1St6pG_EUFB9oRQUQaOSO7a9UPxPkQ5n4qAVyKF9j9tk/edit#heading=h.xt7avgt1nsoh

PROD_BROAD_BUCKET = "gs://prod-drc-broad"
OLD_ARRAYS_PATH_INFIX = "array_old_egt_files"

CURATION_SYNTHETIC_SOURCE_CONFIG = {
  :wgs_aw4_prefix => "gs://all-of-us-workbench-test-genomics/aw4_wgs/test_aw4_2.csv"
}

CURATION_PROD_SOURCE_CONFIG = {
  # Optional: to speed up iteration with this script, download and run against a local directory instead.
  :wgs_aw4_prefix => "#{PROD_BROAD_BUCKET}/AW4_wgs_manifest/AoU_DRCB_SEQ_",
  # This contains all AW4 manifests.
  :microarray_aw4_prefix => "#{PROD_BROAD_BUCKET}/AW4_array_manifest/AoU_DRCB_GEN_"
}

FILE_ENVIRONMENTS = {
  "all-of-us-workbench-test" => {
    :source_config => CURATION_SYNTHETIC_SOURCE_CONFIG,
  },
  "all-of-us-rw-perf" => {
    :source_config => CURATION_SYNTHETIC_SOURCE_CONFIG,
  },
  "all-of-us-rw-staging" => {
    :source_config => CURATION_SYNTHETIC_SOURCE_CONFIG,
  },
  "all-of-us-rw-stable" => {
    :source_config => CURATION_SYNTHETIC_SOURCE_CONFIG,
  },
  "all-of-us-rw-preprod" => {
    :source_config => CURATION_PROD_SOURCE_CONFIG,
  },
  "all-of-us-rw-prod" => {
    :source_config => CURATION_PROD_SOURCE_CONFIG
  }
}

GSUTIL_TASK_CONCURRENCY = 32

AW4_INPUT_SECTION_SCHEMA = {
  :required => {
    # Which AW4 column(s) to pull source URIs from.
    "aw4Columns" => Array,
    # The destination path infix; this excluded the bucket and display version ID.
    "pooledDestPathInfix" => String,
  },
  :optional => {
    # Mapping of AW4 source column to output manifest CSV column. If specified, creates a local
    # manifest of destination URIs corresponding to the given AW4 columns. filename{Match,Replace}
    # are applied as usual.
    "outputManifestSpec" => Hash,
    # A regex pattern which MUST match all source URIs
    "filenameMatch" => String,
    # A regex replacement, which can include group captures from filenameMatch.
    # Additionally, the string {RID} will be replaced with the corresponding research
    # ID for this file.
    "filenameReplace" => String,
    # The GCS storage class, e.g. STANDARD, NEARLINE. Defaults STANDARD.
    "storageClass" => String,
    # Specifies whether this CDR release is a base or delta. If this field exists, then the release
    # is a delta one.
    # The output directory name will be post-fixed by _delta and the logic to compare with previous
    # release will be triggered. The value should be the release to diff against.
    # For example, v6 or v7
    "deltaRelease" => String,
    # If this field exists, then the manifest will be read from it if it can't be read from the
    # regular manifest path.
    "deltaReleaseManifestPath" => String,
  },
}

# This describes the input manifest YAML file.
INPUT_SCHEMAS = {

  # Data sources backed from the microarray AW4 manifests. The manifests
  # refer to either raw GC files, or curated operational files
  "aw4MicroarraySources" => AW4_INPUT_SECTION_SCHEMA,
  # Data sources backed from the WGS AW4 manifests. The manifests
  # refer to either raw GC files, or curated operational files
  "aw4WgsSources" => AW4_INPUT_SECTION_SCHEMA,
  # Curation sources, usually backed by the Broad DRC bucket, i.e. an internal
  # operational bucket.
  "curationSources" => {
    :required => {
      # A gsutil wildcard pattern to select the upstream files. gsutil ls -d is used,
      # which allows one to select subdirectories, in addition to individual files.
      # filenameMatch and filenameReplace should only be used in combination with a
      # sourcePattern that matches individual files.
      "sourcePattern" => String,
      # A relative destination directory in the published bucket. This should not include
      # the bucket or display version ID.
      "destination" => String,
    },
    :optional => {
      "filenameMatch" => String,
      # See above documentation, but does NOT support {RID} replacement.
      "filenameReplace" => String,
    }
  },
  # Curation sources, usually backed by the Broad DRC bucket, i.e. an internal
  # operational bucket.
  "preprodCTSources" => {
    :required => {
      # The preprod CT Terra source project for this data. This field is primarily
      # for documentation purposes, but should correspond to the project containing
      # the pattern referenced by the sourcePattern's below bucket.
      "preprodCTTerraProject" => String,
      "sourcePattern" => String,
      "destination" => String,
    },
    :optional => {
      "filenameMatch" => String,
      # See above documentation, but does NOT support {RID} replacement.
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

  adjust_old_array_uris = aw4_prefix.include? OLD_ARRAYS_PATH_INFIX

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
      if adjust_old_array_uris
        # The old AW4 array manifests still point to the "active" VCF directory, which contains
        # an overlapping subset of reprocessed files. Adjust the URIs to point to this older directory
        # This should be removed once we switch to the newer AW4 manifests in a future CDR release.
        row.each do |k, v|
          if v.include? PROD_BROAD_BUCKET and not v.include? OLD_ARRAYS_PATH_INFIX
            row[k] = v.gsub(PROD_BROAD_BUCKET, File.join(PROD_BROAD_BUCKET, OLD_ARRAYS_PATH_INFIX))
          end
        end
      end

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
    missing_str = missing_rids.length <= 100 ? missing_rids.join(",") : (missing_rids.to_a[0..100].join(",") + "...")
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

def _apply_filename_replacement(source_name, match, replace_tmpl, rid = nil)
  replace = replace_tmpl
  unless rid.nil?
    replace = replace_tmpl.sub("{RID}", rid)
  end
  if match.nil?
    match = ".*"
  end
  return source_name.sub(Regexp.new(match), replace)
end

def _apply_storage_class_staging_prefix(path, storage_class)
  parts = path.split("/")
  unless parts[0] == "gs:" and parts[1] == ""
    raise ArgumentError("expected input path to be a full gs:// path")
  end
  # insert the prefix after the bucket
  (parts[0..2] + ["#{storage_class.downcase}-staged"] + parts[3..-1]).join("/")
end

def _build_copy_manifest_row(
  source_path, ingest_base_path, destination, input_section,
  rid = nil, preprod_source_cdr_base_path = nil, preprod_source_ingest_base_path = nil)
  common = Common.new
  source_name = File.basename(source_path)
  dest_name = source_name
  replace = input_section["filenameReplace"]
  unless replace.nil?
    dest_name = _apply_filename_replacement(
      source_name, input_section["filenameMatch"], replace, rid)
    if source_name == dest_name
      common.warning "filename replacement failed for '#{source_name}'"
    end
  end

  preprod_source_cdr_path = nil
  unless preprod_source_cdr_base_path.nil?
    preprod_source_cdr_path = File.join(preprod_source_cdr_base_path, dest_name)
  end
  preprod_source_ingest_path = nil
  unless preprod_source_ingest_base_path.nil?
    preprod_source_ingest_path = File.join(preprod_source_ingest_base_path, dest_name)
  end

  # For custom storage classes, we apply a global path prefix so that we can still use
  # cloud storage transfer service downstream (it operates on prefix matching only). This
  # allows us to interleave storage classes within the same directory, e.g. for CRAM files
  # (NEARLINE) and CRAM indexes (STANDARD).
  storage_class = input_section.fetch("storageClass", "STANDARD")
  unless storage_class == "STANDARD"
    ingest_base_path = _apply_storage_class_staging_prefix(ingest_base_path, storage_class)
  end
  {
    # Due to idiosyncracies of gsutil cp, we want to specify src directories without a trailing /
    :source_path => source_path.chomp("/"),
    :preprod_source_project => input_section["preprodCTTerraProject"],
    :preprod_source_cdr_path => preprod_source_cdr_path,
    :preprod_source_ingest_path => preprod_source_ingest_path,
    :ingest_path => File.join(ingest_base_path, dest_name),
    :destination_dir => destination,
    :dest_storage_class => input_section["storageClass"]
  }
end

# Logic to read the previous manifest for a delta release.
# Returns a hash of person_id => CSV row found in previous manifest
def _read_previous_manifest(project, dest_bucket, deltaReleaseManifestPath, deltaRelease, infix)
  common = Common.new
  prev_manifest = ""
  manifest_path = deltaReleaseManifestPath

  deploy_account = must_get_env_value(project, :publisher_account)

  # If deltaReleaseManifestPath is specified, try to use it.
  unless deltaReleaseManifestPath.nil?
    if deltaReleaseManifestPath.start_with?("gs://")
      prev_manifest = common.capture_stdout(["gsutil", "-i", deploy_account, "cat", deltaReleaseManifestPath])
    else
      prev_manifest = IO.read(deltaReleaseManifestPath)
    end
  end

  # Try to find the manifest given the deltaRelease field.
  if not deltaRelease.nil? and prev_manifest.empty?
    manifest_path = "#{dest_bucket}/#{deltaRelease}/#{infix}/manifest.csv"
    prev_manifest = common.capture_stdout(["gsutil", "-u", project, "-i", deploy_account, "cat", manifest_path])
  end

  # If the manifest still cannot be
  # read then throw an error because config is not correct.

  unless deltaRelease.nil? and deltaReleaseManifestPath.nil?
    if prev_manifest.empty?
      raise ArgumentError.new("failed to read previous manifest from #{manifest_path},
        make sure to provide the correct previous release in the input manifest")
    end
  end

  # Convert the prev_manifest to dict of research ID -> CSV::Row
  prev_manifest_csv = CSV.parse(prev_manifest, headers: true)
  prev_manifest_hash = {}
  prev_manifest_csv.each do |manifest_row|
    rid = manifest_row["person_id"]
    prev_manifest_hash[rid] = manifest_row
  end

  return prev_manifest_hash
end

##
def build_manifests_for_aw4_section(project, input_section, ingest_bucket, dest_bucket, display_version_id, aw4_rows, output_manifest_path)
  path_prefix = _get_pooled_path(input_section["pooledDestPathInfix"], display_version_id, input_section["deltaRelease"])

  ingest_base_path = File.join(ingest_bucket, path_prefix)
  destination = File.join(dest_bucket, path_prefix)

  output_manifest = nil
  unless input_section["outputManifestSpec"].to_s.empty?
    output_manifest = []
  end

  prev_manifest = _read_previous_manifest(
    project,
    dest_bucket,
    input_section["deltaReleaseManifestPath"],
    input_section["deltaRelease"],
    input_section["pooledDestPathInfix"])

  copy_manifest = aw4_rows.flat_map do |aw4_entry|
    unless output_manifest.nil?
      out_row = { "person_id" => aw4_entry["research_id"] }
      # If the research_id already exists in the prev manifest, just put as is in the new output manifest
      unless prev_manifest.nil? or prev_manifest[aw4_entry["research_id"]].nil?
        output_manifest.push(prev_manifest[aw4_entry["research_id"]].to_h)
      else
        input_section["outputManifestSpec"].each do |aw4_col, out_col|
          row = _build_copy_manifest_row(aw4_entry[aw4_col], ingest_base_path, destination, input_section, aw4_entry["research_id"])
          destination_uri = File.join(row[:destination_dir], File.basename(row[:ingest_path]))
          out_row[out_col] = destination_uri
        end
        output_manifest.push(out_row)
      end
    end

    source_paths = input_section["aw4Columns"].map { |k| aw4_entry[k] }
    source_paths.map do |source_path|
      if prev_manifest.nil? or prev_manifest[aw4_entry["research_id"]].nil?
        _build_copy_manifest_row(source_path, ingest_base_path, destination, input_section, aw4_entry["research_id"])
      end
    end
  end

  # To remove nils
  copy_manifest = copy_manifest.compact

  unless output_manifest.nil?
    unpooled_prefix = "#{display_version_id}/#{input_section["pooledDestPathInfix"]}"
    unpooled_ingest_base_path = File.join(ingest_bucket, unpooled_prefix)
    unpooled_destination = File.join(dest_bucket, unpooled_prefix)
    copy_manifest.push(_build_copy_manifest_row(
                         output_manifest_path, unpooled_ingest_base_path, unpooled_destination, {
                         "filenameMatch" => File.basename(output_manifest_path),
                         "filenameReplace" => "manifest.csv",
                       }))
  end

  return [copy_manifest, output_manifest]
end

def _get_pooled_path(pathInfix, display_version_id, is_delta_release)

  version_postfix = "base"

  unless is_delta_release.nil?
    version_postfix = "delta"
  end

  return "pooled/#{pathInfix}/#{display_version_id}_#{version_postfix}"

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

  tier = must_get_env_value("all-of-us-rw-preprod", :accessTiers)["controlled"]
  preprod_cdr_base_path = ingest_base_path.sub(ingest_bucket, tier[:dest_cdr_bucket])
  preprod_ingest_base_path = ingest_base_path.sub(ingest_bucket, tier[:ingest_cdr_bucket])

  destination = File.join(dest_bucket, path_prefix)

  source_uris = Common.new.capture_stdout(["gsutil", "-i", "all-of-us-rw-preprod@appspot.gserviceaccount.com", "ls", "-d", input_section["sourcePattern"]]).split("\n")
  if source_uris.empty?
    raise ArgumentError.new("sourcePattern '#{input_section["sourcePattern"]}' did not match any files")
  end
  return source_uris.map do |source_path|
    _build_copy_manifest_row(source_path, ingest_base_path, destination, input_section, nil, preprod_cdr_base_path, preprod_ingest_base_path)
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

def _update_project_iam_object_viewer(sa_actor, sa_principal, project_id, add)
  Common.new.run_inline([
                          "gcloud",
                          "--impersonate-service-account",
                          sa_actor,
                          "--quiet",
                          "projects",
                          add ? "add-iam-policy-binding" : "remove-iam-policy-binding",
                          project_id,
                          "--member",
                          "serviceAccount:#{sa_principal}",
                          "--role",
                          "roles/storage.objectViewer"
                        ])
end

def _update_bucket_storage_admin(sa_actor, sa_principal, bucket, add)
  Common.new.run_inline(
    ["gsutil", "-i", sa_actor, "iam", "ch"] +
      (add ? [] : ["-d"]) +
      ["serviceAccount:#{sa_principal}:objectAdmin", bucket]
  )
end

def _maybe_update_preprod_access(project, all_tasks, add)
  deploy_account = must_get_env_value(project, :publisher_account)
  preprod_appspot_account = "all-of-us-rw-preprod@appspot.gserviceaccount.com"
  preprod_deploy_account = must_get_env_value("all-of-us-rw-preprod", :publisher_account)
  preprod_ingest_bucket = must_get_env_value("all-of-us-rw-preprod", :accessTiers)["controlled"][:ingest_cdr_bucket]

  if (project == "all-of-us-workbench-test")
    return
  end

  needs_cross_env_grant = false

  preprod_source_projects = (
    all_tasks
      .filter { |k, _v| k == "preprod_source_project" }
      .map { |_k, v| v }
      .filter { |p| !p.to_s.empty? }
      .to_set
  )

  needs_preprod_grant = !preprod_source_projects.empty?
  if needs_preprod_grant
    unless ["all-of-us-rw-prod", "all-of-us-rw-preprod"].include? project
      raise ArgumentError.new("specified a preprod source with project #{project}, this is not allowed")
    end
    # preprod -> prod requires a temporary access grant
    needs_preprod_grant = project == "all-of-us-rw-prod"
  end

  if needs_preprod_grant
    preprod_source_projects.each do |p|
      _update_project_iam_object_viewer(preprod_appspot_account, preprod_deploy_account, p, add)
      Common.new.status "Sleeping for 1m after IAM grant to mitigate consistency issues"
      sleep 60
    end
  end
  if needs_cross_env_grant
    _update_bucket_storage_admin(preprod_deploy_account, deploy_account, preprod_ingest_bucket, add)
  end
end

def maybe_grant_preprod_access(project, all_tasks)
  _maybe_update_preprod_access(project, all_tasks, true)
end

def maybe_revoke_preprod_access(project, all_tasks)
  _maybe_update_preprod_access(project, all_tasks, false)
end

# Stage files as specified by the given manifest lines. This copies files into the VPC-SC
# ingest bucket to prepare them for publishing. This intermediate step is required
# for publishing. For details, see:
# https://docs.google.com/document/d/1EHw5nisXspJjA9yeZput3W4-vSIcuLBU5dPizTnk1i0/edit
#
# IMPORTANT: custom storage classes should NOT be respected within the ingest staging
# bucket; only STANDARD storage class should be used here. This file staging is transient
# and therefore receives only penalties for colder storage.
def stage_files_by_manifest(project, all_tasks, logs_dir, concurrency = GSUTIL_TASK_CONCURRENCY)
  # For now, we support pulling specifically from a ct preprod workspace project as a source.
  # This scenario is special-cased, since it's where we're doing operational prep of CDR assets.
  # For publishing in lower environments, i.e. from an arbitrary bucket, just use a normal curationSource.
  deploy_account = must_get_env_value(project, :publisher_account)
  preprod_deploy_account = must_get_env_value("all-of-us-rw-preprod", :publisher_account)

  _process_files_by_manifest(
    all_tasks,
    File.join([logs_dir, "stage"]),
    "Staged",
    concurrency
  ) do |task, wout, werr|
    ingest_path = task["ingest_path"]

    unless task["preprod_source_project"].to_s.empty?
      # preprod workspace -> (cp) -> preprod ingest -> (mv) -> prod ingest
      unless system(
        "gsutil",
        "-i",
        preprod_deploy_account,
        "-m",
        "cp",
        "-r",
        task["source_path"],
        task["preprod_source_cdr_path"],
        :out => wout,
        :err => werr)
        # Note: we use next here because this is inside a block; this yields the value up
        # to the caller which is yielding to this block, i.e. _process_files_by_manifest.
        next [ingest_path, false]
      end
      unless system(
        "gsutil",
        "-i",
        preprod_deploy_account,
        "-m",
        "cp",
        "-r",
        task["preprod_source_cdr_path"],
        task["preprod_source_ingest_path"],
        :out => wout,
        :err => werr)
        next [ingest_path, false]
      end
      next [ingest_path, system(
        "gsutil",
        "-i",
        deploy_account,
        "-m",
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
      "-m",
      "cp",
      "-r",
      task["source_path"],
      ingest_path,
      :out => wout,
      :err => werr)]
  end
end

def _find_common_gcs_prefix(a, b)
  prefix = ""
  for i in 0..a.length - 1 do
    if a[i] != b[i]
      break
    end
    prefix += a[i]
  end

  # A common GCS prefix contain at least one path component.
  if prefix["gs://".length, prefix.length].chomp("/").split("/").length < 2
    return nil
  end
  prefix
end

# Returns a list of publishing configs in the form of a dictionary with the following structure:
# [{
#   :source => "gs://..."
#   :dest => "gs://..."
#   :storage_class => nil | "NEARLINE" | "..."
# }, ...]
#
# This function attempts to merge the given copy manifests into the smallest number of possible
# publishing configs, executable by Storage Transfer Service. When finding common path prefixes
# it will not be unnnecessarily broad, but if other files exist in the ingest bucket at time of
# copying which are under a common parent directory of the copy manifest paths, it may be
# picked up in a transfer. For this reason, it is recommended that the ingest bucket is cleared
# before staging any new data, and to avoid concurrent file staging/publishing.
def build_publish_configs(manifest_paths)
  # Group by unique ingest directory.
  config_by_source_dir = {}
  manifest_paths.each do |path|
    CSV.foreach(path, headers: true, return_headers: false) do |row|
      ingest_dir = File.dirname(row["ingest_path"])
      dest_dir = row["destination_dir"];
      unless ingest_dir.end_with?("/")
        ingest_dir += "/";
      end
      unless dest_dir.end_with?("/")
        dest_dir += "/";
      end
      unless config_by_source_dir.key? ingest_dir
        config_by_source_dir[ingest_dir] = {
          :source => ingest_dir,
          :dest => dest_dir,
          :storage_class => row["dest_storage_class"],
        }
      end
    end
  end

  # Group by configuration parameters which cannot vary within a single transfer job. Currently, this is
  # just storage class.
  configs_by_storage_class = {}
  config_by_source_dir.each_value do |c|
    existing = configs_by_storage_class[c[:storage_class]]
    if existing.nil?
      configs_by_storage_class[c[:storage_class]] = {
        c[:source] => c
      }
      next
    end

    # Merge all values that share configuration settings into the longest common shared prefix.
    mergable_config = nil
    existing.each do |existing_source, existing_config|
      unless _find_common_gcs_prefix(existing_source, c[:source]).nil?
        mergable_config = existing_config
        break
      end
    end

    if mergable_config.nil?
      existing[c[:source]] = c
    else
      existing.delete(mergable_config[:source])
      common_source = _find_common_gcs_prefix(mergable_config[:source], c[:source])
      existing[common_source] = {
        :source => common_source,
        :dest => _find_common_gcs_prefix(mergable_config[:dest], c[:dest]),
        :storage_class => c[:storage_class],
      }
    end
  end

  configs_by_storage_class.values.flat_map { |configs| configs.values }
end

# Creates Cloud Storage Transfer service jobs to publish data to the CDR bucket,
# according to the given configs.
def publish(project, config, job_name)
  common = Common.new
  deploy_account = must_get_env_value(project, :publisher_account)

  maybe_args = []
  unless config[:storage_class].to_s.empty?
    maybe_args += ["--custom-storage-class=#{config[:storage_class]}"]
  end
  common.run_inline([
                      "gcloud",
                      "--impersonate-service-account",
                      deploy_account,
                      "transfer",
                      "jobs",
                      "create",
                      config[:source],
                      config[:dest],
                      "--project=#{project}",
                      "--name=#{job_name}",
                      "--description='#{job_name}: transfer job was automatically created by RW tooling and is intended to be run only once, see genomic_manifests.rb.'",
                      "--delete-from=source-after-transfer",
                    ] + maybe_args)
end
