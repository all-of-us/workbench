#!/usr/bin/ruby

require 'active_support/core_ext/hash/keys'
require 'csv'
require 'enumerator'
require 'json'
require 'ostruct'
require 'time'
require 'yaml'

TABLE_NAME = ARGV[0]
INPUT_DIR = File.expand_path(ARGV[1])
OUTPUT_DIR = File.expand_path(ARGV[2])
puts "Generate types for #{TABLE_NAME}..."
Dir.mkdir(OUTPUT_DIR) unless Dir.exist?(OUTPUT_DIR)

def to_camel_case(snake_case, capitalize_initial)
  result =  snake_case.split('_').collect(&:capitalize).join
  unless capitalize_initial
    result[0] = result[0].downcase
  end
  result
end

def to_input_path(dir_name, suffix)
  File.expand_path(File.join(dir_name, "#{TABLE_NAME}.#{suffix}"))
end

def to_output_path(subdir_name, suffix)
  dir_name = File.join(OUTPUT_DIR, subdir_name)
  Dir.mkdir(dir_name) unless Dir.exist?(dir_name)
  File.expand_path(File.join(dir_name, "#{TABLE_NAME}.#{suffix}"))
end

INPUTS = {
    :describe_csv => to_input_path(File.join(INPUT_DIR, 'mysql_describe_csv'), 'csv'),
    :excluded_columns => to_input_path(File.join(INPUT_DIR, 'excluded_COLUMNS'), 'txt'),
    :descriptions => to_input_path(File.join(INPUT_DIR, 'descriptions'), 'yaml')
}.freeze

OUTPUTS = {
    :big_query_json => to_output_path('big_query_json', 'json'),
    :swagger_yaml => to_output_path('swagger_yaml', 'yaml'),
    :projection_interface => to_output_path('projection_interface',  'java'),
    :projection_query => to_output_path('projection_query', 'java'),
    :unit_test_constants => to_output_path('unit_test_constants',  'java'),
    :unit_test_mocks => to_output_path('unit_test_mocks',  'java'),
    :dto_assertions => to_output_path('dto_assertions',  'java'),
    :bigquery_insertion_payload_transformer => to_output_path('query_parameter_columns', 'java'),
    :dto_decl => to_output_path('dto_decl',  'java'),
    :entity_decl => to_output_path('entity_decl',  'java'),
    :row_mapper => to_output_path('row_mapper', 'java'),
    :yaml_template => to_output_path('yaml_template', 'yaml'),
    :yaml_everything => to_output_path('yaml_everything', 'yaml')
}.freeze

# N.B.: the bq command-line tool accepts 'INT64' in the schema
# files for integer fields, but the console's "Edit as Text"
# UI for table schemas expects 'INTEGER'. Also, whereas QueryParameterValue
# docs and function names use `int64`, the schema view
# in the console uses 'INTEGER'. From this point forward, I want to  specify
# 'INTEGER' in the schemas so that  the `bq show` output  matches.

# This is the canonical type map, but there are places where we assign a tinyint MySql column a Long
# Entity field, etc.
#
# Some columns are enums in the entity class and are exposed as either an enum value or Short. We need
# to handle this separately.
MYSQL_TYPE_TO_SIMPLE_TYPE = {
    'varchar' => {
        :bigquery => 'STRING',
        :projection => 'String',
        :swagger => {
            'type'  => 'string'
        }
    },
    'datetime' => {
        :bigquery => 'TIMESTAMP',
        :projection => 'Timestamp',
        :swagger => {
            'type' => 'string',
            'format' => 'date-time'
        },
        'dto' => 'OffsetDateTime'
    },
    'bigint' => {
        :bigquery => 'INTEGER',
        :projection => 'Long',
        :swagger => {
            'type'  => 'integer',
            'format' => 'int64'
        }
    },
    'smallint' => {
        :bigquery => 'INTEGER',
        :projection => 'Short',
        :swagger => {
            'type'  => 'integer',
            'format' => 'int32' # Swagger has no int16 type
        }
    },
    'int' => {
        :bigquery => 'INTEGER',
        :projection => 'Integer',
        :swagger => {
            'type'  => 'integer',
            'format' => 'int32'
        },
    },
    'tinyint' => {
        :bigquery => 'INTEGER',
        :projection => 'Short',
        :swagger => {
            'type'  => 'integer',
            'format' => 'int32' # Swagger has no int8 type. This is generally overridden by an enum type anyway.
        },
    },
    'bit' => {
        :bigquery => 'BOOLEAN',
        :projection => 'Boolean',
        :swagger => {
            'type' => 'boolean'
        }
    },
    'double' => {
        :bigquery =>  'FLOAT64',
        :projection => 'Double',
        :swagger => {
            'type' =>  'number',
            'format' =>  'double'
        }
    },
    'text' => {
        :bigquery => 'STRING',
        :projection => 'String',
        :swagger =>  {
            'type'  => 'string'
        }
    },
    'longtext' => {
        :bigquery => 'STRING',
        :projection => 'String',
        :swagger => {
          'type' => 'string'
        },
    },
    'mediumblob' => {
        :bigquery => 'STRING',
        :projection => 'String',
        :swagger => {
            'type'  => 'string'
        },
    }
}.freeze

