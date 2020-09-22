const url = require('url');
const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36';

const navTimeOut = parseInt(process.env.PUPPETEER_NAVIGATION_TIMEOUT, 10) || 90000;
const timeOut = parseInt(process.env.PUPPETEER_TIMEOUT, 10) || 90000;
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
  // See https://github.com/puppeteer/puppeteer/blob/master/docs/api.md#pagesetdefaultnavigationtimeouttimeout
  page.setDefaultNavigationTimeout(navTimeOut);
  page.setDefaultTimeout(timeOut);
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

  // Log failed requests: 4xx and 5xx status code.
  // Warning: blocked requests from above will be logged as failed requests, safe to ignore these.
  page.on('requestfailed', request => {
    console.error(`❌ Failed request => ${request.method()} ${request.url()} ${request.response().status()}`);
    request.continue();
  });

  // Emitted when the page crashed
  page.on('error', err => {
    console.error(`❌ Crashed page => ${err}`);
    throw new Error(err.message);
  });
});

afterAll(async () => {
  await page.setRequestInterception(false);
});
