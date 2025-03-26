# Wraps the jira-ruby library to support creating and appending to All of Us
# release tickets. Requires basic auth credentials. The initial ticket is
# populated with a formatted git commit log since the last version.
#
# Note, this script is a Ruby analog to the RDR python release scripts:
# - https://github.com/all-of-us/raw-data-repository/blob/master/ci/release_notes.py
# - https://github.com/all-of-us/raw-data-repository/blob/master/rest-api/tools/update_release_tracker.py

require "open3"
require "jira-ruby"

require_relative "../../aou-utils/utils/common"


REPO_BASE_URL = "https://github.com/all-of-us/workbench"

RISK_RE = / ?\[risk=(no|low|moderate|severe)\]/i

# Formatting for release notes in JIRA comments.
# Note that JIRA auto-linkifies JIRA IDs, so avoid using commit message text in a link.
LOG_LINE_FORMAT = "--format=*   [%aN %ad|" + REPO_BASE_URL + "/commit/%h] %s"
# Overall release notes template.
RELEASE_NOTES_T = """h1. Release Notes for %{current}
h2. deployed to %{project}, listing changes since %{prev}
%{history}
"""

JIRA_INSTANCE_URL = "https://precisionmedicineinitiative.atlassian.net/"
JIRA_PROJECT_NAME = "PD"

def linkify_pull_request_ids(text)
  # Converts all substrings like "(#123)" to links to pull requests.
  return text.gsub(
      /\(#([0-9]+)\)/,
      '([#\1|' + REPO_BASE_URL + '/pull/\1])')
end

def annotate_commit_risk(line)
  # From the commit message, pulls out tokens like [risk=low] and appends a risk
  # level to the commit.
  match = RISK_RE.match(line)
  # Leave a TODO for the release engineer for untagged commits.
  risk = '*TODO*'
  if match
    risk = match[1].downcase
    line.sub!(RISK_RE, '')
  end
  return "#{line.chomp} (risk: #{risk})"
end


def get_release_notes_between_tags(project, from_tag, to_tag)
  """Formats release notes for JIRA from commit messages, between the two tags."""
  commit_messages = Common.new.capture_stdout(
    ['git', 'log', "#{from_tag}..#{to_tag}", LOG_LINE_FORMAT])
  unless commit_messages
    raise RuntimeError.new "failed to retrieve commits"
  end

  history = ""
  linkify_pull_request_ids(commit_messages).each_line do |line|
    history += annotate_commit_risk(line) + "\n"
  end
  return RELEASE_NOTES_T % {
    :current => to_tag,
    :project => project,
    :prev => from_tag,
    :history => history
  }
end

def format_jira_error(e)
  return "JIRA request failed with #{e.response.code} #{e.response.message}: " +
         e.response.body
end

class JiraReleaseClient
  def initialize(username, password)
    # Set :http_debug => true to see outgoing JIRA requests
    @client = JIRA::Client.new({
      :site => JIRA_INSTANCE_URL,
      :context_path => "",
      :username => username,
      :password => password,
      :auth_type => :basic,
    })
  end

  def self.from_gcs_creds(project)
    gcs_uri = "gs://#{project}-credentials/jira-login.json"
    jira_creds = Common.new.capture_stdout(['gsutil', 'cat', gcs_uri])
    unless jira_creds
      raise RuntimeError.new "failed to read JIRA login from '#{gcs_uri}'"
    end
    jira_json = JSON.parse(jira_creds)
    return JiraReleaseClient.new(jira_json['username'], jira_json['apiToken'])
  end

  def ticket_summary(tag)
    return "Release tracker: Workbench #{tag}"
  end

  def create_ticket(project, from_tag, to_tag, circle_url = nil)
    # Adds release notes to a new or existing JIRA issue.
    summary = ticket_summary(to_tag)
    description = get_release_notes_between_tags(project, from_tag, to_tag)
    if circle_url
      description += "\nCircle test output: #{circle_url}"
    end

    jira_project = @client.Project.find(JIRA_PROJECT_NAME).id
    issue = @client.Issue.build
    begin
      issue.save!({"fields" => {
                     "project" => {"id" => jira_project},
                     "summary" => summary,
                     "description" => description,
                     "issuetype" => {
                       "name" => "Release"
                     }
                   }
                  })
    rescue JIRA::HTTPError => e
      raise RuntimeError.new format_jira_error(e)
    end

    Common.new.status "Created [#{issue.key}] with release notes for #{to_tag}"
  end
  
  def create_tanagra_ticket(project, from_tag, to_tag)
    # Adds release notes to a new or existing JIRA issue.
    commits = ""
    summary = "Data Apps 2.0 Release Tracker #{to_tag}"
    description = "Release Notes for #{to_tag} 
    deploying to all-of-us-rw-preprod, listing changes since #{from_tag}:
    Change Management Description
    System: All of Us DRC, Workbench
    Developers: Brian Freeman, Will Dolbeer, Mariko Medlock, Tim Jennison, Dex Amundsen
    Needed By Date/Event: 
    Priority: Medium
    Configuration/Change Manager: TBD
    
    Anticipated Impact:
    Software Impact: Improvements to the Data Apps
    Training Impact: None
    Data Impact: Improved data quality
    Security Impact: low
    Related Changes: N/A
    Site(s) Affected: None
    Notes: Normal weekly release
    
    Security Impact Analysis:
    Does this release impact participant data? 
    Does this release introduce new data types? 
    Will this release cause scheduled downtime? 
    If a new tool or use case is being introduced, and a CONOPs with a SIA has been completed, what is the highest impact level on the SIA? 
    
    Testing
    Testers: Brian Freeman, Will Dolbeer, Mariko Medlock, Tim Jennison, Dex Amundsen
    Date Test Was Completed: 
    Implementation/Deployment Date: Ongoing
    FYI: TBD
    
    Github Test Cases
    https://github.com/DataBiosphere/tanagra/blob/main/README.md#codebase-test-status
    
    Changes since #{from_tag}: #{commits}
    
    Snyk Security Review
    [upload screenshot here]"
  
    jira_project = @client.Project.find(JIRA_PROJECT_NAME).id
    issue = @client.Issue.build
    begin
      issue.save!({"fields" => {
                     "project" => {"id" => jira_project},
                     "summary" => summary,
                     "description" => description,
                     "issuetype" => {
                       "name" => "Release"                      }
                     }
                  })
    rescue JIRA::HTTPError => e
      raise RuntimeError.new format_jira_error(e)
    end
  
    Common.new.status "Created [#{issue.key}] with release notes for #{to_tag}"
  end

  def comment_ticket(tag, msg)
    common = Common.new

    summary = ticket_summary(tag)
    begin
      issues = @client.Issue.jql(
        "project = \"#{JIRA_PROJECT_NAME}\" AND " +
        "summary ~ \"#{summary}\" ORDER BY created DESC")
    rescue JIRA::HTTPError => e
      raise RuntimeError.new format_jira_error(e)
    end
    if issues.empty?
      raise StandardError.new "no JIRA ticket found for summary \"#{summary}\""
    end
    if issues.length > 1
      common.warning "Found multiple release tracker matches, using newest: " +
                     issues.map { |iss| "[#{iss.key}] #{iss.fields['summary']}" }.join(', ')
    end
    issue = issues.first
    comment = issue.comments.build
    begin
      comment.save!(:body => msg)
    rescue JIRA::HTTPError => e
      raise RuntimeError.new format_jira_error(e)
    end

    common.status "Added comment \"#{msg}\" to issue [#{issue.key}]"
  end
end