# Enumerated types should be the enum in Java and the DTO, but a STRING in BQ.
# We could either try to remap to the enum type when writing to BQ, but we still
# have to handle getting the value into the projection, and the only way I've been
# able to do that is to use the exact same type.
#
# All enum values should map to
#   * the type of the @Column-annotated property in the entity class in a Projection interface,
#     usually Short or EnumTypeName
#   * the definition reference of the enum tupe in Swagger, e.g. `"#/definitions/StatusResponse"`
#   * a STRING in BigQuery
ENUM_TYPES = {
     'workspace' => {
        'billing_status' => {
            :projection => 'BillingStatus',
            :swagger => 'BillingStatus',
            :bigquery => 'STRING',
            :default_constant_value => 'BillingStatus.ACTIVE'
        },
        'billing_account_type' => {
            :projection => 'BillingAccountType',
            :swagger => 'BillingAccountType',
            :bigquery => 'STRING',
            :default_constant_value => 'BillingAccountType.FREE_TIER'
        }
     },
     'user' => {
         'data_access_level' => {
             :projection => 'Short',
             :swagger => 'DataAccessLevel',
             :bigquery => 'STRING',
             :default_constant_value => 1 # REGISTERED
         }
     },
     # Unlike some other entity  classes, DbInstitution exposes typed enum values.
     'institution' => {
         'dua_type_enum' => {
             :projection => 'DuaType',
             :swagger => 'DuaType',
             :bigquery => 'STRING',
             :default_constant_value => 'DuaType.MASTER'
         },
         'organization_type_enum' => {
             :projection => 'OrganizationType',
             :swagger => 'OrganizationType',
             :bigquery => 'STRING',
             :default_constant_value => 'OrganizationType.ACADEMIC_RESEARCH_INSTITUTION'
         }
     },
     'user_verified_institutional_affiliation' => {
         'institutional_role_enum' => {
             :projection => 'InstitutionalRole',
             :swagger => 'InstitutionalRole',
             :bigquery => 'STRING',
             :default_constant_value => 'InstitutionalRole.UNDERGRADUATE'
         }
     },
     'concept_set' => {
         'survey' => {
             :projection => 'Surveys',
             :swagger => 'Surveys',
             :bigquery => 'STRING',
             :default_constant_value => 'Surveys.LIFESTYLE'
         }
     }
}.freeze

# strip size/kind
def simple_mysql_type(mysql_type)
  type_pattern = Regexp.new("(?<type>\\w+)(\\(\\d+\\))?")
  match_data = mysql_type.match(type_pattern)
  result = match_data[:type]
  raise "MySQL type #{mysql_type} not recognized." if result.nil?
  result
end

excluded_fields = File.exist?(INPUTS[:excluded_columns]) \
  ? File.readlines(INPUTS[:excluded_columns]) \
      .map{ |c| c.strip } \
  : []

def include_field?(excluded_fields, field)
  !excluded_fields.include?(field)
end

ENTITY_MODIFIED_COLUMNS = {
    'workspace' => {
        'cdr_version_id' => 'cdrVersion.cdrVersionId AS cdrVersionId',
        'creator_id' => 'creator.userId AS creatorId',
        'needs_rp_review_prompt' => 'needsResearchPurposeReviewPrompt AS needsRpReviewPrompt'
    },
    'cohort' => {
        'creator_id' => 'creator.userId AS creatorId'
    },
    'dataset' => {
        'dataset_id' => 'dataSetId AS datasetId'
    }
}.freeze

