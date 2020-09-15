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

inputs = {
    :describe_csv => to_input_path(File.join(input_dir, 'mysql_describe_csv'), table_name,'csv'),
    :exclude_columns => to_input_path(File.join(input_dir, 'excluded_columns'), table_name,'txt')
}

outputs = {
    :big_query_json => to_output_path(File.join(output_dir, 'big_query_json'), table_name,'json'),
    :swagger_yaml => to_output_path(File.join(output_dir, 'swagger_yaml'), table_name,'yaml'),
    :projection_interface => to_output_path(File.join(output_dir, 'projection_interface'), table_name, 'java'),
    :projection_query => to_output_path(File.join(output_dir, 'projection_query'), table_name,'java'),
    :unit_test_constants => to_output_path(File.join(output_dir, 'unit_test_constants'), table_name, 'java'),
    :unit_test_mocks => to_output_path(File.join(output_dir, 'unit_test_mocks'), table_name, 'java'),
    :dto_assertions => to_output_path(File.join(output_dir, 'dto_assertions'), table_name, 'java'),
    :query_parameter_columns => to_output_path(File.join(output_dir, 'query_parameter_columns'), table_name, 'java'),
    :dto_decl => to_output_path(File.join(output_dir, 'dto_decl'), table_name, 'java')
}

MYSQL_TO_BIGQUERY_TYPE = {
    'varchar' => 'STRING',
    'datetime' => 'TIMESTAMP',
    'bigint' => 'INT64',
    'smallint' => 'INT64',
    'longtext' => 'STRING',
    'int' => 'INT64',
    'tinyint' => 'INT64',
    'bit' => 'BOOLEAN',
    'double' => 'FLOAT64',
    'text' => 'STRING',
    'mediumblob' => 'STRING'
}


BIGQUERY_TYPE_TO_JAVA  = {
    'STRING' => 'String',
    'INT64' => 'long',
    'TIMESTAMP' =>  'Timestamp',
    'BOOLEAN' =>  'boolean',
    'FLOAT64' => 'double'
}

def to_bq_type(mysql_type)
  type_pattern = Regexp.new("(?<type>\\w+)(\\(\\d+\\))?")
  match_data = mysql_type.match(type_pattern)
  result = MYSQL_TO_BIGQUERY_TYPE[match_data[:type]]
  raise "MySQL type #{mysql_type} not recognized." if result.nil?
  result
end

excluded_fields = File.exist?(inputs[:exclude_columns]) \
  ? File.readlines(inputs[:exclude_columns]) \
      .map{ |c| c.strip } \
  : []

def include_field?(excluded_fields, field)
  !excluded_fields.include?(field)
end

## BigQuery schema
describe_rows = CSV.new(File.read(inputs[:describe_csv])).sort_by { |row| row[0]  }

dto_name = "BqDto#{to_camel_case(table_name, true)}"

columns = describe_rows.filter{ |row| include_field?(excluded_fields, row[0])} \
  .map{ |row| \
    col_name = row[0]
    big_query_type = to_bq_type(row[1])
    {
        :name => col_name, \
        :lambda_var => table_name[0].downcase, \
        :mysql_type => row[1], \
        :big_query_type => big_query_type, \
        :java_type => BIGQUERY_TYPE_TO_JAVA[big_query_type], \
        :java_field_name => "#{to_camel_case(col_name, false)}", \
        :java_constant_name => "#{table_name.upcase}__#{col_name.upcase}", \
        :getter => "get#{to_camel_case(col_name, true)}"
    }}

big_query_schema = columns.map{ |col| { :name => col[:name], :type => col[:big_query_type]} }
schema_json = JSON.pretty_generate(big_query_schema)

def write_output(path, contents, description)
  IO.write(path, contents)
  puts "  #{description}: #{path}"
end

write_output(outputs[:big_query_json], schema_json, 'Spring BigQuery Schema')

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
}

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

def to_swagger_property(column)
  { 'description' => column[:description] || '' } \
    .merge(BIGQUERY_TYPE_TO_SWAGGER[column[:big_query_type]])
end

swagger_object =  {  dto_name => {
    'type'  =>  'object',
    'properties' => columns.to_h  { |field|
      [to_property_name(field[:name]), to_swagger_property(field)]
    }
  }
}

indented_yaml = swagger_object.to_yaml \
  .split("\n") \
  .reject{ |line| '---'.eql?(line)} \
  .map{ |line| '  ' + line } \
  .join("\n")

write_output(outputs[:swagger_yaml], indented_yaml, 'DTO Swagger Definition')

### Projection Interface
def to_getter(field)
  "  #{field[:java_type]} get#{to_camel_case(field[:name], true)}();"
end

getters = columns.map { |field|
  to_getter(field)
}

def projection_name(table_name)
  "Prj#{to_camel_case(table_name, true )}"
end

java = "public interface #{projection_name(table_name)} {\n"
java << getters.join("\n")
java << "\n}\n"

write_output(outputs[:projection_interface], java, 'Spring Data Projection Interface')

### Projection query
def hibernate_column_name(field)
  to_camel_case(field[:name], false)
end

