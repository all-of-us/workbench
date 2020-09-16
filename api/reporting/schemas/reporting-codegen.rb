#!/usr/bin/ruby
require 'csv'
require 'json'
require 'yaml'
require 'enumerator'

table_name = ARGV[0]
input_dir = File.expand_path(ARGV[1])
output_dir = File.expand_path(ARGV[2])
puts "Generate types for #{table_name}..."
Dir.mkdir(output_dir) unless Dir.exist?(output_dir)

def to_camel_case(snake_case, capitalize_initial)
  result =  snake_case.split('_').collect(&:capitalize).join
  unless capitalize_initial
    result[0] = result[0].downcase
  end
  result
end

def to_input_path(dir_name, table_name, suffix)
  File.expand_path(File.join(dir_name, "#{table_name}.#{suffix}"))
end

def to_output_path(dir_name, table_name, suffix)
  Dir.mkdir(dir_name) unless Dir.exist?(dir_name)
  File.expand_path(File.join(dir_name, "#{table_name}.#{suffix}"))
end

INPUTS = {
    :describe_csv => to_input_path(File.join(input_dir, 'mysql_describe_csv'), table_name,'csv'),
    :excluded_columns => to_input_path(File.join(input_dir, 'excluded_COLUMNS'), table_name,'txt')
}.freeze

OUTPUTS = {
    :big_query_json => to_output_path(File.join(output_dir, 'big_query_json'), table_name,'json'),
    :swagger_yaml => to_output_path(File.join(output_dir, 'swagger_yaml'), table_name,'yaml'),
    :projection_interface => to_output_path(File.join(output_dir, 'projection_interface'), table_name, 'java'),
    :projection_query => to_output_path(File.join(output_dir, 'projection_query'), table_name,'java'),
    :unit_test_constants => to_output_path(File.join(output_dir, 'unit_test_constants'), table_name, 'java'),
    :unit_test_mocks => to_output_path(File.join(output_dir, 'unit_test_mocks'), table_name, 'java'),
    :dto_assertions => to_output_path(File.join(output_dir, 'dto_assertions'), table_name, 'java'),
    :query_parameter_COLUMNS => to_output_path(File.join(output_dir, 'query_parameter_COLUMNS'), table_name, 'java'),
    :dto_decl => to_output_path(File.join(output_dir, 'dto_decl'), table_name, 'java'),
    :entity_decl => to_output_path(File.join(output_dir, 'entity_decl'), table_name, 'java'),
}.freeze

# This is the canonical type map, but there are places where we assign a tinyint MySql column a Long Entity field, etc.
MYSQL_TO_TYPES = {
    'varchar' => {
        :bigquery => 'STRING',
        :java => 'String'
    },
    'datetime' => {
        :bigquery => 'TIMESTAMP',
        :java => 'Timestamp'
    },
    'bigint' => {
        :bigquery => 'INT64',
        :java => 'Long'
    },
    'smallint' => {
        :bigquery => 'INT64',
        :java => 'Short'
    },
    'longtext' => {
        :bigquery => 'STRING',
        :java => 'String'
    },
    'int' => {
        :bigquery => 'INT64',
        :java => 'Integer'
    },
    'tinyint' => {
        :bigquery => 'INT64',
        :java => 'Short'
    },
    'bit' => {
        :bigquery => 'BOOLEAN',
        :java => 'Boolean'
    },
    'double' => {
        :bigquery =>  'FLOAT64',
        :java => 'Double'
    },
    'text' => {
        :bigquery => 'STRING',
        :java => 'String'
    },
    'mediumblob' => {
        :bigquery => 'STRING',
        :java => 'String'
    }
}.freeze


BIGQUERY_TYPE_TO_JAVA  = {
    'STRING' => 'String',
    'INT64' => 'long',
    'TIMESTAMP' =>  'Timestamp',
    'BOOLEAN' =>  'boolean',
    'FLOAT64' => 'double'
}.freeze

# strip size/kind
def simple_mysql_type(mysql_type)
  type_pattern = Regexp.new("(?<type>\\w+)(\\(\\d+\\))?")
  match_data = mysql_type.match(type_pattern)
  result = match_data[:type]
  raise "MySQL type #{mysql_type} not recognized." if result.nil?
  result
end

def to_bq_type(mysql_type)
  MYSQL_TO_TYPES[simple_mysql_type(mysql_type)][:bigquery]
end

excluded_fields = File.exist?(INPUTS[:excluded_columns]) \
  ? File.readlines(INPUTS[:excluded_columns]) \
      .map{ |c| c.strip } \
  : []

def include_field?(excluded_fields, field)
  !excluded_fields.include?(field)
end

## BigQuery schema
describe_rows = CSV.new(File.read(INPUTS[:describe_csv])).sort_by { |row| row[0]  }