## BigQuery schema
DESCRIBE_ROWS = CSV.new(File.read(INPUTS[:describe_csv])).sort_by { |row| row[0]  }

root_class_name = to_camel_case(TABLE_NAME, true)
TABLE_HASH = {
    :dto_class => "Reporting#{root_class_name}",
    :entity_class => "Db#{root_class_name}",
    :entity_modified_columns => ENTITY_MODIFIED_COLUMNS[:TABLE_NAME] || {},
    :enum_column_info => ENUM_TYPES[TABLE_NAME] || {},
    :instance_name => to_camel_case(TABLE_NAME, false),
    :lambda_var => TABLE_NAME[0].downcase, # also query alias
    :test_fixture_class => "Reporting#{root_class_name}Fixture",
    :mock => "mock#{root_class_name}",
    :name => TABLE_NAME,
    :projection_interface => "ProjectedReporting#{root_class_name}",
    :sql_alias => TABLE_NAME[0].downcase
}.freeze

def provenance_text
  "This code was generated using #{File.basename($PROGRAM_NAME)} at #{Time.now.iso8601}.\n" +
      "Manual modification should be avoided if possible as this is a one-time generation\n" +
      "and does not run on every build and updates must be merged manually for now."
end

def provenance_yaml
  { 'x-aou-note' => provenance_text }
end

def java_inline_comment(block_text)
  block_text.split("\n").map do |line|
    '// ' + line
end.join("\n") + "\n\n"

end

def provenance_java
  java_inline_comment(provenance_text)
end

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

SWAGGER_TYPE_TO_RESULT_SET_METHOD = {
    'long' => 'getLong',
    'int' => 'getInt',
    'String' => 'getString'
}

DESCRIPTIONS = File.exist?(INPUTS[:descriptions]) ?
                   YAML.load_file(INPUTS[:descriptions])[TABLE_HASH[:name]] :
                   {}

# Fix up research purpose entity fields, which don't match the column names (i.e. there's no 'rp' prefix)
def projection_query_item(field)
  md = field.match(/^rp_(?<root>\w+$)/)
  projection_field = to_camel_case(field, false)
  entity_property = projection_field
  if md and md['root']
    entity_property = to_camel_case(md['root'], false)
    "#{TABLE_HASH[:sql_alias]}.#{entity_property} AS #{projection_field}"
  elsif TABLE_HASH[:entity_modified_columns][field]
    "#{TABLE_HASH[:sql_alias]}.#{TABLE_HASH[:entity_modified_columns][field]}"
  else
    "#{TABLE_HASH[:sql_alias]}.#{entity_property}"
  end
end

# TODO(jaycarlton): use YAML input file (built by output of this thing for current files)
# The COLUMNS hash contains essentially all the transformed representations of a column
# in various languages and forms. One row in the describe output looks like `street_address_1,varchar(95),NO,"",,""`.
# Currently this script only uses the column name and type(length) columns.
COLUMNS = DESCRIBE_ROWS.filter{ |row| include_field?(excluded_fields, row[0]) } \
  .map{ |row| \
    col_name = row[0]
    primitive_type = MYSQL_TYPE_TO_SIMPLE_TYPE[simple_mysql_type(row[1])]
    enum_type_override = TABLE_HASH[:enum_column_info][col_name]
    {
        :big_query_type => enum_type_override ? enum_type_override[:bigquery] : primitive_type[:bigquery],
        :default_enum_value => enum_type_override && enum_type_override[:default_constant_value],
        :description => DESCRIPTIONS[col_name] ? DESCRIPTIONS[col_name].strip : '',
        :getter => "get#{to_camel_case(col_name, true)}",
        :is_enum => !enum_type_override.nil?,
        :java_constant_name => "#{TABLE_NAME.upcase}__#{col_name.upcase}",
        :java_field_name => "#{to_camel_case(col_name, false)}",
        :mysql_type => simple_mysql_type(row[1]),
        :name => col_name,
        :projection_query_item => projection_query_item(col_name),
        :projection_type => enum_type_override ? enum_type_override[:projection] : primitive_type[:projection],
        :setter => "set#{to_camel_case(col_name, true)}",
        :swagger_property_name => to_property_name(col_name),
        :swagger_type => enum_type_override ? enum_type_override[:swagger] : primitive_type[:swagger]
    }
}.freeze

