const url = require('url');
const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36';

/**
 * Before starting any test, set up page common properties:
 * - Page view port
 * - Page user-agent
 * - Page navigation timeout
 * - waitFor functions timeout
 */
beforeAll(async () => {
  await page.setViewport({ width: 0, height: 0 });
  await page.setUserAgent(userAgent);
  // See https://github.com/puppeteer/puppeteer/blob/master/docs/api.md#pagesetdefaultnavigationtimeouttimeout
  await page.setDefaultNavigationTimeout(90000);
  await page.setDefaultTimeout(60000);
});

/**
 * Before start of each test, enable network interception in new page and block unwanted requests.
 */
beforeEach(async () => {
  await page.setRequestInterception(true);
  page.on('request', async (request) => {
    const requestUrl = url.parse(request.url(), true);
    const host = requestUrl.hostname;
    // to improve page load performance, block network requests unrelated to application.
    try {
      if (host === 'www.google-analytics.com'
         || host === 'accounts.youtube.com'
         || host === 'static.zdassets.com'
         || host === 'play.google.com'
         || request.url().endsWith('content-security-index-report')) {
        await request.abort();
      } else {
        await request.continue();
      }
    } catch (err) {
      console.error(err);
    }
  });
});

/**
 * At the end of each test completion, do:
 * - Disable network interception.
 * - Delete broswer cookies.
 * - Reset global page and browser variables.
 */
afterEach(async () => {
  await page.setRequestInterception(false);
  await page.deleteCookie(...await page.cookies());
  await jestPuppeteer.resetPage();
  await jestPuppeteer.resetBrowser();
});
