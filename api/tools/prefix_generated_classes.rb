#!/usr/bin/env ruby
# frozen_string_literal: true
require 'find'

# usage:  ./prefix_generated_classes.rb ../api org.pmiops Firecloud
ROOT_DIR = ARGV[0]
PACKAGE = ARGV[1]
PREFIX = ARGV[2]
IMPORT_PATTERN = "import #{PACKAGE}."
IMPORT_MARKER = '8675309867530986753098675309'
NEW_IMPORT_PATTERN = "#{IMPORT_PATTERN}#{PREFIX}"
QUALIFIED_USAGE_PATTERN = "#{PACKAGE}."
FILE_SEPARATOR = '--------------------------------------------------------------------------------'
DRY_RUN = ARGV[3] ? true : false

puts "Adding prefix #{PREFIX} to all classes in package #{PACKAGE} under #{ROOT_DIR}"

java_sources = []

Find.find(ROOT_DIR) do |path|
  java_sources << path if path =~ /.*\.java$/
end

puts "Found #{java_sources.length} java source files."

java_sources.each do |source_file|
  puts "\tProcessing #{source_file}"
  original = File.read(source_file)

  # mask out the import statements so the next replacement doesn't stomp on them
  replaced_import = original.gsub(IMPORT_PATTERN, IMPORT_MARKER)

  # we don't need to qualify the class name anymore, since it won't clash
  repalced_qualified_imports = replaced_import.gsub(QUALIFIED_USAGE_PATTERN, PREFIX)

  # fixup the import
  final_text = repalced_qualified_imports.gsub(IMPORT_MARKER, NEW_IMPORT_PATTERN)
  if DRY_RUN
    puts FILE_SEPARATOR
    puts final_text
    puts FILE_SEPARATOR
  else
    File.open(source_file, "w") {|file| file.puts final_text}
  end
end
