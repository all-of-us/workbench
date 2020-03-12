require 'open3'
require 'yaml'

class DeveloperEnvironment
  def initialize(options)
    @logger = options[:'logger']
    @output_file = options[:'output-file'] || 'dev-tools-list.yaml'
    @input = {'tools' => []}
  end

  def list
    versions = {}
    # Assumes that a ruby version always has three sections.
    get_version_info(versions, 'ruby', /ruby\s(?<version>\d+\.\d+\.\w+)/)
    get_version_info(versions, 'yarn', /(?<version>\d+\.\d+\.\d+)/)
    # Gradle uses a multiline version  string, so take that into account. The Gradle x.y.z
    # is on a line by itself, offset with some spaces
    get_version_info(versions, 'gradle', /\s+Gradle\s(?<version>\d+\.\d+\.\d+)$/)
    get_version_info(versions, 'docker',/Docker\sversion\s(?<version>\d+\.\d+\.\d+),/)
    get_version_info(versions, 'gcloud', '--version')
    get_version_info(versions, 'node')
    get_version_info(versions, 'javac', nil, '-version', true)
    get_version_info(versions, 'java', nil, '-version', true)
    get_version_info(versions, 'python', nil, '--version', true)

    yaml = YAML.dump(versions)
    @logger.info(yaml)

    @logger.info("writing to output file: #{@output_file}")
    IO.write(@output_file, yaml)
    versions

    @logger.info(YAML.dump(@input))
  end

  private

  VERSION_NOT_RECOGNIZED = '<version number not recognized>'

  # Do the work for each tool. Runs that tool's version mode and captures the output, then applies the
  # given number_regex to pull the version number itself out. Also grabs installation path via `which`.
  # There's no attempt here to encourage or discourage particular installation directories for various tools,
  # but the information is handy when debugging system inconsistencies (such as when you have more pythons than
  # you realized)
  # versions -  result hash object into which this tool is planted with its key as  its cmd invocation name (e.g. use http instead of httpie)
  # tool_cmd - tool to get the version info from
  # flag - flag to pass  to the tool to get the version info
  # use_stderr - if true, capture stderr instead of stdout for this tool
  # number_regex - optional. Regex that captures a group named 'version' when matched against a tool's long-form output
  def get_version_info(versions, tool_cmd, number_regex = nil, flag = '-v', use_stderr = false) # number_extractor = ->(x) { x.itself })
    input = {
        'tool_cmd' => tool_cmd,
        'number_regex' => number_regex.to_s,
        'flag' => flag,
        'use_stderr' => use_stderr
    }
    @input['tools'] << input
    @logger.info("#{tool_cmd} #{flag} #{use_stderr} #{number_regex}")
    result = {}
    full_cmd  = "#{tool_cmd} #{flag}"
    if use_stderr
      _stdout, output, _status = Open3.capture3(full_cmd)
    else
      output, _status = Open3.capture2(full_cmd)
    end
    result['version_string'] = output.strip
    result['version_number'] = extract_version_number(result['version_string'], tool_cmd, number_regex)

    installation_dir  = `which #{tool_cmd}`.strip
    result['installed_at'] = installation_dir.empty? ? INSTALLATION_NOT_FOUND : installation_dir
    versions[tool_cmd] = result
  end

  def extract_version_number(version_string, tool_cmd, number_regex)
    if number_regex.nil?
      version_string
    else
      match_data = version_string.match(number_regex)
      version_number = ''
      if match_data && match_data.captures.size > 0
        @logger.info("Tool: #{tool_cmd} MatchData: #{match_data}")
        version_number = match_data[:version] # match_data.captures[0]
      end
      if version_number.nil? || version_number.empty?
        return VERSION_NOT_RECOGNIZED
      else
        return version_number
      end
    end
  end
end
