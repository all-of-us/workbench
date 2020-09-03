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