FIXED_COLUMNS = [
    :description => 'Time snapshot was taken, in Epoch milliseconds. Same across all rows and all tables in the snapshot, ' +
        'and uniquely defines a particular snapshot.',
    :name => 'snapshot_timestamp',
    :type => 'INTEGER'
]

BIG_QUERY_SCHEMA = FIXED_COLUMNS.concat(COLUMNS.map do |col|
  { :name => col[:name], :type => col[:big_query_type], :description => col[:description].strip }
end).freeze

schema_json = JSON.pretty_generate(BIG_QUERY_SCHEMA)

def write_output(path, contents, description)
  IO.write(path, contents)
  puts "  #{description}: #{path}"
end

write_output(OUTPUTS[:big_query_json], schema_json, 'BigQuery JSON Schema')

## Swagger DTO Objects

def to_swagger_property(column)
  if column[:is_enum]
    swagger_value = {
        'description' => column[:description],
        '$ref' => "#/definitions/#{column[:swagger_type]}"
  }
  else
    swagger_value = MYSQL_TYPE_TO_SIMPLE_TYPE[column[:mysql_type]][:swagger] \
      .merge({ 'description' => column[:description]})
  end
  swagger_value
end

swagger_object =  {TABLE_HASH[:dto_class] => provenance_yaml.merge({
    'type'  =>  'object',
    'properties' => COLUMNS.to_h  { |col|
      [col[:swagger_property_name], to_swagger_property(col)]
    }
  }) # .merge(provenance_yaml)
}

def indent_yaml(yaml)
  yaml \
  .to_yaml \
  .split("\n") \
  .reject{ |line| '---'.eql?(line)} \
  .map{ |line| '  ' + line } \
  .join("\n")
end

write_output(OUTPUTS[:swagger_yaml], indent_yaml(swagger_object), 'DTO Swagger Definition')

### Projection Interface
def to_getter(field)
  property_type = field[:projection_type]
  "  #{property_type} #{field[:getter]}();"
end

PROJECTION_INFO = "This is a Spring Data projection interface for the Hibernate entity\nclass #{TABLE_HASH[:entity_class]}. " +
    "The properties listed correspond to query results\nthat will be mapped into BigQuery rows in a (mostly) 1:1 fashion.\n" +
    "Fields may not be renamed or reordered or have their types\nchanged unless both the entity class and any queries returning\n" +
    "this projection type are in complete agreement."
projection_decl = java_inline_comment(PROJECTION_INFO)
projection_decl << provenance_java + "\n"
projection_decl << "public interface #{TABLE_HASH[:projection_interface]} {\n"
projection_decl << COLUMNS.map { |field|
  to_getter(field)
}.join("\n")
projection_decl << "\n}\n"

write_output(OUTPUTS[:projection_interface], projection_decl, 'Spring Data Projection Interface')

### Projection query
def hibernate_column_name(field)
  to_camel_case(field[:name], false)
end

query_description = \
"This JPQL query corresponds to the projection interface #{TABLE_HASH[:projection_interface]}. Its\n" +
  "types and argument order must match the column names selected exactly, in name,\n" +
  "type, and order. Note that in some cases a projection query should JOIN one or more\n" +
  "other tables. Currently this is done by hand (with suitable renamings of the other entries\n" +
  " in the projection"
query_comment = java_inline_comment(query_description) + provenance_java
projection_query = query_comment + "\n" +
    "@Query(\"SELECT\\n\"\n" +
    COLUMNS.map do |col| \
      "+ \"  #{col[:projection_query_item]}" \
    end.join(",\\n\"\n") +
    "\\n\"\n" +
    "+ \"FROM #{TABLE_HASH[:entity_class]} #{TABLE_HASH[:sql_alias]}\")\n" +
    "  List<#{TABLE_HASH[:projection_interface]}> getReporting#{to_camel_case(TABLE_HASH[:name], true)}s();"

write_output(OUTPUTS[:projection_query], projection_query, 'Projection Query')

# Unit Test Constants
#
BASE_TIMESTAMP = Time.new(2015, 5, 5).freeze
TIMESTAMP_DELTA_SECONDS = 24 * 60 * 60 # .freeze # seconds in day

