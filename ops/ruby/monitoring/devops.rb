require_relative 'tasks/count_monitoring_assets.rb'

counter = MonitoringAssets.new(ARGV[0])
counter.count
