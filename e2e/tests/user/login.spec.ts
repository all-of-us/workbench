import GoogleLoginPage, {selectors} from '../../app/GoogleLoginPage';
import BasePage from '../../app/mixin-pages/BasePage';
import PuppeteerLaunch from '../../driver/puppeteer-launch';
import {waitUntilFindTexts} from '../../driver/waitFuncs';
require('../../driver/waitFuncs');

jest.setTimeout(60 * 1000);

const configs = require('../../resources/config');

describe.skip('Login tests:', () => {

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

  test('Open \'/workspaces\' page before login redirects to login', async () => {
    const url = configs.uiBaseUrl + configs.workspacesUrlPath;
    const loginPage = new GoogleLoginPage(page);
    await page.goto(url, {waitUntil: 'networkidle0'});
    await page.waitFor(2000);
    expect(await loginPage.loginButton).toBeTruthy();
  });

  test('Wrong password', async () => {
    const INCORRECT_PASSWORD = 'wrongpassword123';
    const loginPage = new GoogleLoginPage(page);
    await loginPage.goto();

    const naviPromise = page.waitForNavigation({waitUntil: 'networkidle0'});
    const googleButton = await loginPage.loginButton;
    await googleButton.click();
    await naviPromise;

    await loginPage.enterEmail(configs.userEmail);
    await loginPage.enterPassword(INCORRECT_PASSWORD);
    const button = await page.waitForXPath(selectors.passwordNextButton);
    await button.click();

    const passwordPage = new BasePage(page);
    const err = await waitUntilFindTexts(page, 'Wrong password. Try again');
    expect(err).toBeTruthy();
  });

});
