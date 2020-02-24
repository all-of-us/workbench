require 'google/cloud/monitoring/v3/alert_policy_service_client'
require './gcp_environment_visitor'

alerts_client = Google::Cloud::Monitoring::V3::AlertPolicyServiceClient.new


visitor = GcpEnvironmentVisitor.new([],
                                    'monitoring-alerts-admin@all-of-us-workbench-test.iam.gserviceaccount.com')
visitor.visit do |env|
  policies = alerts_client.list_alert_policies("projects/#{env.project_id}")

  policies.each do |policy|
    puts policy.to_json
  end
end
