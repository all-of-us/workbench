import GoogleLoginPage from '../../pages/google-login';
import Home from '../../pages/home';
import PuppeteerLaunch from '../../services/puppeteer-launch';

jest.setTimeout(60 * 1000);

const configs = require('../../config/config');

describe.skip('Workspace creation tests:', () => {

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

  test('Create a new workspace from the Home page', async () => {
    const loginPage = new GoogleLoginPage(page);
    await loginPage.login();

    const home = new Home(page);
    const link = await home.getCreateNewWorkspaceLink();
    await link.click();
    //TODO
  });

});
