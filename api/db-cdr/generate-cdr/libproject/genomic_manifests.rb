require "csv"
require "date"

PROD_SOURCE_CONFIG = {
  # XXX: sample value
  # :wgs_aw4_glob => "/usr/local/google/home/calbach/aou/data-ops/2022q2_june_release/aw4_sample/*"
  # XXX: fake, but more real value
  :wgs_aw4_prefix => "/usr/local/google/home/calbach/aou/data-ops/2022q2_june_release/aw4s/AoU_DRCB_SEQ_"
  # XXX: Real value
  # :wgs_aw4_glob => "gs://prod-drc-broad/AW4_wgs_manifest/*"
}


FILE_ENVIRONMENTS = {
  "all-of-us-rw-preprod" => {
    :source_config => PROD_SOURCE_CONFIG,
  },
  "all-of-us-rw-prod" => {
    :source_config => PROD_SOURCE_CONFIG
  }
}

FILE_PREFIX_REPLACEMENTS = {
  :wgs_cram => {
    :match => /^[^\/]+\.cram/,
    :replacementFn => ->(rid) { "wgs_#{rid}.cram" }
  }
}

def _aw4_filename_to_datetime(aw4_prefix, f)
  common = Common.new

  date_match = f.delete_prefix(aw4_prefix).match(/([\d-]+)(_\d+)?.csv/)
  if not date_match
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

def _read_research_id_csvs(aw4_prefix, ridset)
  common = Common.new

  # TODO: doc link for AW4s
  # Expected WGS header:
  # biobank_id,sample_id,sex_at_birth,site_id,vcf_hf_path,vcf_hf_md5_path,vcf_hf_index_path,vcf_raw_path,vcf_raw_md5_path,vcf_raw_index_path,gvcf_path,gvcf_md5_path,cram_path,cram_md5_path,crai_path,research_id,qc_status,drc_sex_concordance,drc_contamination,drc_mean_coverage,drc_fp_concordance,pass_to_research_pipeline

  # Read all AW4s into a CSV::Row[]
  matching_aw4_rows = Dir.glob(aw4_prefix + "*").flat_map do |f|
    filename_date = _aw4_filename_to_datetime(aw4_prefix, f)

    aw4_rows = CSV.read(f, headers: true)
    aw4_rows.each do |row|
      # Parse the filename for the effective datetime for downstream disambiguation.
      row["aw4_datetime"] = filename_date
    end

    aw4_rows.filter do |row|
      # Filter out rids which weren't requested for publishing.
      not row.header_row? and ridset.include? row["research_id"]
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
    if not by_rid.key? rid or aw4_time < by_rid[rid]["aw4_datetime"]
      by_rid[rid] = aw4_row
    end
  end

  missing_rids = ridset - by_rid.keys
  unless missing_rids.empty?
    raise ArgumentError.new("AW4 manifests do not contain information for requested research IDs:\n" + missing_rids.join(","))
  end

  # The input resarch ID set should already account for these properties, these
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

  by_rid.values
end

def read_all_wgs_aw4s(project, ridset)
  _read_research_id_csvs(FILE_ENVIRONMENTS[project][:source_config][:wgs_aw4_prefix], ridset)
end


def read_all_microarry_aw4s(project, ridset)
end


def build_cram_manifest(project, ingest_bucket, dest_bucket, display_version_id, ridset)
  raise ArgumentError.new("manifest generation is unconfigured for #{project}") unless FILE_ENVIRONMENTS.key? project

  wgs_aw4s = read_all_wgs_aw4s(project, ridset)
  replace_config = FILE_PREFIX_REPLACEMENTS[:wgs_cram]

  # TODO(RW-8269): Support delta directories.
  path_prefix = "pooled/wgs/cram/#{display_version_id}_base"
  ingest_base_path = File.join(ingest_bucket, path_prefix)
  dest_base_path = File.join(dest_bucket, path_prefix)

  return wgs_aw4s.flat_map do |aw4_entry|
    [aw4_entry["cram_path"], aw4_entry["crai_path"]].map do |source_path|
      source_name = File.basename(source_path)
      dest_name = source_name.sub(replace_config[:match], replace_config[:replacementFn].call(aw4_entry["sample_id"]))
      {
        :source_path => source_path,
        :ingest_path => File.join(ingest_base_path, dest_name),
        :dest_path => File.join(dest_base_path, dest_name),
        :dest_storage_class => "nearline"
      }
    end
  end
end
