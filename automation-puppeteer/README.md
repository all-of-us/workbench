# All-of-Us Test Automation with Puppeteer
Test using Jest as test runner and Puppeteer to perform automation tests in local installed Chrome browser.

### Try running tests in localhost
#### clone github project
- `git clone git@github.com:all-of-us/workbench.git`

#### set up login credential
- copy `.env.sample` file. Save new file as `.env` (do not commit `.env` file to version control)
- edit `.env` to provide workbench login credential
- 

#### install libraries
- change dir: `cd puppeteer-test`
- run cmd `yarn`

### Run UI tests in `tests` folder
- in `puppeteer-test` dir, run cmd `yarn test`

#### List all available `yarn` tasks from `package.json`
- `yarn run`

### Stop execution in middle of test
* Before starting test:
    * Set devtools: `{devtools: true}`
    * Set default jest test timeout: `jest.setTimeout(300000);`
    * Add `await page.evaluate(() => {debugger;});` to where you want to pause the playback
* Starting test


