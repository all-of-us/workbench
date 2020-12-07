require 'ostruct'

class ReportingTable

  attr_reader :dto_class, :entity_class, :instance_name, :alias, :test_fixture_class, :output_dir,
    :name

  # Treat table as struct backing this thing
  def initialize(table, output_dir)
    self.dto_class = table.dto_class
    self.entity_class = table.entity_class
    self.output_dir = output_dir
  end

  # All output files for a given table go in a single directory.
  # OUTPUTS = {
  #     :big_query_json => to_output_path('big_query_json', 'json'),
  #     :swagger_yaml => to_output_path('swagger_yaml', 'yaml'),
  #     :projection_interface => to_output_path('projection_interface',  'java'),
  #     :projection_query => to_output_path('projection_query', 'java'),
  #     :unit_test_constants => to_output_path('unit_test_constants',  'java'),
  #     :unit_test_mocks => to_output_path('unit_test_mocks',  'java'),
  #     :dto_assertions => to_output_path('dto_assertions',  'java'),
  #     :bigquery_insertion_payload_transformer => to_output_path('query_parameter_columns', 'java'),
  #     :dto_decl => to_output_path('dto_decl',  'java'),
  #     :entity_decl => to_output_path('entity_decl',  'java'),
  #     :yaml_template => to_output_path('yaml_template', 'yaml'),
  #     :yaml_everything => to_output_path('yaml_everything', 'yaml')
  # }.freeze
  def to_output_path(subdir_name, suffix)
    dir_name = File.join(output_dir, subdir_name)
    Dir.mkdir(dir_name) unless Dir.exist?(dir_name)
    File.expand_path(File.join(dir_name, "#{self.name}.#{suffix}"))
  end

  def write_all(top_output_dir)
    # make directory for this table
    Dir.mkdir_p(output_dir, table.name)
  end

  def write_output(path, contents, description)
    IO.write(path, contents)
    puts "  #{description}: #{path}"
  end

  #
  # EXAMPLE INPUT (yaml)
  #
  # last_modified_time:
  #   description: Time of last user modification of this concept set.
  #   name: last_modified_time
  #   mysql_type: datetime
  #   entity_alias: c.lastModifiedTime
  #   is_enum: false

  # EXAMPLE OUTPUT (yaml):
  # last_modified_time:
  #     big_query_type: TIMESTAMP
  # default_enum_value:
  #     description: Time of last user modification of this concept set.
  #     getter: getLastModifiedTime
  # is_enum: false
  # java_constant_name: CONCEPT_SET__LAST_MODIFIED_TIME
  # java_field_name: lastModifiedTime
  # mysql_type: datetime
  # name: last_modified_time
  # projection_query_item: c.lastModifiedTime
  # projection_type: Timestamp
  # setter: setLastModifiedTime
  # swagger_property_name: lastModifiedTime
  # swagger_type:
  #     type: string
  #     format: date-time

end
