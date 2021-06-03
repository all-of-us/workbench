// import { logger } from 'libs/logger';
// import * as fp from 'lodash/fp';

const { CIRCLE_BUILD_NUM } = process.env;

const userAgent =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko)' +
  ' Chrome/80.0.3987.149 Safari/537.36' +
  (CIRCLE_BUILD_NUM ? ` (circle-build-number/${CIRCLE_BUILD_NUM})` : '');

const requestCache = new Map();

const requestUrlPatterns = ['*'];

// https://chromedevtools.github.io/devtools-protocol/tot/Page/
// https://chromedevtools.github.io/devtools-protocol/tot/Fetch/
// https://chromedevtools.github.io/devtools-protocol/tot/Log/

const interceptRequest = async (page) => {
  const cdpSession = await page.target().createCDPSession();

  await cdpSession.send('Log.enable');
  await cdpSession.send('Page.setDownloadBehavior', {
    behavior: 'allow',
    downloadPath: 'logs/download'
  });

  await cdpSession.send('Fetch.enable', {
    patterns: [
      {
        urlPattern: '*',
        requestStage: 'Response',
        resourceType: 'XHR'
      }
    ]
  });

  cdpSession.on('Log.entryAdded', async ({ logEntry }) => {
    console.log(logEntry);
  });

  await cdpSession.on('Fetch.requestPaused', async (requestEvent) => {
    const { requestId } = requestEvent; // 获取浏览器为当前请求分配的编号
    console.log(`Request "${requestId}" paused.`);

    // 检查默认的responseHeader中是否包含目标内容
    const responseHeaders = requestEvent.responseHeaders || [];

    // log out response headers.
    for (const header of responseHeaders) {
      console.log(`header.name: ${header.name}`);
      console.log(`header.value: ${header.value}`);
    }

    const responseObj = await cdpSession.send('Fetch.getResponseBody', {
      requestId
    });
    console.log(`responseObj.body: ${responseObj.body}`);
    await cdpSession.send('Fetch.continueRequest', { requestId });
    console.log(`Request "${requestId}" continued.`);
  });
};

beforeEach(async () => {
  await jestPuppeteer.resetBrowser();
  await page.setUserAgent(userAgent);
  await page.setViewport({ width: 1300, height: 0 });

  // Puppeteer default navigation timeout is 30 seconds.
  page.setDefaultNavigationTimeout(60000);

  // Enable Requests interception.
  const client = await page.target().createCDPSession();
  await client.send('Network.enable');

  // added configuration
  await client.send('Network.setRequestInterception', {
    patterns: [{ urlPattern: '*' }]
  });

  await client.on('Network.requestIntercepted', async (request) => {
    console.log('EVENT INFO: ');
    console.log(request.interceptionId);
    console.log(request.resourceType);
    console.log(request.isNavigationRequest);

    // pass all network requests (not part of a question)
    await client.send('Network.continueInterceptedRequest', {
      interceptionId: request.interceptionId,
      errorReason: request.isNavigationRequest ? undefined : 'Aborted'
    });
  });
});
