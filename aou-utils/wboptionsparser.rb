require "optparse"
require "ostruct"

# Creates a default command-line argument parser.
# command_name: For help text.
def create_parser(command_name)
  OptionParser.new do |parser|
    parser.banner = "Usage: ./project.rb #{command_name} [options]"
    parser
  end
end

class WbOptionsParser
  # Allows parsing of command line options without requiring any subclassing.

  attr_reader :opts, :remaining

  def initialize(command_name, args)
    @opts = OpenStruct.new
    @parser = create_parser(command_name)
    @args = args
    @validators = []
  end

  def add_option(option, assign, help)
    @parser.on(option, help) {|v| assign.call(@opts, v)}
    self
  end

  def add_typed_option(option, type, assign, help)
    @parser.on(option, type, help) {|v| assign.call(@opts, v)}
    self
  end

  def add_validator(fn)
    @validators.push(fn)
    self
  end

  def parse()
    @remaining = @parser.parse @args
    self
  end

  def validate()
    @validators.each do |fn|
      begin
        fn.call(@opts)
      rescue ArgumentError
        STDERR.puts @parser.help
        exit 1
      end
    end
    self
  end
end
