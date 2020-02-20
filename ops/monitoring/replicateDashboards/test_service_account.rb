require 'google/cloud/monitoring/dashboard/v1'

ENV["GOOGLE_APPLICATION_CREDENTIALS"] = './monitoring-key.json'

# Retrieve the template dashboard from the test environment.
dashboard_client = Google::Cloud::Monitoring::Dashboard::V1::DashboardsServiceClient.new
dashboard_template = dashboard_client.get_dashboard(ARGV[0])
puts "Template dashboard: display_name: #{dashboard_template.display_name}, name: #{dashboard_template.name}"
puts dashboard_template.to_json

dashboard_template.display_name = dashboard_template.display_name + ' 2: Electric Boogaloo'
dashboard_client.update_dashboard(dashboard_template)

dashboard_template_updated = dashboard_client.get_dashboard(ARGV[0])
puts dashboard_template_updated.display_name

new_dashboard = dashboard_template_updated
new_dashboard.etag = ""
new_dashboard.name = "projects/602460048110/dashboards/#{ARGV[1]}"
dashboard_client.create_dashboard('projects/602460048110', dashboard_template_updated)