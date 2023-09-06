import { execSync} from "child_process";
import {ServiceAccountContext} from "../libproject/serviceaccounts.mjs"
import { env } from 'node:process';


const YARN_DEPS = 'yarn deps';
const CREDENTIALS_COPY = '/root/gsutil cp gs://all-of-us-workbench-test-credentials/.npmrc ..';
const YARN_INSTALL = 'yarn install --frozen-lockfile';
const YARN_RUN_DEPS = 'yarn run deps';

const BUILD_CMDS = [YARN_DEPS, CREDENTIALS_COPY, YARN_INSTALL, YARN_RUN_DEPS];

async function build_and_deploy(arg) {
  const project = arg[2];
  const version = arg[3];
  const promote = arg[4];
  const dry_run = arg[5];
  const env_name = arg[6];
  const account = arg[7];
  const key_file = arg[8];

  if (isEmpty(project) || isEmpty(version) || isEmpty(promote)) {
    throw new Error('Argument: project, version and promote/no-promote are required');
  }
  await build(env_name);

  const prefix = dry_run ? 'echo [DRY_RUN] ' : []
  const gcloudDeploy = prefix + `gcloud app deploy --project ${project} --version ${version} --no-promote`;

  const servicecontext = new ServiceAccountContext(project, account, key_file, gcloudDeploy);
  servicecontext.run();
  return true;
}
async function build(env) {

  // await runCommand(YARN_DEPS);
  console.log("Current directory:", process.cwd());

  await runCommand("env");
  // await runCommand("process.env");
  await runCommand(CREDENTIALS_COPY);
  await runCommand(YARN_INSTALL);
  await runCommand(YARN_RUN_DEPS);

  // for (let index = 0; index < BUILD_CMDS.length; index++) {
  //   await runCommand(BUILD_CMDS[index]);
  // }

  let optimize = "--aot";
  if (env === 'staging' || env ==='stable' || env === 'preprod' || env === 'prod') {
    optimize = "--prod"
  }
  const react_opts = "REACT_APP_ENVIRONMENT=" + env;
  await runCommand(`${react_opts} yarn run build ${optimize} --no-watch --no-progress`);
}

function  isEmpty(value) {
  if (value !== 'undefined' && value !== null && value.length != 0) {
    return false;
  }
  return true;
}


async function runCommand(cmd) {
  console.log('Going to run the command: '+cmd);
  var output = execSync(cmd, {stdio: 'pipe'}).toString();
  if (output) {
    console.log('stdout:e ' + output);
  }
}

build_and_deploy(process.argv)
