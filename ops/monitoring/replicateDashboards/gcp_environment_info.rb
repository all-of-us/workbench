class GcpEnvironmentInfo
  # def initialize(short_name, project_id, project_number, service_account)
  #   @short_name = short_name
  #   @project_id = project_id
  #   @project_number = project_number
  #   @service_account = service_account
  # end

  def initialize(env_info)
    @short_name = env_info['short_name']
    @project_id = env_info['project_id']
    @project_number = env_info['project_number']
    @service_account = env_info['service_account']
  end

  attr_reader :short_name
  attr_reader :project_id
  attr_reader :project_number
  attr_reader :service_account

  def formatted_project_name
    "projects/#{project_id}"
  end

  def formatted_project_number
    "projects/#{project_number}"
  end

  def to_s
    "short_name: #{short_name}, project_id: #{project_id}, project_number: #{project_number}, service_account: #{service_account}"
  end
end
