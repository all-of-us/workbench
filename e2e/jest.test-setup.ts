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
          console.debug(`❗Request issued: ${method} ${request.url()}`);
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
            responseBody = JSON.stringify(JSON.parse(responseBody.toString()), null, 2);
          }
          const method = request.method();
          console.debug(`❗Request finished: ${status} ${method} ${request.url()}  \n ${responseBody}`, {depth: null, colors: true});
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
  .on('console', async (message) => {
    try {
      const args = await Promise.all(
        message.args().map(async (arg) =>
          await arg.executionContext().evaluate((txt) => {
            if (txt instanceof Error) {
              return JSON.stringify(txt.stack);
            }
            return txt.toString();
          }, arg))
      )
      const msgText = message.text();
      const text = args.filter(msg => msg !== 'undefined').join(' ');
      if (msgText && !message.args().length) {
        return;
      }
      const type = message.type();
      // Don't log "log", "info"
      switch (type) {
        case 'error':
        case 'warning':
        case 'debug':
          console.debug(`❗Page console ${message.type()}: ${JSON.stringify(text, null, 2)}`);
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
