import {exec,execSync, spawnSync} from "child_process";
import * as fs from "fs";
import * as path from "path";
import { tmpdir } from "os";
import {ServiceAccountContext} from "./serviceAccountContext.mjs";
function deploy(arg) {
  console.log(arg);
  // This could be improved so that arg are not dependent on order
  const project = arg[4];
  const account = arg[6];
  let promote = false;
  if (arg[7]=== '--promote') {
    promote = true;
  }
  const dry_run = arg[8];
  console.log(project);
  console.log(account);
  console.log(promote);
  console.log(dry_run);
  // Also we need to add help

  // Assume all arg are present next step
  //The .dockerenv contained the environment variables defined inside the container.
  // They were used to setup the environment variables properly after the lxc-attach.
  if(!fileExists("./dockerenv")) {
    setup_and_enter_docker(project, account)
  }


}

const DOCKER_KEY_FILE_PATH = "/creds/sa-key.json"

function setup_and_enter_docker(project, account) {
  const commandToRun = `gcloud app versions list --format="json(id,service,traffic_split)" --project=${project}`;
  const versions = runCmd (commandToRun);
  if (versions.length === 0) {
    console.error( `Failed to get live GAE version for project ${project}`);
    throw `No live version for project ${project}`;
  }
  const obj = JSON.parse(versions);
  console.log(obj);

  const split = obj.filter((ob)=> ob.traffic_split ===1);
  console.log(split);
  const ids = split.map((obj) => obj.service);
  console.log(ids);

  // let v = versions.to_a.first
  // if validate_version and not VERSION_RE.match(v)
  // common.warning "Found a live version '#{v}' in project '#{project}', but " +
  // "it doesn't match the expected release version format"
  // return nil
  // end
  const keyFile = fs.mkdtempSync(path.join(tmpdir(), `${account}-key`)) + '.json';
 console.log(keyFile);
 const cmd = `docker-compose run --rm -e WORKBENCH_VERSION=v7-77-rc1 -v ${keyFile}:${DOCKER_KEY_FILE_PATH} deploy deploy/deploy.mjs deploy --account ${account} --project ${project} --no-promote --app-version v7-77-rc1 --git-version v7-77-rc1 --key-file ${DOCKER_KEY_FILE_PATH} --no-update-jira --dry-run`;
 const sA = new ServiceAccountContext(project, account, keyFile, ["docker-compose build deploy ", cmd] );
 sA.run();
}

function fileExists(filePath) {
  try {
    fs.accessSync(filePath, fs.constants.F_OK);
    return true;
  } catch (err) {
    return false;
  }
}

function runCmd(command) {

  try {
    // Run the shell command and capture its output
    const output = execSync(command).toString();

    // Process the output as needed
    console.log('Output:', output);
    return output;
  } catch (error) {
    // Handle errors, if any
    console.error('Error:', error.message);
  }
}

deploy(process.argv)