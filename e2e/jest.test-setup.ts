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
          console.debug(`❗Request issued: ${request.url()}`);
        }
      }
      request.continue();
      // tslint:disable-next-line:no-empty
    } catch (e) {
    }
  })
  .on('requestfinished', async (request) => {
    try {
      const response = await request.response();
      const status = request.response().status();
      let responseBody;
      if (request.redirectChain().length === 0) {
        // response body can only be accessed for non-redirect responses.
        if (request.url().includes('api-dot-all-of-us')){
          responseBody = await response.buffer();
          if (responseBody !== undefined) {
            responseBody = JSON.stringify(responseBody.toString());
          }
          const method = request.method();
          console.debug(`❗Request finished: ${status} ${method} ${request.url()}  \n ${responseBody}`);
        }
      }
      await request.continue();
      // tslint:disable-next-line:no-empty
    } catch (e) {
    }
  })
  .on('requestfailed', async (request) => {
    try {
      const response = request.response();
      if (response !== null) {
        const status = response.status();
        const responseText = await response.text();
        const failureError = request.failure().errorText;
        console.debug(`❗Request failed: ${status} ${request.method()} ${request.url()}  \n ${failureError} \n ${responseText}`);
      }
      // tslint:disable-next-line:no-empty
    } catch (err) {
    }
  })
  .on('console', (message) => {
    if (message != null) {
      // Don't log "log", "info" or "debug"
      if (['ERROR', 'WARNING'].includes(message.type().toUpperCase())) {
        console.debug(`❗Page console: ${message.type()}: ${message.text()}`);
      }
    }
  })
  .on('error', (error) => {
    console.debug(`❗Page error: ${error}`);
  })
  .on('pageerror', (error) => {
    if (error != null) {
      console.debug(`❗Page error: ${error}`);
    }
  })
  
});

afterEach(async () => {
  await page.setRequestInterception(false);
});
