import GoogleLoginPage, {selectors} from '../../app/google-login';
import PuppeteerLaunch from '../../driver/puppeteer-launch';

const configs = require('../../resources/workbench-config');

// set timeout globally per suite, not per test.
jest.setTimeout(2 * 60 * 1000);

describe('Login tests:', () => {
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

  test('Open AoU Workspaces page before login redirects to login page', async () => {
    const url = configs.uiBaseUrl + configs.workspacesUrlPath;
    const loginPage = new GoogleLoginPage(page);
    await page.goto(url, {waitUntil: 'networkidle0'});
    expect(await loginPage.loginButton).toBeTruthy();
  });

  test('Entered wrong password', async () => {
    const INCORRECT_PASSWORD = 'wrongpassword123';
    const loginPage = new GoogleLoginPage(page);
    await loginPage.load();

    const naviPromise = page.waitForNavigation({waitUntil: 'networkidle0'});
    const googleButton = await loginPage.loginButton();
    await googleButton.click();
    await naviPromise;

    await loginPage.enterEmail(configs.userEmail);
    await loginPage.enterPassword(INCORRECT_PASSWORD);
    const button = await page.waitForXPath(selectors.NextButton, {visible: true});
    await button.click();

    const err = await loginPage.waitForTextExists('Wrong password. Try again');
    expect(err).toBeTruthy();
  });

});
