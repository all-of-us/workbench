import exec from 'child_process'
const REPO_BASE_URL = 'https://github.com/all-of-us/workbench';

function checking(text) {
  return text.replace(/\(#([0-9]+)\)/g, `([#$1|${REPO_BASE_URL}/pull/$1])`);
}

function deploy() {
  console.log(checking('hey'));
}
function example(project) {
  let gcs_uri = `gs://${project}-credentials/jira-login.json`
  exec(`gsutil cat ${gcsUri}`, (error, stdout, stderr) => {
    if (error) {
      //  console.error(`Error executing command: ${error}`);
      return;
    }
    if (stderr) {
      //console.error(`Command STDERR: ${stderr}`);
    }
    //console.log('Command output:', stdout);
  });
}