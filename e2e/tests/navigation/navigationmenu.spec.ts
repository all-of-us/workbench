import GoogleLoginPage from '../../app/google-login';
import Home from '../../app/home';
import Profile from '../../app/profile';
import Workspaces from '../../app/workspace-page';
import PuppeteerLaunch from '../../services/puppeteer-launch';

jest.setTimeout(60 * 1000);

const configs = require('../../config/config');

describe.skip('Navigation menu tests:', () => {

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
    const loginPage = new GoogleLoginPage(page);
    await loginPage.login();
  });

  afterEach(async () => {
    await incognitoContext.close();
  });

  afterAll(async () => {
    await browser.close();
  });

  test('User sign out', async () => {
    const homePage = new Home(page);
    await homePage.waitForReady();

    await homePage.navigation.signOut();
    expect(await page.url()).toContain('login');
  });

  test('User open Profile page', async () => {
    const homePage = new Home(page);
    await homePage.waitForReady();

    await homePage.navigation.toProfile();

    const profile = new Profile(page);
    await profile.waitForReady();
    expect(await page.url()).toContain('profile');
  });

  test('User open all-workspaces page', async () => {
    const homePage = new Home(page);
    await homePage.waitForReady();

    await homePage.navigation.toAllWorkspaces();

    const workspaces = new Workspaces(page);
    await workspaces.waitForReady();
    expect(await page.url()).toContain('workspaces');
  });

});
