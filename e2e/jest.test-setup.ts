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

  await jestPuppeteer.resetPage();

  await page.setUserAgent(userAgent);
  await page.setViewport({width: 1300, height: 0});

  page.setDefaultNavigationTimeout(60000); // Puppeteer default timeout is 30 seconds.
  page.setDefaultTimeout(30000);

  await page.setRequestInterception(true);

  page.on('request', (request) => {
    try {
      request.continue();
    } catch (e) {
      console.log('');
    }
  })

  page.on('console', async(message) => {
    console[message.type()](`page console => ${message.text()}`)
  });

  // Emitted when the page crashed
  page.on('error', err => {
    console.error(`❌ ${err}`);
  });

  // Emitted when a script has uncaught exception
  page.on('pageerror', error => {
    console.error(`❌ ${error.message}`)
  });

  // Emitted when a request failed. Warning: blocked requests from above will be logged as failed requests, safe to ignore these.
  page.on('requestfailed', request => {
    const response = request.response();
    const status = (response !== null ? response.status() : '');
    console.error(`❌ ${status} ${request.method()} ${request.url()}`);
  });

  page.on('response', async(response) => {
    try {
      const request = response.request();
      const requestUrl = request.url();

      // filter out some responses
      if (requestUrl.includes('api-dot-all-of-us')) {
        const failure = request.failure();
        if (failure === null) {
          const text = await response.text();
          const status = response.status();
          console.info(`${status} ${request.method()} ${requestUrl} \n ${text}`);
        } else {
          console.info(`${status} ${request.method()} ${requestUrl} \n ${failure}`);
        }

      }

    } catch (err) {
      console.log('');
    }
  });
  
});

afterEach(async () => {
  await page.setRequestInterception(false);
});
