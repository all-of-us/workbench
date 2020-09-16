# All-of-Us Test Automation with Puppeteer in Chrome or Chromium

* Puppeteer `e2e` end-to-end integration tests.
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
 
## Set up test user login credential (two ways)
**The environment property file `.env` is used to store test user login credential and Puppeteer global settings. Test user must have 2FA-bypassed.**

```
# .env

# Login user email
USER_NAME=********
# Login password
PASSWORD=********

# Puppeteer env
PUPPETEER_HEADLESS=true
# Slows down test playback speed in milliseconds
PUPPETEER_SLOWMO=10
# Time out in milliseconds
PUPPETEER_NAVIGATION_TIMEOUT=90000
PUPPETEER_TIMEOUT=90000

```

1: Download latest `.env` file from All-of-Us workbench Google bucket
  
   - `gcloud auth login` to your PMI account.
   - In `workbench/` directory, run cmd: <div class="text-blue">`gsutil cp gs://all-of-us-workbench-test-credentials/puppeteer.local.env e2e/.env`</div>

2: Alternatively, copy `.env.sample` file and name the new file as `.env`
   - edit new `.env` file to provide your own user login credential.
 

## Yarn
**List preconfigured scripts and select a script to run**
- `yarn run`

### Few notable preconfigured scripts
* Run all tests in parallel **in headless Chrome** on deployed AoU "test" environment <div class="text-blue">`yarn test`</div>
* Run one test in headless Chrome with node `--inspect-brk` argument. It pauses test playback at breakpoints which is useful for debugging or/and writing new tests <div class="text-blue">`yarn test:debugTest [TEST_FILE]` </div>
* Run one test on your local server <div class="text-blue">`yarn test-local [TEST_FILE]` </div>
* Run one test on deployed AoU "test" environment <div class="text-blue">`yarn test:debug [TEST_FILE]` </div>

## Few examples of how to run tests on localhost (from `e2e` directory)
**Specify `USER_NAME` an `PASSWORD` environment variables in `yarn` command if don't use values from `.env` file.**

* Run "workspace-ui.spec.ts" test on "test" environment <div class="text-blue">`yarn test tests/workspace/workspace-ui.spec.ts`</div>

* Run all tests on "test" environment <div class="text-blue">`yarn test`</div>

* Run "login.spec.ts" test against the local server. Optionally specify your own userid and password <div class="text-blue">`USER_NAME=<YOUR_USERID> PASSWORD=<YOUR_USER_PASSWORD> yarn test-local tests/user/login.spec.ts`</div>



## Suspends test execution during test playback. 
-  Insert one line of code <div class="text-blue">`await jestPuppeteer.debug();`</div>

(Refers to https://github.com/smooth-code/jest-puppeteer#put-in-debug-mode for details)

## Features
* One retry running failed test by [jest-circus](https://github.com/facebook/jest/blob/f45d1c939cbf55a71dbfdfc316d2be62b590197f/docs/JestObjectAPI.md#jestretrytimes).
* Take screenshot and save the html page to `/log` directory if test fails.
