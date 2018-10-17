require_relative "../../../../aou-utils/utils/common"
require_relative "../../../libproject/wboptionsparser"
require "json"
require "set"
require "tempfile"

def update_bq_acl(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
   "--bq-project [project]",
      ->(opts, v) { opts.bq_project = v},
      "Project containing the CDR version. Required."
    )
  op.add_option(
    "--bq-dataset [dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "Dataset for the CDR version. Required."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset }
  op.parse.validate

  # TODO: add controlled authorization domains here later; choose controlled vs. registered based
  # on data access level of CDR version.
  if op.opts.bq_project == "all-of-us-ehr-dev"
    # We include prod in here for now since it uses synthetic data. We might remove this in future.
    authorization_domains = ["all-of-us-registered-prod@firecloud.org",
                             "GROUP_all-of-us-registered-stable@firecloud.org",
                             "GROUP_all-of-us-registered-staging@firecloud.org",
                             "GROUP_all-of-us-registered-test@dev.test.firecloud.org"]
  elsif op.opts.bq_project == "aou-res-curation-prod"
    authorization_domains = ["all-of-us-registered-prod@firecloud.org"]
  else
    raise ArgumentError.new("bq-project must be all-of-us-ehr-dev (synthetic) or aou-res-curation-prod (prod)")
  end

  common = Common.new
  config_file = Tempfile.new("#{op.opts.bq_dataset}-config.json")
  begin
    common.run_inline %{bq show --format=prettyjson #{op.opts.bq_project}:#{op.opts.bq_dataset} > #{config_file.path}}
    json = JSON.parse(File.read(config_file.path))
    existing_groups = Set[]
    for entry in json["access"]
      if entry.key?("groupByEmail")
        existing_groups.add(entry["groupByEmail"])
      end
    end
    for domain in authorization_domains
      if existing_groups.include?(domain)
        puts "#{domain} already in ACL, skipping..."
      else
        puts "Adding #{domain} to ACL..."
        new_entry = { "groupByEmail" => domain, "role" => "READER"}
        json["access"].push(new_entry)
      end
    end
    File.open(config_file.path, "w") do |f|
      f.write(JSON.pretty_generate(json))
    end
    common.run_inline %{bq update --source #{config_file.path} #{op.opts.bq_project}:#{op.opts.bq_dataset}}
  ensure
    config_file.unlink
  end
end

Common.register_command({
  :invocation => "update-bq-acl",
  :description => "Updates the BigQuery dataset ACL for a CDR version to have the appropriate FireCloud groups on it.",
  :fn => ->(*args) { update_bq_acl("update-bq-acl", args) }
})
