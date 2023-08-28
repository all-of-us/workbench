const { execSync } = require('child_process');
const JiraApi = require('jira-client');

const REPO_BASE_URL = 'https://github.com/all-of-us/workbench';
const RISK_RE = / ?\[risk=(no|low|moderate|severe)\]/i;
const LOG_LINE_FORMAT = '--format=*   [%aN %ad|' + REPO_BASE_URL + '/commit/%h] %s';
const RELEASE_NOTES_T = `h1. Release Notes for %{current}
h2. deployed to %{project}, listing changes since %{prev}
%{history}
`;

const JIRA_INSTANCE_URL = 'https://precisionmedicineinitiative.atlassian.net/';
const JIRA_PROJECT_NAME = 'PD';


function linkifyPullRequestIds(text) {
  return text.replace(/\(#([0-9]+)\)/g, `([#$1|${REPO_BASE_URL}/pull/$1])`);
}

function annotateCommitRisk(line) {
  const match = RISK_RE.exec(line);
  let risk = '*TODO*';
  if (match) {
    risk = match[1].toLowerCase();
    line = line.replace(RISK_RE, '');
  }
  return `${line.trim()} (risk: ${risk})`;
}

function getReleaseNotesBetweenTags(project, fromTag, toTag) {
  const commitMessages = execSync(`git log ${fromTag}..${toTag} --pretty=format:"${LOG_LINE_FORMAT}"`, { encoding: 'utf-8' });
  const formattedCommitMessages = linkifyPullRequestIds(commitMessages).split('\n')
      .map(line => annotateCommitRisk(line))
      .join('\n');
  return RELEASE_NOTES_T.replace('%{current}', toTag)
      .replace('%{project}', project)
      .replace('%{prev}', fromTag)
      .replace('%{history}', formattedCommitMessages);
}

class JiraReleaseClient {
  constructor(username, password) {
    this.client = new JiraApi({
      protocol: 'https',
      host: JIRA_INSTANCE_URL.replace('https://', ''),
      username,
      password,
      apiVersion: '2',
      strictSSL: true
    });
  }

  static fromGcsCreds(project) {
    // You will need to implement the GCS reading logic here
    // and parse the JSON credentials.
    const jiraJson = {}; // Replace with your GCS read logic
    return new JiraReleaseClient(jiraJson['username'], jiraJson['apiToken']);
  }

  ticketSummary(tag) {
    return `Release tracker: Workbench ${tag}`;
  }

  createTicket(project, fromTag, toTag, circleUrl) {
    const summary = this.ticketSummary(toTag);
    let description = getReleaseNotesBetweenTags(project, fromTag, toTag);
    if (circleUrl) {
      description += `\nCircle test output: ${circleUrl}`;
    }

    const jiraProject = JIRA_PROJECT_NAME; // Use the actual project ID
    const issue = {
      fields: {
        project: { key: jiraProject },
        summary: summary,
        description: description,
        issuetype: { name: 'Release' }
      }
    };

    this.client.addNewIssue(issue)
        .then(() => {
          console.log(`Created [${issue.key}] with release notes for ${toTag}`);
        })
        .catch(error => {
          console.error(`JIRA request failed: ${error}`);
        });
  }

  commentTicket(tag, msg) {
    const summary = this.ticketSummary(tag);

    this.client.searchJira(`project = "${JIRA_PROJECT_NAME}" AND summary ~ "${summary}" ORDER BY created DESC`)
        .then(issues => {
          if (issues.length === 0) {
            throw new Error(`no JIRA ticket found for summary "${summary}"`);
          }

          const issue = issues[0];
          const comment = {
            body: msg
          };

          return this.client.addComment(issue.id, comment);
        })
        .then(() => {
          console.log(`Added comment "${msg}" to issue [${issue.key}]`);
        })
        .catch(error => {
          console.error(`JIRA request failed: ${error}`);
        });
  }
}
