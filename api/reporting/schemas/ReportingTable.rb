require 'ostruct'

class ReportingTable
  attr_reader :dto_class, :entity_class, :instance_name, :alias, :test_fixture_class

  # Treat table as struct backing this thing
  def initialize(table)
    self.dto_class = table.dto_class
    self.entity_class = table.entity_class

  end

end
