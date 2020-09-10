#!/usr/bin/ruby

# Ugly wrapper script for reporting-codegen.rb. Just use all the files in the csv directory
input_dir = File.expand_path(ARGV[0])
describe_csv_dir = File.join(input_dir, 'mysql_describe_csv')
output_dir = File.expand_path(ARGV[1])

def table_name(filename)
  File.basename(filename, '.csv')
end

Dir.each_child(describe_csv_dir) do |file|
  `./reporting-codegen.rb #{table_name(file)} #{input_dir} #{output_dir}`
end
