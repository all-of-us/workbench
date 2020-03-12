require 'open3'
require 'yaml'

class DeveloperEnvironment
  def initialize(options)
    @logger = options[:'logger']
    @output_file = options[:'output-file'] || 'dev-tools-list.yaml'
  end

  def list
    versions = {}
    get_version_info(versions, 'ruby')
    get_version_info(versions, 'yarn')
    get_version_info(versions, 'gradle')
    get_version_info(versions, 'docker')
    get_version_info(versions, 'gcloud', '--version')
    get_version_info(versions, 'node')
    get_version_info(versions, 'javac', '-version', true)
    get_version_info(versions, 'java', '-version', true)
    get_version_info(versions, 'python', '--version', true)

    yaml = YAML.dump(versions)
    @logger.info(yaml)

    @logger.info("writing to output file: #{@output_file}")
    IO.write(@output_file, yaml)
    versions
  end

  private

  def get_version_info(versions, tool_cmd, flag = '-v', use_stderr = false, number_extractor = ->(x) { x.itself })
    result = {}
    full_cmd  = "#{tool_cmd} #{flag}"
    if use_stderr
      _stdout, output, _status = Open3.capture3(full_cmd)
    else
      output, _status = Open3.capture2(full_cmd)
    end
    result['version_string'] = output.strip
    result['version_number'] = number_extractor.call(result['version_string'])
    result['installed_at'] = `which #{tool_cmd}`.strip
    versions[tool_cmd] = result
  end
end
