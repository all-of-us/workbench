#!/usr/bin/ruby
#
# A wrapper script to call reporting-wizard repeatedly on all Workbench MySQL tables. This will send
# generated files to a specified output directory. Typically, output is sent to a scratch space
# and then files / snippets are manually copied to the right target location (e.g. Swagger schema
# files, Java code, or JSON schema files in workbench-terraform-modules).
#
# Example invocation:
#
# > cd workbench/api/reporting/schemas
# > ruby generate_all_tables.rb ./input /tmp/reporting-output
# > cat /tmp/reporting-output/big_query_json/workspace.json
#
require 'open3'

input_dir = File.expand_path(ARGV[0])
describe_csv_dir = File.join(input_dir, 'mysql_describe_csv')
output_dir = File.expand_path(ARGV[1])

def table_name(filename)
  File.basename(filename, '.csv')
end

Dir.each_child(describe_csv_dir) do |file|
  full_cmd = "./reporting-wizard.rb #{table_name(file)} #{input_dir} #{output_dir}"
  _stdout, output, _status = Open3.capture3(full_cmd)
  puts output if output
  puts _stdout if _stdout.strip
end
