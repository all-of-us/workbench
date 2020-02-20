class AouEnvironmentInfo
  def initialize(short_name, project_id, project_number)
    @short_name = short_name
    @project_id = project_id
    @project_number = project_number
  end

  attr_reader :short_name
  attr_reader :project_id
  attr_reader :project_number

  def formatted_project_name
    "projects/#{@project_id}"
  end

  def formatted_project_number
    "projects/#{@project_number}"
  end

  def to_s
    "short_name: #{@short_name}, project_id: #{@project_id}, project_number: #{@project_number}"
  end
end