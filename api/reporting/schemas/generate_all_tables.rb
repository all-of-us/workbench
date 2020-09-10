#!/usr/bin/ruby
require 'open3'

# Ugly wrapper script for reporting-codegen.rb. Just use all the files in the csv directory
input_dir = File.expand_path(ARGV[0])
describe_csv_dir = File.join(input_dir, 'mysql_describe_csv')
output_dir = File.expand_path(ARGV[1])

def table_name(filename)
  File.basename(filename, '.csv')
end

Dir.each_child(describe_csv_dir) do |file|
  full_cmd = "./reporting-codegen.rb #{table_name(file)} #{input_dir} #{output_dir}"
  _stdout, output, _status = Open3.capture3(full_cmd)
  puts output if output
  puts _stdout if _stdout.strip
end