# Fix up research purpose entity fields, which don't match the column names (i.e. there's no 'rp' prefix)
def adjust_rp_col(field, table_alias)
  md = field.match(/^rp_(?<root>\w+$)/)
  projection_field = to_camel_case(field, false)
  entity_property = projection_field
  if md and md['root']
    entity_property = to_camel_case(md['root'], false)
    "#{table_alias}.#{entity_property} AS #{projection_field}"
  else
    "#{table_alias}.#{entity_property}"
  end
end

  def to_query(table_name, schema)
  table_alias = table_name[0].downcase
  "@Query(\"SELECT\\n\"\n" \
    + schema.map do |field|
      "+ \"  #{adjust_rp_col(field[:name], table_alias)}"
    end \
    .join(",\\n\"\n") \
    + "\\n\"\n" \
    + "+ \"FROM Db#{to_camel_case(table_name, true)} #{table_alias}\")\n" \
    + "  List<#{projection_name(table_name)}> getReporting#{to_camel_case(table_name, true)}s();"
end

sql = to_query(table_name, columns)

write_output(outputs[:projection_query], sql, 'Projection Query')

# Unit Test Constants
#
JAVA_TYPE_TO_DEFAULT = {
    'String' => '"foo"',
    'int' => '42',
    'long' => '1001',
    'double' => '5.2',
    'Timestamp' => 'Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"))',
    'boolean' => 'false'
}
BASE_TIMESTAMP = Time.new(2015, 5, 5)
TIMESTAMP_DELTA_SECONDS = 24 * 60 * 60 # seconds in day

def to_constant_declaration(column, index)
  value = case column[:java_type]
          when 'String'
            "\"foo_#{index}\""
          when 'int'
            "%d" % [index]
          when 'long'
            "%dL" % [index]
          when 'double'
            "%f" % [index + 0.5]
          when 'boolean'
            index.even? # just flip it every time
          when 'Timestamp'
            # add a day times the index to base timestamp
            timestamp = BASE_TIMESTAMP + TIMESTAMP_DELTA_SECONDS * index
            "Timestamp.from(Instant.parse(\"#{timestamp.strftime("%Y-%m-%dT00:00:00.00Z")}\"))"
          else
            index.to_s
          end
  "private static final #{column[:java_type]} #{column[:java_constant_name]} = #{value};"
end

constants = columns.enum_for(:each_with_index) \
  .map { |col, index| to_constant_declaration(col, index) } \
  .join("\n")

write_output(outputs[:unit_test_constants], constants, 'Unit Test Constants')

### Mock Instantiation
# Mock the projection interface for testing with mock services exposing them
mockName = "mock#{to_camel_case(table_name, true)}"
mocks = columns.map { |col|
  "doReturn(#{col[:java_constant_name]}).when(#{mockName}).#{col[:getter]}();"
}

lines = []
lines << "final #{projection_name(table_name)} #{mockName} = mock(#{projection_name(table_name)}.class);"
lines << mocks
lines.flatten!

write_output(outputs[:unit_test_mocks], lines.join("\n"), 'Unit Test Mocks')

### Assertions
dto_assertions = columns.map{ |col|
  getter_call = "#{to_camel_case(table_name, false)}.#{col[:getter]}()"
  expected = col[:java_constant_name]
  if col[:java_type].eql?('Timestamp')
    "    assertTimeApprox(#{getter_call}, #{expected});"
  else
    "    assertThat(#{getter_call}).isEqualTo(#{expected});"
  end

}.join("\n")

write_output(outputs[:dto_assertions], dto_assertions, 'Unit Test DTO Assertions')

### Parameter Column Enum
def object_value_function(col, dto_name)
  case col[:java_type]
  when 'Timestamp' # actually OffsetDateTime on the DTO
    "#{col[:lambda_var]} -> toInsertRowString(#{col[:lambda_var]}.#{col[:getter]}())"
  else
    "#{dto_name}::#{col[:getter]}"
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
  case col[:java_type]
  when 'Timestamp'
    "#{col[:lambda_var]} -> #{JAVA_TYPE_TO_QPV_FACTORY[col[:java_type]]}(#{col[:lambda_var]}.#{col[:getter]}())"
  else
    "#{col[:lambda_var]} -> #{JAVA_TYPE_TO_QPV_FACTORY[col[:java_type]]}(#{col[:lambda_var]}.#{col[:getter]}())"
  end
end

def query_parameter_column_entry(col, dto_name)
  "#{col[:name].upcase}(\"#{col[:name]}\", #{object_value_function(col, dto_name)}, #{qpv_function(col)})"
end

qpc_enum = columns.map do |col|
  query_parameter_column_entry(col, dto_name)
end.join(",\n")

write_output(outputs[:query_parameter_columns], qpc_enum, "QueryParameterValue Enum Entries")

### DTO Fluent Setters
#
dto_setters = columns.map do |col|
  value = case col[:java_type]
      when 'Timestamp'
            "offsetDateTimeUtc(#{col[:java_constant_name]})"
          else
            "#{col[:java_constant_name]}"
          end
  "    .#{col[:java_field_name]}(#{value})"
end

dto_decl = ["new #{dto_name}()"]
dto_decl << dto_setters

dto_decl_str = dto_decl.join("\n") + ';'
write_output(outputs[:dto_decl], dto_decl_str, "DTO Object Construction")
