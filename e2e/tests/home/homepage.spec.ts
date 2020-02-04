import Workspaces from '../../app/Workspaces';
import GoogleLoginPage from '../../pages/google-login';
import Home from '../../pages/home';
import PuppeteerLaunch from '../../services/puppeteer-launch';

jest.setTimeout(60 * 1000);

const configs = require('../../config/config');

describe('Home page tests:', () => {
  let browser;
  let page;
  let cookies;

  beforeAll(async () => {
    browser = await PuppeteerLaunch();
  });

  beforeEach(async () => {
    page = await browser.newPage();
    await page.setUserAgent(configs.puppeteerUserAgent);
    await page.setDefaultNavigationTimeout(60000);
  });

  afterEach(async () => {
    await page.close();
  });

  afterAll(async () => {
    await browser.close();
  });

  test('Homepage is the landing page after sign in', async () => {
    const loginPage = new GoogleLoginPage(page);
    await loginPage.login();
    const homePage = new Home(page);
    await homePage.waitForReady();
    const title = await page.title();
    expect(title).toMatch('Homepage');
    cookies = await page.cookies();
  });

  test('Sign in with cookies', async () => {
    await page.setCookie(...cookies);
    const workspaces = new Workspaces(page);
    await workspaces.goURL();
    await workspaces.waitForReady();
    expect(await page.title()).toMatch('Workspaces');
  });


});
