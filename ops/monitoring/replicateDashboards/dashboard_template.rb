require 'logger'

class DashboardTemplate
  def initialize(template_json, target_projects, logger)
    @template_json = template_json
    @target_projects = target_projects
    @logger = logger
  end

  RESOURCE_NAMESPACE_PATTERN = /resource.label."namespace"="\w+"/

  # Replace all occurrences of the resource namespace in the dashboard filter
  # with the value passed in.
  def populate(namespace)
    result = @template_json.dup
    @logger.info("Processing Dashboard #{result['name']}")
    #@logger.debug(pp(result))
    # Make sure we don't have an ETAG in the input method
    result.delete('etag')
    result['gridLayout']['widgets'].map! do |widget|
      @logger.debug('processing widget ' + widget['title'])
      widget['xyChart']['dataSets'].map! do |dataSet|
        @logger.debug('processing dataSet')
        # ['xyChart']['dataSets'][0]['timeSeriesQuery']['timeSeriesFilter']['filter']
        old_filter = dataSet['timeSeriesQuery']['timeSeriesFilter']['filter']
        @logger.debug("original filter: #{old_filter}")
        new_filter = old_filter.gsub(RESOURCE_NAMESPACE_PATTERN,
                                     'resource.label."namespace"="' + namespace + '"')
        dataSet['timeSeriesQuery']['timeSeriesFilter']['filter'] = new_filter
        @logger.info("Applied new filter to dashboard: #{new_filter}")
        dataSet
      end
      widget
    end
    result
  end

  def dashboard_name(project_number)
    "projects/602460048110/dashboards/6849129967503530308"
  end
end