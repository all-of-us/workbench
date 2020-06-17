const url = require('url');

const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36';

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
