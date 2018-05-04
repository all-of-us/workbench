require "open3"
require "set"
require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require_relative "../../api/libproject/devstart"
require_relative "../../api/libproject/gcloudcontext"
require_relative "../../api/libproject/wboptionsparser"


DOCKER_KEY_FILE_PATH = "/creds/sa-key.json"

STAGING_PROJECT = "all-of-us-rw-staging"
STABLE_PROJECT = "all-of-us-rw-stable"
RELEASE_MANAGED_PROJECTS = [STAGING_PROJECT, STABLE_PROJECT]

VERSION_RE = /^v[[:digit:]]+-[[:digit:]]+-rc[[:digit:]]+$/

def get_live_gae_version(project, validate_version=true)
  common = Common.new
  versions = common.capture_stdout %W{
    gcloud app
    --format json(id,service,traffic_split)
    --project #{project}
    versions list
  }
  if versions.empty?
    common.error "Failed to get live GAE version for project '#{project}'"
    exit 1
  end
  services = Set["api", "default", "public-api"]
  actives = JSON.parse(versions).select{|v| v["traffic_split"] == 1.0}
  if actives.empty?
    common.warning "Found 0 active GAE services in project '#{project}'"
    return nil
  elsif services != actives.map{|v| v["service"]}.to_set
    common.warning "Found active services #{v}, expected " +
                   "[#{services.to_a.join(', ')}] for project '#{project}'"
    return nil
  end

  versions = actives.map{|v| v["id"]}.to_set
  if versions.length != 1
    common.warning "Found varying IDs across GAE services in project '#{project}': " +
                   "[#{versions.to_a.join(', ')}]"
    return nil
  end
  v = versions.to_a.first
  if validate_version and not VERSION_RE.match(v)
    common.warning "Found a live version '#{v}' in project '#{project}', but " +
                   "it doesn't match the expected release version format"
    return nil
  end
  return v
end