root_class_name = to_camel_case(table_name, true)
TABLE_INFO = {
    :name => table_name,
    :instance_name => to_camel_case(table_name, false),
    :dto_class => "BqDto#{root_class_name}",
    :entity_class => "Db#{root_class_name}",
    :mock => "mock#{root_class_name}",
    :projection_interface => "Prj#{root_class_name}",
    :sql_alias => table_name[0].downcase
}.freeze


def to_swagger_name(snake_case, is_class_name)
  result =  snake_case.split('_').collect(&:capitalize).join
  unless is_class_name
    result[0] = result[0].downcase
  end
  result
end

def to_property_name(column_name)
  to_swagger_name(column_name, false)
end

COLUMNS = describe_rows.filter{ |row| include_field?(excluded_fields, row[0]) } \
  .map{ |row| \
    col_name = row[0]
    mysql_type = simple_mysql_type(row[1])
    type_info = MYSQL_TO_TYPES[mysql_type]
    {
        :name => col_name, \
        :lambda_var => table_name[0].downcase, \
        :mysql_type => mysql_type, \
        :big_query_type => type_info[:bigquery], \
        :java_type => type_info[:java], \
        :java_field_name => "#{to_camel_case(col_name, false)}", \
        :java_constant_name => "#{table_name.upcase}__#{col_name.upcase}", \
        :getter => "get#{to_camel_case(col_name, true)}",
        :setter => "set#{to_camel_case(col_name, true)}",
        :property => to_property_name(col_name)
    }
}.freeze

big_query_schema = COLUMNS.map{ |col| { :name => col[:name], :type => col[:big_query_type]} }
schema_json = JSON.pretty_generate(big_query_schema)

def write_output(path, contents, description)
  IO.write(path, contents)
  puts "  #{description}: #{path}"
end

write_output(OUTPUTS[:big_query_json], schema_json, 'BigQuery JSON Schema')

## Swagger DTO Objects
BIGQUERY_TYPE_TO_SWAGGER  = {
    'STRING' =>  {
        'type'  => 'string'
    },
    'INT64' => {
        'type'  => 'integer',
        'format' => 'int64'
    },
    'TIMESTAMP' =>  {
        'type' => 'string',
        'format' => 'date-time'
    },
    'BOOLEAN' =>  {
        'type'  => 'boolean',
        'default' => false
    },
    'FLOAT64' => {
        'type' =>  'number',
        'format' =>  'double'
    }
}.freeze


def to_swagger_property(column)
  { 'description' => column[:description] || '' } \
    .merge(BIGQUERY_TYPE_TO_SWAGGER[column[:big_query_type]])
end

swagger_object =  {  TABLE_INFO[:dto_class] => {
    'type'  =>  'object',
    'properties' => COLUMNS.to_h  { |field|
      [field[:property], to_swagger_property(field)]
    }
  }
}

indented_yaml = swagger_object.to_yaml \
  .split("\n") \
  .reject{ |line| '---'.eql?(line)} \
  .map{ |line| '  ' + line } \
  .join("\n")

write_output(OUTPUTS[:swagger_yaml], indented_yaml, 'DTO Swagger Definition')

### Projection Interface
def to_getter(field)
  "  #{field[:java_type]} get#{to_camel_case(field[:name], true)}();"
end

getters = COLUMNS.map { |field|
  to_getter(field)
}


java = "public interface #{TABLE_INFO[:projection_interface]} {\n"
java << getters.join("\n")
java << "\n}\n"

write_output(OUTPUTS[:projection_interface], java, 'Spring Data Projection Interface')

### Projection query
def hibernate_column_name(field)
  to_camel_case(field[:name], false)
end

# Fix up research purpose entity fields, which don't match the column names (i.e. there's no 'rp' prefix)
def adjust_rp_col(field)
  md = field.match(/^rp_(?<root>\w+$)/)
  projection_field = to_camel_case(field, false)
  entity_property = projection_field
  if md and md['root']
    entity_property = to_camel_case(md['root'], false)
    "#{TABLE_INFO[:sql_alias]}.#{entity_property} AS #{projection_field}"
  else
    "#{TABLE_INFO[:sql_alias]}.#{entity_property}"
  end
end

def to_query()
  "@Query(\"SELECT\\n\"\n" \
    + COLUMNS.map do |field|
      "+ \"  #{adjust_rp_col(field[:name])}"
    end \
    .join(",\\n\"\n") \
    + "\\n\"\n" \
    + "+ \"FROM #{TABLE_INFO[:entity_class]} #{TABLE_INFO[:sql_alias]}\")\n" \
    + "  List<#{TABLE_INFO[:projection_interface]}> getReporting#{to_camel_case(TABLE_INFO[:name], true)}s();"
end

sql = to_query

write_output(OUTPUTS[:projection_query], sql, 'Projection Query')

# Unit Test Constants
#
BASE_TIMESTAMP = Time.new(2015, 5, 5).freeze
TIMESTAMP_DELTA_SECONDS = (24 * 60 * 60).freeze # seconds in day

