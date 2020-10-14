const url = require('url');
const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36';

const isDebugMode = process.argv.includes('--debug');

/**
 * Set up page common properties:
 * - Page view port
 * - Page user-agent
 * - Page navigation timeout
 * - waitFor functions timeout
 */
beforeEach(async () => {
  await page.setUserAgent(userAgent);
  await page.setViewport({width: 1280, height: 0});
  page.setDefaultNavigationTimeout(60000); // Puppeteer default timeout is 30 seconds.
  page.setDefaultTimeout(10000);

  page.on('request', (request) => {
        request.continue();
  });

  page.on('error', error => console.error(`❌ ${error.toString()}`));

  page.on('requestfailed', request => {
    console.error(`url: ${request.url()}, errText: ${request.failure().errorText}, method: ${request.method()}`)
  });

  // Catch console log errors
  page.on('pageerror', err => {
    console.error(`Page error: ${err.toString()}`);
  });

});

/**
 * At the end of each test completion, do:
 * - Disable network interception.
 * - Delete broswer cookies.
 * - Reset global page and browser variables.
 */
afterEach(async () => {
  await page.deleteCookie(...await page.cookies());
  await jestPuppeteer.resetPage();
  await jestPuppeteer.resetBrowser();
});

/**
 * Enable network interception in new page and block unwanted requests.
 */
beforeAll(async () => {
  await page.setRequestInterception(true);

});

afterAll(async () => {
  await page.setRequestInterception(false);
});
