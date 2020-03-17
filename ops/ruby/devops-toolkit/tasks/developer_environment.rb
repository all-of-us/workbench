require 'open3'
require 'yaml'

class DeveloperEnvironment
  def initialize(options)
    @logger = options[:'logger']
    @logger.level = Logger::INFO
    # TODO(jaycarlton) make this configurable
    @input_file = options[:'input-tools-file'] || './tasks/input/aou-workbench-dev-tools.yaml'
    @output_file = options[:'output-file'] || 'dev-tools-list.yaml'
    # Use strings as hash key
    @output = { 'tools' =>  []}
  end

  def list
    input_yaml = YAML.load(IO.read(@input_file))
    @logger.debug(input_yaml)
    input_yaml['tools'].each do |tool|
      get_version_info(tool)
    end

    @output['metadata'] = {}
    metadata = @output['metadata']
    metadata['hostname'] = get_stdout('hostname')
    metadata['shell'] = get_stdout('echo $0')
    metadata['timestamp'] = Time.now.getutc
    metadata['os_name'] = get_stdout('uname -a')
    metadata['username'] = get_stdout('whoami')

    yaml = YAML.dump(@output)
    @logger.info(yaml)

    @logger.info("writing to output file: #{@output_file}")
    IO.write(@output_file, yaml)
    @output
  end

  private

  VERSION_NOT_RECOGNIZED = '<version number not recognized>'
  # Do the work for each tool. Runs that tool's version mode and captures the output, then applies the
  # given number_regex to pull the version number itself out. Also grabs installation path via `which`.
  # There's no attempt here to encourage or discourage particular installation directories for various tools,
  # but the information is handy when debugging system inconsistencies (such as when you have more pythons than
  # you realized)
  # All args are values in the tool hash (string keys)
  # versions -  result hash object into which this tool is planted with its key as  its cmd invocation name (e.g. use http instead of httpie)
  # tool_cmd - tool to get the version info from
  # flag - flag to pass  to the tool to get the version info
  # use_stderr - if true, capture stderr instead of stdout for this tool
  # number_regex - optional. Regex that captures a group named 'version' when matched against a tool's long-form output
  # def get_version_info(tool_cmd, number_regex = nil, flag = '-v', use_stderr = false) # number_extractor = ->(x) { x.itself })
  def get_version_info(tool)
    tool_cmd = tool['tool']
    number_regex_string = tool['number_regex']
    if number_regex_string.nil? || number_regex_string.empty?
      number_regex = nil
    else
      number_regex = Regexp.new(number_regex_string)
    end
    if tool['flag'].nil? || tool['flag'].empty?
      flag = '-v'
    else
      flag = tool['flag']
    end

    if tool['use_stderr'].nil?
      use_stderr = false
    else
      use_stderr = tool['use_stderr']
    end
    @logger.debug("#{tool_cmd} flag: '#{flag}' #{use_stderr} #{number_regex}")
    result = { 'tool' => tool_cmd }
    full_cmd  = "#{tool_cmd} #{flag}"
    if use_stderr
      output = get_stderr(full_cmd)
    else
      output = get_stdout(full_cmd)
    end
    result['version_string'] = output.strip
    result['version_number'] = extract_version_number(result['version_string'], number_regex)

    installation_dir  = `which #{tool_cmd}`.strip
    result['installed_at'] = installation_dir.empty? ? INSTALLATION_NOT_FOUND : installation_dir
    @output['tools'] << result
  end

  def get_stderr(full_cmd)
    _stdout, output, _status = Open3.capture3(full_cmd)
    output
  end

  def get_stdout(full_cmd)
    output, _status = Open3.capture2(full_cmd)
    output.strip
  end

  def extract_version_number(version_string, number_regex)
    if number_regex.nil?
      version_string
    else
      match_data = version_string.match(number_regex)
      version_number = ''
      if match_data && match_data.captures.size > 0
        version_number = match_data[:version]
      end
      if version_number.nil? || version_number.empty?
        return VERSION_NOT_RECOGNIZED
      else
        return version_number
      end
    end
  end
end
