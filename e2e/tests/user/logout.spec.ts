import GoogleLoginPage from '../../app/GoogleLoginPage';
import HomePage from '../../app/HomePage';
import PuppeteerLaunch from '../../driver/puppeteer-launch';
import {waitUntilURLMatch} from '../../driver/waitFuncs';
require('../../driver/waitFuncs');

jest.setTimeout(60 * 1000);

const configs = require('../../resources/config');

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
    const homePage = new HomePage(page);
    await homePage.waitForReady();
    await homePage.navigation.signOut();
    await waitUntilURLMatch(page, 'login');
    expect(await page.url()).toContain('login');
  });


});
