## All-of-Us Test Automation with Puppeteer in Chrome
#####`e2e` end-to-end integration tests.
##### Puppeteer run on the latest [maintenance LTS](https://github.com/nodejs/Release#release-schedule) version of Node.


#### Set up
##### Clone github project
- `git clone git@github.com:all-of-us/workbench.git`

##### Install node libraries
- change dir: `cd e2e`
- run cmd `yarn` or `yarn install`

##### Set up test user login credential (two ways)
1: Download latest `.env` file from All-of-Us workbench Google bucket.
  
  - `gcloud auth login` to your PMI account.
  - in `workbench/` directory, run command:
  `gsutil cp gs://all-of-us-workbench-test-credentials/puppeteer.local.env e2e/.env`

2: Alternatively, copy `.env.sample` file and name the new file as `.env`.
- edit new `.env` file to provide test user login credential.
  
  ** note: test user must have 2FA-bypassed to run tests.


#### List all `yarn` tasks from `package.json`
- `yarn run`


#### Run e2e tests on localhost (from `e2e` directory)
(Note: Specify `USER_NAME` an `PASSWORD` environment variables before `yarn` command for using your own userid and password)

##### \>> Run one test against "test" environment.
- run cmd `yarn test tests/workspace/workspace-ui.spec.ts`

##### \>> Run all tests against "test" environment.
- run cmd `yarn test`

##### \>> Run one test against your local server. Optionally specify your own userid and password.
- run cmd `USER_NAME=<YOUR_USERID> PASSWORD=<YOUR_USER_PASSWORD> yarn test-local tests/user/login.spec.ts`

##### \>> Run all tests against "test" environment.
- run cmd `USER_NAME=<YOUR_USERID> PASSWORD=<YOUR_USER_PASSWORD> yarn test`

##### Suspends test execution during test playback in order to inspect the browser. 
-  Insert one line of code: `await jestPuppeteer.debug();`
 
 (Refers to https://github.com/smooth-code/jest-puppeteer#put-in-debug-mode for details)
 