# N.B. some Short fields are only valid up to the number of associated enum values - 1. Fixing these
# up by hand for now.
def to_constant_declaration(column, index)
  value = case column[:java_type]
          when 'String'
            "\"foo_#{index}\""
          when 'Integer'
            "%d" % [index]
          when 'Long'
            "%dL" % [index]
          when 'Double'
            "%f" % [index + 0.5]
          when 'Boolean'
            index.even? # just flip it every time
          when 'Timestamp'
            # add a day times the index to base timestamp
            timestamp = BASE_TIMESTAMP + TIMESTAMP_DELTA_SECONDS * index
            "Timestamp.from(Instant.parse(\"#{timestamp.strftime("%Y-%m-%dT00:00:00.00Z")}\"))"
          else
            index.to_s
          end
  "public static final #{column[:java_type]} #{column[:java_constant_name]} = #{value};"
end

constants = COLUMNS.enum_for(:each_with_index) \
  .map { |col, index| to_constant_declaration(col, index) } \
  .join("\n")

write_output(OUTPUTS[:unit_test_constants], constants, 'Unit Test Constants')

### Mock Instantiation
# Mock the projection interface for testing with mock services exposing them
mocks = COLUMNS.map { |col|
  "doReturn(#{col[:java_constant_name]}).when(#{TABLE_INFO[:mock]}).#{col[:getter]}();"
}

lines = []
lines << "final #{TABLE_INFO[:projection_interface]} #{TABLE_INFO[:mock]} = mock(#{TABLE_INFO[:projection_interface]}.class);"
lines << mocks
lines.flatten!

write_output(OUTPUTS[:unit_test_mocks], lines.join("\n"), 'Unit Test Mocks')

### Assertions
dto_assertions = COLUMNS.map{ |col|
  getter_call = "#{to_camel_case(table_name, false)}.#{col[:getter]}()"
  expected = col[:java_constant_name]
  if col[:java_type].eql?('Timestamp')
    "    assertTimeApprox(#{getter_call}, #{expected});"
  else
    "    assertThat(#{getter_call}).isEqualTo(#{expected});"
  end

}.join("\n")

write_output(OUTPUTS[:dto_assertions], dto_assertions, 'Unit Test DTO Assertions')

### Parameter Column Enum
def object_value_function(col)
  case col[:java_type]
  when 'Timestamp' # actually OffsetDateTime on the DTO
    "#{col[:lambda_var]} -> toInsertRowString(#{col[:lambda_var]}.#{col[:getter]}())"
  else
    "#{TABLE_INFO[:dto_class]}::#{col[:getter]}"
  end
end

JAVA_TYPE_TO_QPV_FACTORY = {
    'int' => 'QueryParameterValue.int64',
    'long' => 'QueryParameterValue.int64',
    'String' => 'QueryParameterValue.string',
    'double' => 'QueryParameterValue.float64',
    'boolean' => 'QueryParameterValue.bool',
    'Timestamp' => 'toTimestampQpv'
}

def qpv_function(col)
  "#{col[:lambda_var]} -> #{JAVA_TYPE_TO_QPV_FACTORY[col[:java_type]]}(#{col[:lambda_var]}.#{col[:getter]}())"
end

def query_parameter_column_entry(col)
  "#{col[:name].upcase}(\"#{col[:name]}\", #{object_value_function(col)}, #{qpv_function(col)})"
end

QPC_ENUM = COLUMNS.map do |col|
  query_parameter_column_entry(col)
end.join(",\n").freeze

write_output(OUTPUTS[:query_parameter_COLUMNS], QPC_ENUM, "QueryParameterValue Enum Entries")

### DTO Fluent Setters
DTO_SETTERS = COLUMNS.map do |col|
  value = case col[:java_type]
      when 'Timestamp'
            "offsetDateTimeUtc(#{col[:java_constant_name]})"
          else
            "#{col[:java_constant_name]}"
          end
  "    .#{col[:java_field_name]}(#{value})"
end.freeze

DTO_DECL = ["new #{TABLE_INFO[:dto_class]}()"]
DTO_DECL << DTO_SETTERS

DTO_DECL_STR = DTO_DECL.join("\n") + ';'
write_output(OUTPUTS[:dto_decl], DTO_DECL_STR, "DTO Object Construction")

### Entity Field Settings for DAO Test
ENTITY_DECL = (["final #{TABLE_INFO[:entity_class]} #{TABLE_INFO[:instance_name]} = new #{TABLE_INFO[:entity_class]}();"] \
  << COLUMNS.map do |col|
    "#{TABLE_INFO[:instance_name]}.#{col[:setter]}(#{col[:java_constant_name]});"
  end) \
.join("\n").freeze

write_output(OUTPUTS[:entity_decl], ENTITY_DECL, "Entity Class Instance Declaration")
