# All-of-Us Test Automation with Puppeteer
Test using Jest as test runner and Puppeteer to perform automation tests in local installed Chrome browser.

### We use `yarn` instead `npm`
#### Install all libs
- `yarn`

#### List all `yarn` tasks from `package.json`
- `yarn run`

#### Run all UI tests found in `/tests` folder.
- Run command: `cd automation/`
- Copy `.env.sample` and name new file as `.env`. Add username and password in this file.
- Run command: `yarn test`

### External URLs
#### [Puppeteer examples](https://github.com/GoogleChromeLabs/puppeteer-examples/tree/b28a59d3333db42e3c7d1c6a7d53c5320e6c279b)
#### [Puppeteer debugging tips](https://github.com/GoogleChrome/puppeteer#debugging-tips)
#### [Puppeteer API Documentation](https://pub.dev/documentation/puppeteer/latest/puppeteer/puppeteer-library.html)
#### [Google developers Puppeteer tool](https://developers.google.com/web/tools/puppeteer)
#### [Difference between Chrome and Chromium](https://www.howtogeek.com/202825/what%E2%80%99s-the-difference-between-chromium-and-chrome)
#### [Chromium process models](https://www.chromium.org/developers/design-documents/process-models)
#### [Browser testing in CircleCI with Selenium](https://circleci.com/docs/2.0/browser-testing/#selenium)

### Reasons for Chrome switches usage
Disable throttling which can cause flakiness in tests.
https://blog.bigbinary.com/2018/08/15/debugging-failing-tests-in-background-tab-in-puppeteer.html
* '--disable-background-timer-throttling'
* '--disable-backgrounding-occluded-windows'
* '--disable-renderer-backgrounding'

Run headless with GPU enabled. https://github.com/GoogleChrome/puppeteer/issues/2901
* '--headless' 

https://github.com/GoogleChrome/puppeteer/issues/1664
* '--enable-features=NetworkService'

### Stop execution in middle of test
* Before starting test:
    * Set devtools: `{devtools: true}`
    * Set default jest test timeout: `jest.setTimeout(300000);`
    * Add `await page.evaluate(() => {debugger;});` to where you want to pause the playback
* Starting test


