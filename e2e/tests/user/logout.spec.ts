import GoogleLoginPage from '../../app/google-login';
import Home from '../../app/home';
import {waitUntilURLMatch} from '../../driver/waitFuncs';
import PuppeteerLaunch from '../../services/puppeteer-launch';
require('../../driver/waitFuncs');

jest.setTimeout(60 * 1000);

const configs = require('../../config/config');

describe.skip('Logout tests:', () => {

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


  test('Sign Out', async () => {
    const loginPage = new GoogleLoginPage(page);
    await loginPage.login();
    const homePage = new Home(page);
    await homePage.waitForReady();
    await homePage.navigation.signOut();
    await waitUntilURLMatch(page, 'login');
    expect(await page.url()).toContain('login');
  });


});
