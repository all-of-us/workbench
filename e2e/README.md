# All-of-Us Test Automation with Puppeteer in Chrome or Chromium
Puppeteer `e2e` end-to-end integration tests.  These tests are run in CircleCI
for every PR and merge to master ([example](https://app.circleci.com/pipelines/github/all-of-us/workbench/4074/workflows/ca636d7c-8c11-463e-bfdc-39ea63b6df52/jobs/100294)).

* See [Puppeteer API](https://github.com/puppeteer/puppeteer/blob/v5.0.0/docs/api.md). Puppeteer supports the latest [maintenance LTS](https://github.com/nodejs/Release#release-schedule) version of Node.
* Use [jest-puppeteer](https://github.com/smooth-code/jest-puppeteer) preset to configure `jest` to run Pupppeteer tests.
  Puppeteer `browser`, `context` and `page` variables are global variables exposed by jest-puppeteer preset.
* AoU tests run in Chromium by default.

## Set up project
* Clone github project: `git clone git@github.com:all-of-us/workbench.git`

* Install required node libraries
  - change to e2e dir `cd e2e`
  - run cmd `yarn install`
* Compile `yarn compile`
 
## Test Users
e2e tests are run using pre-existing users (rather than by generating new users)
so we must supply their credentials using an environment property file `.env`.  See [.env.sample](.env.sample) for an example. 

### CircleCI Test Users
CircleCI uses the user `puppetcilocaltester1@fake-research-aou.org` to run the e2e tests on every PR and 
`puppetcitester6@fake-research-aou.org` for running tests on master after every merge.  **DO NOT**
use these users for local development.

### Local Test Users
Developers' Local environments share a Google Suite domain with the Test environment,
so users with matching emails share the same Terra and Google cloud resources between these
environments. To avoid contention, it's necessary to create a test user unique to the local 
environment.  This is the same process as creating any other local user.  Ensure that this 
user is 2FA-Bypassed and populate the environment property file `.env` with this user's credentials.

## Running tests
Command line tests are run using the [Yarn](https://classic.yarnpkg.com/en/) build tool. Tests can be run in 
**headless mode** (with invisible browser execution) or "headful" mode, with the browser
interactions visible to you.

**To see the list of available Yarn commands**
- `yarn run`

### Examples
* Run all tests in parallel **in headless Chrome** on deployed AoU "test" environment <div class="text-blue">`yarn test`</div>
* Run one test on deployed AoU "test" environment <div class="text-blue">`yarn test:debug [TEST_FILE]` </div>
* Run one test on your local server <div class="text-blue">`yarn test-local [TEST_FILE]` </div>
* Run one test in headless Chrome with node `--inspect-brk` argument. It pauses test playback at breakpoints which is useful for debugging or/and writing new tests <div class="text-blue">`yarn test:debugTest [TEST_FILE]` </div>
* If you don't want to use the `.env` file, you can also specify `USER_NAME` and `PASSWORD` as environment variables. <div class="text-blue">`USER_NAME=<YOUR_USERID> PASSWORD=<YOUR_USER_PASSWORD> yarn test-local tests/user/login.spec.ts`</div>

### Debugging
- To suspend test execution during test playback, insert one line of code <div class="text-blue">`await jestPuppeteer.debug();`</div>

(Refers to https://github.com/smooth-code/jest-puppeteer#put-in-debug-mode for details)

## Features
* One retry running failed test by [jest-circus](https://github.com/facebook/jest/blob/f45d1c939cbf55a71dbfdfc316d2be62b590197f/docs/JestObjectAPI.md#jestretrytimes).
* Take screenshot and save the html page to `/log` directory if test fails.
