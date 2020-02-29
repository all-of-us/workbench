import GoogleLoginPage from '../../app/GoogleLoginPage';
import HomePage from '../../app/HomePage';
import ProfilePage from '../../app/ProfilePage';
import WorkspaceEditPage from '../../app/WorkspaceEditPage';
import PuppeteerLaunch from '../../driver/puppeteer-launch';

jest.setTimeout(60 * 1000);

const configs = require('../../resources/workbench-config');

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
    const homePage = new HomePage(page);
    await homePage.waitForReady();

    await homePage.navigation.signOut();
    expect(await page.url()).toContain('login');
  });

  test('User open Profile page', async () => {
    const homePage = new HomePage(page);
    await homePage.waitForReady();

    await homePage.navigation.navToProfile();

    const profile = new ProfilePage(page);
    await profile.waitForReady();
    expect(await page.url()).toContain('profile');
  });

  test('User open all-workspaces page', async () => {
    const homePage = new HomePage(page);
    await homePage.waitForReady();

    await homePage.navigation.navToMyWorkspaces();

    const workspaces = new WorkspaceEditPage(page);
    await workspaces.waitForReady();
    expect(await page.url()).toContain('workspaces');
  });

});