# N.B. some Short fields are only valid up to the number of associated enum values - 1. Fixing these
# up by hand for now.
def to_constant_declaration(column, index)
  value = case column[:projection_type]
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
            if column[:is_enum]
              column[:default_enum_value]
            else
              index.to_s
            end
          end
  "public static final #{column[:projection_type]} #{column[:java_constant_name]} = #{value};"
end

TEST_FIXTURE_HEADER = "@Service\n" +
    "public class #{TABLE_HASH[:test_fixture_class]} implements ReportingTestFixture" +
    "<#{TABLE_HASH[:entity_class]}, #{TABLE_HASH[:projection_interface]}, #{TABLE_HASH[:dto_class]}>"

CONSTANT_DESCRIPTIONS = java_inline_comment( \
  "All constant values, mocking statements, and assertions in this file are generated. The values\n" +
     "are chosen so that errors with transposed columns can be caught. \n" +
    "Mapping Short values with valid enums can be tricky, and currently there are\n" +
    "a handful of places where we have to use use a Short in the projection interface but an Enum\n" +
    " type in the model class. An example of such a manual fix is the following:\n" +
    ".dataUseAgreementSignedVersion(USER__DATA_USE_AGREEMENT_SIGNED_VERSION.longValue())")

CONSTANTS = CONSTANT_DESCRIPTIONS + "\n\n" + provenance_java + "\n" +
    COLUMNS.enum_for(:each_with_index) \
      .map { |col, index| to_constant_declaration(col, index) } \
    .join("\n")

write_output(OUTPUTS[:unit_test_constants], CONSTANTS, 'Unit Test Constants')

### Mock Instantiation
# Mock the projection interface for testing with mock services exposing them
MOCKS = COLUMNS.map { |col|
  "doReturn(#{col[:java_constant_name]}).when(#{TABLE_HASH[:mock]}).#{col[:getter]}();"
}

mock_comment = "Projection interface query objects can't be instantiated and must be mocked instead.\n" +
    "This is slightly unfortunate, as the most common issue with projections is a column/type mismatch\n" +
    "in the query, which only shows up when calling the accessors on the proxy. So live DAO tests are\n" +
    " essential as well."
lines = [java_inline_comment(mock_comment), provenance_java]
lines << "final #{TABLE_HASH[:projection_interface]} #{TABLE_HASH[:mock]} = mock(#{TABLE_HASH[:projection_interface]}.class);"
lines << MOCKS
lines.flatten!

write_output(OUTPUTS[:unit_test_mocks], lines.join("\n"), 'Unit Test Mocks')

### Assertions
dto_assertions = COLUMNS.map{ |col|
  getter_call = "#{TABLE_HASH[:instance_name]}.#{col[:getter]}()"
  expected = col[:java_constant_name]
  if col[:projection_type].eql?('Timestamp')
    "    assertTimeApprox(#{getter_call}, #{expected});"
  else
    "    assertThat(#{getter_call}).isEqualTo(#{expected});"
  end

}.join("\n")

write_output(OUTPUTS[:dto_assertions], dto_assertions, 'Unit Test DTO Assertions')

DTO_ASSERTIONS_FUNCTION = "@Override\n" +
    "public void assertDTOFieldsMatchConstants(#{TABLE_HASH[:dto_class]} #{TABLE_HASH[:instance_name]} {\n" +
    dto_assertions + "\n}\n"

### Parameter Column Enum
def object_value_function(col)
  if col[:is_enum]
    "#{TABLE_HASH[:lambda_var]} -> enumToString(#{col[:lambda_var]}.#{col[:getter]}())"
  else
    case col[:projection_type]
    when 'Timestamp' # actually OffsetDateTime on the DTO
      "#{TABLE_HASH[:lambda_var]} -> toInsertRowString(#{col[:lambda_var]}.#{col[:getter]}())"
    else
      "#{TABLE_HASH[:dto_class]}::#{col[:getter]}"
    end
  end
end

# enum types are special. This assumes static imports of QueryParameterValue.int64 et al.
# Repeating that long class name clutters up the enum listing.
JAVA_TYPE_TO_QPV_FACTORY = {
    'Integer' => 'int64',
    'Long' => 'int64',
    'Short' => 'int64',
    'String' => 'string',
    'Double' => 'float64',
    'Boolean' => 'bool',
    'Timestamp' => 'toTimestampQpv'
}

