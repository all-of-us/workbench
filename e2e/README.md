## All-of-Us Test Automation with Puppeteer in Chrome
#####`e2e` end-to-end integration tests.
##### Puppeteer run on the latest [maintenance LTS](https://github.com/nodejs/Release#release-schedule) version of Node.


####Set up
##### Clone github project
- `git clone git@github.com:all-of-us/workbench.git`

##### Set up login credential
- copy `.env.sample` file, save new file as `.env` (do not commit `.env` file to version control).
- edit new `.env` file to provide workbench login credential.
  ** note: we have a 2FA-bypassed test user created to run tests in test env. if you don't know it, ask others for test user credential.

##### install node libraries
- change dir: `cd e2e`
- run cmd `yarn` or `yarn install`

#### Run single test in `tests` folder on localhost
- example: in `e2e` dir, run cmd `yarn test tests/workspace/workspace-ui.spec.ts`

#### Run all tests in `tests` folder on localhost
- in `e2e` dir, run cmd `yarn test`

#### List all available `yarn` tasks from `package.json`
- `yarn run`

#### Stop execution in middle of test
* Before starting test:
    * Set devtools: `{devtools: true}`
    * Set default jest test timeout: `jest.setTimeout(300000);`
    * Add `await page.evaluate(() => {debugger;});` to where you want to pause the playback
* Starting test
