import * as _ from 'lodash';
const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36';

/**
 * Set up page common properties:
 * - Page view port
 * - Page user-agent
 * - Page navigation timeout
 * - waitFor functions timeout
 */
beforeEach(async () => {

  await jestPuppeteer.resetPage();
  await jestPuppeteer.resetBrowser();

  await page.setUserAgent(userAgent);
  await page.setViewport({width: 1300, height: 0});

  page.setDefaultNavigationTimeout(60000); // Puppeteer default timeout is 30 seconds.
  page.setDefaultTimeout(30000);

  await page.setRequestInterception(true);

  page.on('request', (request) => {
    try {
      if (request.url().includes('api-dot-all-of-us')) {
        const method = request.method();
        if (method !== 'OPTIONS') {
          const data = request.postData();
          const dataString = (data === undefined) ? '' : JSON.stringify(data, null, 2);
          console.debug(`❗Request issued: ${method} ${request.url()} \n ${dataString}`);
        }
      }
      request.continue();
      // tslint:disable-next-line:no-empty
    } catch (e) {
    }
  })
  .on('requestfinished', async (request) => {
    try {
      // response body can only be accessed for non-redirect responses.
      if (request.redirectChain().length === 0) {
        const response = await request.response();
        if (response != null) {
          if (request.url().includes('api-dot-all-of-us')) {
            const status = response.status();
            const failure = request.failure();
            if (failure != null) {
              const errorText = failure.errorText;
              const text = await response.text();
              console.debug(`❗Request failed: ${status} ${request.method()} ${request.url()}  \n ${errorText} \n ${text}`);
            } else {
              const buffer = await response.buffer();
              const truncatedResponse = _.truncate(JSON.stringify(JSON.parse(buffer.toString()), null, 2), {length: 2000});
              const method = request.method();
              console.debug(`❗Request finished: ${status} ${method} ${request.url()}  \n ${truncatedResponse}`);
            }
          }
        }
      }
      await request.continue();
      // tslint:disable-next-line:no-empty
    } catch (e) {
    }
  })
  .on('console', async (message) => {
    if (!message.args().length) {
      return;
    }
    try {
      const texts = await Promise.all(
        message.args().map(async (arg) =>
          await arg.executionContext().evaluate((txt) => {
            if (txt instanceof Error) {
              return txt.stack;
            }
            return txt.toString();
          }, arg))
      )
      const type = message.type();
      // Don't log "log", "info", "debug"
      switch (type) {
        case 'error':
        case 'warning':
          console.debug(`❗Page console ${message.type()}: ${texts}`);
          break;
      }
      // tslint:disable-next-line:no-empty
    } catch (err) {
    }
  })
  .on('error', (error) => {
    console.debug(`❗Page error: ${error}`);
  })
  .on('pageerror', (error) => {
    try {
      console.debug(`❗Page error: ${error}`);
      // tslint:disable-next-line:no-empty
    } catch (err) {
    }
  })

});

afterEach(async () => {
  await page.setRequestInterception(false);
});
