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
  await page.setViewport({width: 1300, height: 0});
  page.setDefaultNavigationTimeout(60000); // Puppeteer default timeout is 30 seconds.
  page.setDefaultTimeout(30000);
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
  page.on('request', (request) => {
    const requestUrl = url.parse(request.url(), true);
    const host = requestUrl.hostname;
    // to improve page load performance, block network requests unrelated to application.
    try {
      if (host === 'www.google-analytics.com'
           || host === 'accounts.youtube.com'
           || host === 'static.zdassets.com'
           || host === 'play.google.com'
           || request.url().endsWith('content-security-index-report')) {
        request.abort();
      } else {
        request.continue();
      }
    } catch (err) {
      console.error(err);
    }
  });
  if (isDebugMode) {
    // Emitted when a request failed. Warning: blocked requests from above will be logged as failed requests, safe to ignore these.
    page.on('requestfailed', request => {
      console.error(`❌ Failed request => ${request.method()} ${request.url()}`);
      request.continue();
    });
    // Emitted when the page crashed
    page.on('error', error => console.error(`❌ ${error}`));
    // Emitted when a script has uncaught exception
    page.on('pageerror', error => console.error(`❌ ${error}`));
  }
});

afterAll(async () => {
  await page.setRequestInterception(false);
});
