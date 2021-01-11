const url = require('url');
const userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36';

const isDebugMode = process.argv.includes('--debug');

/**
 * Enable network interception in new page and block unwanted requests.
 */
beforeEach(async () => {
  await page.setUserAgent(userAgent);
  await page.setViewport({width: 1280, height: 0});
  page.setDefaultNavigationTimeout(60000); // Puppeteer default timeout is 30 seconds.
  page.setDefaultTimeout(30000);

  await page.tracing.start({path: './logs/tracing.json', screenshots: true});

  await page.setRequestInterception(true);

  page.on('request', async(request) => {
      // console.info(`Request started => ${request.url()}`)
      await request.continue();

  });

  page.on('response', async response => {
    try {
      const req = response.request();
      const reqUrl = req.url();

      // filter out some responses
      if (reqUrl.indexOf('.png') !== -1
         || reqUrl.indexOf('.svg') !== -1
         || reqUrl.indexOf('.woff2') !== -1
         || reqUrl.indexOf('aousupporthelp') !== -1
         || reqUrl.indexOf('zdassets') !== -1
         || reqUrl.indexOf('.js') !== -1)
        return;

      const text = await response.text();
      const status = response.status();
      console.info(`response => ${reqUrl}, ${status}, ${text}`);
    } catch (err) {
      console.error(`catch errors: ${err}`);
    }
  });

  // Emitted when the page crashed
  page.on('error', async(err) => {
    console.error(`❌ error =>  ${err}`);
    throw new Error(err.message);
  });

  // Emitted when a script has uncaught exception
  page.on('pageerror', async(error) => {
    console.error(`❌ page error => ${error.message}`)
  });

  // Emitted when a script within the page uses `console`
  page.on('console', async(message) => {
    console[message.type()](`page console => ${message.text()}`)
  });

});

afterEach(async () => {
  await page.tracing.stop();
  await page.setRequestInterception(false);
});
