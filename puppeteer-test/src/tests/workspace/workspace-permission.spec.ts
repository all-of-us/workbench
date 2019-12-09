import PuppeteerLaunch from '../../services/puppeteer-launch';

jest.setTimeout(60 * 1000);

const configs = require('../../config/config');

describe.skip('Workspace permission tests:', () => {

  let browser;
  let incognitoContext;
  let page;

  beforeAll(async () =>  {
    browser = await PuppeteerLaunch();
  });

  beforeEach(async () => {
    incognitoContext = await browser.createIncognitoBrowserContext();
    page = await incognitoContext.newPage();
    await page.setUserAgent(configs.puppeteerUserAgent);
    await page.setDefaultNavigationTimeout(60000);
  });

  afterEach(async () => {
    await incognitoContext.close();
  });

  afterAll(async () => {
    await browser.close();
  });

  test('user is OWNER', async () => {
      // TODO
  });

  test('user is READER', async () => {
      // TODO
  });
});
