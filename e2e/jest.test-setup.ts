const url = require('url');

const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36';

// Runs this beforeEach() before test's beforeEach().
beforeEach(async () => {
  await page.setUserAgent(userAgent);
  // Refer to https://github.com/puppeteer/puppeteer/blob/master/docs/api.md#pagesetdefaultnavigationtimeouttimeout
  await page.setDefaultNavigationTimeout(90000);
  await page.setDefaultTimeout(60000);
});

// Runs this afterEach() before test's afterEach().
afterEach(async () => {
  await page.deleteCookie(...await page.cookies());
  await jestPuppeteer.resetBrowser();
});

// Runs this beforeAll() before test's beforeAll().
beforeAll(async () => {
  await page.setRequestInterception(true);
  page.on('request', (request) => {
    const requestUrl = url.parse(request.url(), true);
    const host = requestUrl.hostname;
    // to improve page load performance, block requests if it is not for application functions.
    if (host === 'www.google-analytics.com'
       || host === 'accounts.youtube.com'
       || host === 'static.zdassets.com'
       || host === 'play.google.com'
       || request.url().endsWith('content-security-index-report')) {
      request.abort()
    } else {
      request.continue();
    }
  });
});

// Runs this afterAll() before test's afterAll().
afterAll(async () => {
  await page.setRequestInterception(false);
});
