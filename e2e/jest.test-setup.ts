import { initPageBeforeTest } from './libs/page-manager';

// TODO Delete this file when deleting jest-puppeteer
beforeEach(async () => {
  await initPageBeforeTest(page);
});

afterEach(async () => {
  await page.setRequestInterception(false);
  await jestPuppeteer.resetBrowser();
});
