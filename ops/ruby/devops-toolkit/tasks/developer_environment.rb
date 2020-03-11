require 'open3'

class DeveloperEnvironment
  def initialize(options)
    @logger = options[:'logger']
    @output_file = options[:'output-file']
  end

  def list
    versions = {}
    versions['ruby'] = `ruby -v`.strip
    versions['yarn'] = `yarn -v`.strip
    versions['gradle'] = `gradle -v`.strip
    versions['docker'] = `docker -v`.strip
    versions['gcloud'] = `gcloud -v`.strip
    versions[:node] = `node -v`.strip

    versions[:javac] = get_stderr('javac -version')

    stderr = get_stderr('java -version')
    versions['java'] = stderr

    _stdout, stderr, status = Open3.capture3('python --version')
    versions[:python] = stderr.strip
    @logger.info(JSON.pretty_generate(versions))


    # IO.write(@output_file, versions.to_json)
    # versions
  end

  private

  def get_stderr(cmd)
    stdout, stderr, status = Open3.capture3(cmd)
    # @logger.info("java: #{stdout} #{stderr} #{status}")
    stderr
  end
end
