const url = require('url');

const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36';

// Set beforeEach for every test
beforeEach(async () => {
  await page.setUserAgent(userAgent);
  await page.setDefaultNavigationTimeout(90000);
});

// Set afterEach for every test
afterEach(async () => {
  await page.deleteCookie(...await page.cookies());
  await jestPuppeteer.resetBrowser();
});

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

afterAll(async () => {
  await page.setRequestInterception(false);
});
