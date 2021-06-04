// import { logger } from 'libs/logger';
// import * as fp from 'lodash/fp';

import { CDPSession } from 'puppeteer';

const { CIRCLE_BUILD_NUM } = process.env;

const userAgent =
  'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko)' +
  ' Chrome/80.0.3987.149 Safari/537.36' +
  (CIRCLE_BUILD_NUM ? ` (circle-build-number/${CIRCLE_BUILD_NUM})` : '');

// https://chromedevtools.github.io/devtools-protocol/tot/Page/
// https://chromedevtools.github.io/devtools-protocol/tot/Fetch/
// https://chromedevtools.github.io/devtools-protocol/tot/Log/

// eslint-disable-next-line @typescript-eslint/no-unused-vars,@typescript-eslint/ban-ts-comment
// @ts-ignore
const interceptNetworkRequest = async (cdpSession: CDPSession) => {
  //await cdpSession.send('Page.enable');
  //await cdpSession.send('Network.enable');
  await cdpSession.send('Log.enable');
  await cdpSession.send('Page.setDownloadBehavior', {
    behavior: 'allow',
    downloadPath: 'logs/download'
  });

  cdpSession.on('Log.entryAdded', ({ logEntry }) => {
    console.log(logEntry);
  });
};

beforeEach(async () => {
  await jestPuppeteer.resetBrowser();
  await page.setUserAgent(userAgent);
  await page.setViewport({ width: 1300, height: 0 });

  // Puppeteer default navigation timeout is 30 seconds.
  page.setDefaultNavigationTimeout(60000);

  // Enable Requests interception.
  await interceptNetworkRequest(await page.target().createCDPSession());

  browser.on('targetcreated', async (target) => {
    const page = await target.page();
    await interceptNetworkRequest(await page.target().createCDPSession());
  });
});
