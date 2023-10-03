import * as fs from 'fs';
import {exec, execSync} from "child_process";
import {promisify} from 'util';
import * as os from 'os';

export class ServiceAccountContext {
  static TEST_SERVICE_ACCOUNTS = new Set([
    'all-of-us-workbench-test@appspot.gserviceaccount.com',
    'aou-db-test@appspot.gserviceaccount.com'
  ]);
  static SERVICE_ACCOUNT_KEY_PATH = 'sa-key.json';

  constructor(project, serviceAccount = null, keyfilePath = null, cmd) {
    console.log('in const ')
    this.project = project;
    this.serviceAccount = serviceAccount || `${this.project}@appspot.gserviceaccount.com`;
    this.keyfilePath = keyfilePath || (ServiceAccountContext.TEST_SERVICE_ACCOUNTS.has(this.serviceAccount)
        ? process.cwd() + ServiceAccountContext.SERVICE_ACCOUNT_KEY_PATH
        : path.join(os.tmpdir(), `${this.serviceAccount}-key.json`));
    console.log(this.keyfilePath);
    this.cmd = cmd;
  }

  async existingFileAccount(keyfilePath) {
    try {
      const content = await promisify(fs.readFile)(keyfilePath, 'utf8');
      const { client_email } = JSON.parse(content);
      return client_email;
    } catch (error) {
      return null;
    }
  }

  async run() {
    process.env.GOOGLE_APPLICATION_CREDENTIALS = this.keyfilePath;

    const existingAccount = await this.existingFileAccount(this.keyfilePath);
    if (this.serviceAccount === existingAccount) {
      console.log(`Attaching to existing keyfile @ ${this.keyfilePath}`);
      await this.runCmd(this.cmd[0]);
      await this.runCmd(this.cmd[1]);
      return;
    }


    if (ServiceAccountContext.TEST_SERVICE_ACCOUNTS.has(this.serviceAccount)) {
      console.log(`Copying key from GCS for ${this.serviceAccount} @ ${this.keyfilePath}`);
      await this.runCmd(`gsutil cp gs://${this.project}-credentials/app-engine-default-sa.json ${this.keyfilePath}`);
      await this.runCmd(this.cmd[0]);
      await this.runCmd(this.cmd[1]);
    } else {
      console.log(`Creating new key for ${this.serviceAccount} @ ${this.keyfilePath}`);
      await this.runCmd(`gcloud iam service-accounts keys create ${this.keyfilePath} --iam-account=${this.serviceAccount} --project=${this.project}`);
      try {
        await this.runCmd(this.cmd[0]);
        await this.runCmd(this.cmd[1]);
      } finally {
        const { stdout: tmpPrivateKey } = await exec(`grep private_key_id ${this.keyfilePath} | cut -d\\" -f4`);
        console.log(tmpPrivateKey);
        await this.runCmd(`gcloud iam service-accounts keys delete ${tmpPrivateKey} -q --iam-account=${this.serviceAccount} --project=${this.project}`);
        await this.runCmd(`rm ${this.keyfilePath}`);
      }
    }
  }

  async runCmd(command) {
    try {
      // Run the shell command and capture its output
      const output = execSync(command).toString();
      console.log('try');
      // Process the output as needed
      console.log('Output:', output);
      return output;
    } catch (error) {
      console.log('in catch');
      // Handle errors, if any
      console.error('Error:', error.message);
    }
  }
}