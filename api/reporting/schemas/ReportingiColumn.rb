require 'string_util'
# Data class for useful values for the reporting structure.
class ReportingColumn
  attr_reader :description, :name, :projection_name, :getter,
              :setter, :types, :unit_test_constant, :projection_expression

  def initialize(input)
    self.description = input['description']
    self.name = input['name']
    self.projection_name = to_camel_case(input['name'], true)
  end


end
