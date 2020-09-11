#!/usr/bin/ruby
require 'csv'
require 'json'
require 'yaml'

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

dto_class_name = "BqDto#{to_camel_case(table_name, true)}"

inputs = {
    :describe_csv => to_input_path(File.join(input_dir, 'mysql_describe_csv'), table_name,'csv'),
    :exclude_columns => to_input_path(File.join(input_dir, 'excluded_columns'), table_name,'txt')
}

outputs = {
    :big_query_json => to_output_path(File.join(output_dir, 'big_query_json'), table_name,'json'),
    :swagger_yaml => to_output_path(File.join(output_dir, 'swagger_yaml'), table_name,'yaml'),
    :projection_interface => to_output_path(File.join(output_dir, 'projection_interface'), table_name, 'java'),
    :projection_query => to_output_path(File.join(output_dir, 'projection_query'), table_name,'java')
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

def is_valid_field?(excluded_fields, field)
  result = !excluded_fields.include?(field)
  result
end

## BigQuery schema
describe_rows = CSV.new(File.read(inputs[:describe_csv])).sort_by { |row| row[0]  }

columns = describe_rows.filter{ |row| is_valid_field?(excluded_fields, row[0])} \
  .map{ |row| {:name => row[0], :mysql_type => row[1], :big_query_type => to_bq_type(row[1])} }

big_query_schema = columns.map{ |col| { :name => col[:name], :type => col[:big_query_type]} }
schema_json = JSON.pretty_generate(big_query_schema)

IO.write(outputs[:big_query_json], schema_json)
puts "  Spring BigQuery Schema: #{outputs[:big_query_json]}"

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
        'type'  => 'integer',
        'format' => 'int64'
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

swagger_object =  {  dto_class_name => {
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
IO.write(outputs[:swagger_yaml], indented_yaml)
puts "  DTO Swagger Definition to #{outputs[:swagger_yaml]}"

### Projection Interface

BIGQUERY_TYPE_TO_JAVA  = {
    'STRING' => 'String',
    'INT64' => 'long',
    'TIMESTAMP' =>  'Timestamp',
    'BOOLEAN' =>  'boolean',
    'FLOAT64' => 'double'
}

def to_getter(field)
  "  #{BIGQUERY_TYPE_TO_JAVA[field[:big_query_type]]} get#{to_camel_case(field[:name], true)}();"
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

IO.write(outputs[:projection_interface], java)
puts "  Spring Data Projection Interface: #{outputs[:projection_interface]}"

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
IO.write(outputs[:projection_query], sql)
puts "  Projection Query: #{outputs[:projection_query]}"