def qpv_function(col)
  if col[:is_enum]
    convert_fn = 'enumToQpv'
  else
    # TODO: this hash is keyed off the projection type, but the type we care about is actually
    #   the Swagger-generated DTO model type. In some cases, a Short in the projection could be an
    #   Integer in the DTO model because of its trip through Swagger (which only has Integer and Long).
    #   It's something of a cosmetic issue, but does require manual fixup in a couple of places for now.
    convert_fn = "#{JAVA_TYPE_TO_QPV_FACTORY[col[:projection_type]]}"
  end
  "#{col[:lambda_var]} -> #{convert_fn}(#{col[:lambda_var]}.#{col[:getter]}())"
end

def query_parameter_column_entry(col)
  "#{col[:name].upcase}(\"#{col[:name]}\", #{object_value_function(col)}, #{qpv_function(col)})"
end

QPC_ENUM = (COLUMNS.map do |col|
  query_parameter_column_entry(col)
end.join(",\n")).freeze

write_output(OUTPUTS[:bigquery_insertion_payload_transformer], QPC_ENUM, "BigQueryInsertionPayloadTransformer Enum Entries")

### DTO Fluent Setters
# TODO: handle the case where projection type does not match the model, e.g.
#         .dataAccessLevel(DbStorageEnums.dataAccessLevelFromStorage(USER__DATA_ACCESS_LEVEL))
DTO_SETTERS = COLUMNS.map do |col|
  value = case col[:projection_type]
      when 'Timestamp'
            "offsetDateTimeUtc(#{col[:java_constant_name]})"
      else
        "#{col[:java_constant_name]}"
      end
  "    .#{col[:java_field_name]}(#{value})"
end.freeze

DTO_DECL = ["new #{TABLE_HASH[:dto_class]}()"]
DTO_DECL << DTO_SETTERS

dto_decl_str = (DTO_DECL.join("\n") + ';').freeze
write_output(OUTPUTS[:dto_decl], dto_decl_str, "DTO Object Construction")

### Entity Field Settings for DAO Test
ENTITY_DECL = ("final #{TABLE_HASH[:entity_class]} #{TABLE_HASH[:instance_name]} = new #{TABLE_HASH[:entity_class]}();\n" \
  << COLUMNS.map do |col|
    "#{TABLE_HASH[:instance_name]}.#{col[:setter]}(#{col[:java_constant_name]});"
  end \
.join("\n")).freeze

write_output(OUTPUTS[:entity_decl], ENTITY_DECL, "Entity Class Instance Declaration")

def result_set_function(column)
  if (column[:is_enum])
    'getShort'
  else
    "get#{column[:projection_type]}" # mostly works
  end
end

### RowMapper
#
ROW_MAPPERS = 'return jdbcTemplate.query(' + "\n" +
    '"SELECT \n"' + COLUMNS.map do |col| \
      "  + \"  #{col[:name]}" \
end.join(",\\n\"\n") +
      "\\n\"\n" +
      "+ \"FROM #{TABLE_HASH[:name]}).\"\n" +
    "(rs, unused) ->\n" +
    "new #{TABLE_HASH[:dto_class]}()\n" +
    COLUMNS.map do |col| \
        "    .#{col[:swagger_property_name]}(" +
        "rs.#{result_set_function(col)}(\"#{col[:name]}\")" +
        ")"
    end.join("\n") +
    ' )'

write_output(OUTPUTS[:row_mapper], ROW_MAPPERS, 'Row Mappeers')
### Empty schema description input file

DESCRIPTION_YAML_TEMPLATE = {
    TABLE_HASH[:name] => COLUMNS.to_h do |col|
        [col[:name], {
            'description' => col[:description] || '',
            'swagger_type' => col[:swagger_type],
            'entity_alias' => col[:projection_query_item]
        }]
    end
}

write_output(OUTPUTS[:yaml_template], indent_yaml(DESCRIPTION_YAML_TEMPLATE), 'YAML Input Template')

# basis for input file (needs to be thinned out first, though)
EVERYTHING_YAML = COLUMNS.to_h do |col|
  [col[:name], col.deep_stringify_keys]
end.to_yaml

write_output(OUTPUTS[:yaml_everything], EVERYTHING_YAML, 'All column info')
