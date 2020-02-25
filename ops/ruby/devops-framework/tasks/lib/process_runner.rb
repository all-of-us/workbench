require 'logger'

class ProcessRunner
  def initialize(logger = Logger.new(STDOUT))
    @logger = logger
  end
  def run(cmd)
    @logger.info(cmd.join(' '))
    pid = spawn(*cmd)
    Process.wait pid
    if $?.exited?
      unless $?.success?
        exit $?.exitstatus
      end
    else
      error "Command exited abnormally."
      exit 1
    end
  end
end
