require 'google/cloud/monitoring/v3/alert_policy_service_client'
require './gcp_environment_visitor'

ENV['GOOGLE_APPLICATION_CREDENTIALS'] = './monitoring-key.json'

project_id = 'all-of-us-workbench-test'
alerts_client = Google::Cloud::Monitoring::V3::AlertPolicyServiceClient.new


visitor = GcpEnvironmentVisitor.new([],
                                    'monitoring-alerts-admin@all-of-us-workbench-test.iam.gserviceaccount.com',
                                    './monitoring-key.json')
visitor.load_environments_json

puts visitor.environments

visitor.visit_single('prod') do |env|
  puts "visiting #{env}"
  policies = alerts_client.list_alert_policies("projects/#{env.project_id}")

  policies.each do |policy|
    puts policy.to_json
  end
end