def setup_and_enter_docker(cmd_name, opts)
  common = Common.new
  if not opts.git_version or not opts.app_version
    if opts.project == STAGING_PROJECT
      common.error "--git-version and --app-version are required when " +
                   "using this script to deploy to staging. Note: this " +
                   "should be an uncommon use case, please see the release " +
                   "documentation for details"
      exit 1
    end
    live_staging_version = get_live_gae_version(STAGING_PROJECT)
    if not live_staging_version
      common.error "No default staging version could be determined for " +
                   "promotion; please investigate or else be explicit in " +
                   "the version to promote by specifying both " +
                   "--git-version and --app-version"
      exit 1
    end
    if live_staging_version and not opts.app_version
      common.status "--app-version defaulting to '#{live_staging_version}'; " +
                    "found on project '#{STAGING_PROJECT}'"
      opts.app_version = live_staging_version
    end
    if live_staging_version and not opts.git_version
      common.status "--git-version defaulting to '#{live_staging_version}'; " +
                    "found on project '#{STAGING_PROJECT}'"
      opts.git_version = live_staging_version
    end
  end

  # TODO: Might be nice to emit the last version creation time here as a
  # sanity check (need to pick which service to do that for...).
  live_version = get_live_gae_version(opts.project, validate_version=false)
  common.status "Current live version is '#{live_version}' (project " +
                "#{opts.project})"
  puts "Will deploy git version '#{opts.git_version}' as App Engine " +
       "version '#{opts.app_version}' in project '#{opts.project}'"
  printf "Continue? (Y/n): "
  got = STDIN.gets.chomp.strip.upcase
  unless got == '' or got == 'Y'
    exit 1
  end

  # By default Tempfile on OS X does not use a docker-friendly location
  key_file = Tempfile.new(["#{opts.account}-key", ".json"], "/tmp")
  ServiceAccountContext.new(
    opts.project, account=opts.account, path=key_file.path).run do
    common.run_inline %W{docker-compose build deploy}
    common.run_inline %W{
      docker-compose run --rm
      -e WORKBENCH_VERSION=#{opts.git_version}
      -v #{key_file.path}:#{DOCKER_KEY_FILE_PATH}
      deploy deploy/project.rb #{cmd_name}
      --account #{opts.account}
      --project #{opts.project}
      #{opts.promote ? "--promote" : "--no-promote"}
      --app-version #{opts.app_version}
      --git-version #{opts.git_version}
      --key-file #{DOCKER_KEY_FILE_PATH}
    } +
      (opts.circle_url.nil? ? [] : %W{--circle-url #{opts.circle_url}}) +
      (opts.update_jira ? [] : %W{"--no-update-jira"})
  end
end

def deploy(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--project [project]",
    lambda {|opts, v| opts.project = v},
    "The Google Cloud project to deploy to."
  )
  op.add_option(
    "--account [account]",
    lambda {|opts, v| opts.account = v},
    "Service account to act as for deployment, if any. Defaults to the GAE " +
    "default service account."
  )
  op.add_option(
    "--key-file [key file]",
    lambda {|opts, v| opts.key_file = v},
    "Path to a service account key file to be used for deployment"
  )
  op.add_option(
    "--git-version [git version]",
    lambda {|opts, v| opts.git_version = v},
    "GitHub tag or branch, e.g. 'v1-0-rc1', 'origin/master'. Branch names " +
    "must be prefixed with 'origin/'. By default, uses the current live " +
    "staging release tag (if staging is in a good state)"
  )
  op.add_option(
    "--app-version [app version]",
    lambda {|opts, v| opts.app_version = v},
    "App Engine version to deploy as. By default, uses the current live " +
    "staging release version (if staging is in a good state)"
  )
  op.add_option(
    "--no-update-jira",
    lambda {|opts, v| opts.update_jira = false},
    "Don't update or create a ticket in JIRA; by default will pick " +
    "depending on the target project. On --no-promote JIRA is never updated"
  )
  op.add_option(
    "--promote",
    lambda {|opts, v| opts.promote = true},
    "Promote this version to immediately begin serving traffic"
  )
  op.add_option(
    "--no-promote",
    lambda {|opts, v| opts.promote = false},
    "Deploy, but do not yet serve traffic from this version - DB migrations " +
    "are still applied"
  )
  op.add_option(
    "--circle-url [circle url]",
    lambda {|opts, v| opts.circle_url = v},
    "Circle test output URL to attach to the release tracker; only " +
    "relevant for runs where a release ticket is created (staging)"
  )
  op.add_validator lambda {|opts| raise ArgumentError if opts.project.nil?}
  op.add_validator lambda {|opts| raise ArgumentError if opts.account.nil?}
  op.add_validator lambda {|opts| raise ArgumentError if opts.promote.nil?}

  op.parse.validate

  if op.opts.update_jira.nil?
     op.opts.update_jira = RELEASE_MANAGED_PROJECTS.include? op.opts.project
  end
  op.opts.update_jira = op.opts.update_jira and op.opts.promote

  unless Workbench::in_docker?
    return setup_and_enter_docker(cmd_name, op.opts)
  end

  # Everything following runs only within Docker.
  # Only require Jira stuff within Docker to avoid burdening the user with local
  # workstation Ruby gem setup.
  require_relative 'jirarelease'

  if op.opts.key_file.nil?
    raise ArgumentError.new("--key-file is required when running within docker")
  end
  if op.opts.app_version.nil?
    raise ArgumentError.new("--app-version is required when running within docker")
  end
  if op.opts.git_version.nil?
    raise ArgumentError.new("--git-version is required when running within docker")
  end
  common = Common.new
  common.run_inline %W{gcloud auth activate-service-account -q --key-file #{op.opts.key_file}}

  jira_client = nil
  create_ticket = false
  from_version = nil
  maybe_log_jira = lambda { |msg| common.status msg }
  if op.opts.update_jira
    if not VERSION_RE.match(op.opts.app_version) or
      op.opts.app_version != op.opts.git_version
      raise RuntimeError.new "for releases, the --git_version and " +
                             "--app_version should be equal and should be a " +
                             "release tag (e.g. v0-1-rc1); you shouldn't " +
                             "bypass this, but if you need to you can pass " +
                             "--no-update-jira"
    end

    # We're either creating a new ticket (staging), or commenting on an existing
    # release ticket (stable, prod).
    jira_client = JiraReleaseClient.from_gcs_creds(op.opts.project)
    if op.opts.update_jira and op.opts.project == STAGING_PROJECT
      create_ticket = true
      from_version = get_live_gae_version(STAGING_PROJECT)
      if not from_version
        # Alternatively, we could support a --from_version flag
        raise RuntimeError "could not determine live staging version, and " +
                           "therefore could not generate a delta commit log; " +
                           "please manually deploy staging with the old " +
                           "version and supply --no-update-jira, then retry"
      end
    else
      maybe_log_jira = lambda { |msg|
        begin
          jira_client.comment_ticket(op.opts.app_version, msg)
        rescue StandardError => e
          common.error "comment_ticket failed: #{e}"
        end
      }
    end
  end

  # TODO: Add more granular logging, e.g. call deploy natively and pass an
  # optional log writer. Also rescue and log if deployment fails.
  maybe_log_jira.call "'#{op.opts.project}': Beginning deploy of api and " +
                      "public-api services (including DB updates)"
  common.run_inline %W{
    ../api/project.rb deploy
      --project #{op.opts.project}
      --account #{op.opts.account}
      --key-file #{op.opts.key_file}
      --version #{op.opts.app_version}
      #{op.opts.promote ? "--promote" : "--no-promote"}
  }

  maybe_log_jira.call "'#{op.opts.project}': completed api and public-api " +
                      "service deployment; beginning deploy of UI service"
  common.run_inline %W{
    ../ui/project.rb deploy-ui
      --project #{op.opts.project}
      --account #{op.opts.account}
      --key-file #{op.opts.key_file}
      --version #{op.opts.app_version}
      #{op.opts.promote ? "--promote" : "--no-promote"}
      --quiet
  }
  maybe_log_jira.call "'#{op.opts.project}': completed UI service deployment"

  if create_ticket
    jira_client.create_ticket(op.opts.project, from_version,
                              op.opts.git_version, op.opts.circle_url)
  end
end

Common.register_command({
  :invocation => "deploy",
  :description => "",
  :fn => lambda { |*args| deploy("deploy", args) }
})

def docker_clean(*args)
  common = Common.new
  common.run_inline %W{docker-compose down --volumes}
end

Common.register_command({
  :invocation => "docker-clean",
  :description => \
    "Removes docker containers and volumes, allowing the next `deploy` to " \
    "start from scratch (e.g., no git repo cache). This should not normally " \
    "be necessary outside of deploy script development",
  :fn => lambda { |*args| docker_clean(*args) }
})
