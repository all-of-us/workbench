import Link from '../../app/aou-elements/Link';
import WebElement from '../../app/aou-elements/WebElement';
import GoogleLoginPage from '../../app/GoogleLoginPage';
import HomePage from '../../app/HomePage';
import WorkspaceResourceCard from '../../app/mixin-pages/WorkspaceCard';
import WorkspacesPage from '../../app/WorkspacesPage';
import launchBrowser from '../../driver/puppeteer-launch';

jest.setTimeout(60 * 1000);

const configs = require('../../resources/workbench-config');

describe('Home', () => {
  let browser;
  let page;

  beforeAll(async () => {
    browser = await launchBrowser();
  });

  beforeEach(async () => {
    page = await browser.newPage();
    await page.setUserAgent(configs.puppeteerUserAgent);
  });

  afterEach(async () => {
    await page.close();
    await page.waitFor(1000);
  });

  afterAll(async () => {
    await browser.close();
  });

  test('Homepage is the landing page after Sign In', async () => {

    // Inspect some important api
    await page.setRequestInterception(true);

    // Following network requests expected to happen
    const targetRequestsUrls = [
      configs.apiBaseUrl+'/status-alert',
      configs.apiBaseUrl+'/config',
      configs.apiBaseUrl+'/profile',
      configs.apiBaseUrl+'/workspaces',
      configs.apiBaseUrl+'/cdrVersions',
      configs.apiBaseUrl+'/workspaces/user-recent-workspaces',
      configs.apiBaseUrl+'/workspaces/user-recent-resources',
    ];
    const requestsUrls = [];
    const requestsFailed = [];
    const pageErrors = [];

    page.on('request', request => {
      request.continue();
    });

    // Catch console log errors
    page.on('pageerror', err => {
      pageErrors.push(`${err.toString()}`);
    });

    page.on('response', response => {
      const request = response.request();
      const url = request.url().split('?')[0].split('#')[0];
      const status = response.status();
      if (targetRequestsUrls.includes(url)) {
        requestsUrls.push(request.url());
        // console.log('response url:', url, 'status:', status);
        if (status !== 200) {
          requestsFailed.push(request.url());
        }
      }
    });

    const loginPage = new GoogleLoginPage(page);
    await loginPage.login();

    const home = new HomePage(page);
    await home.waitForReady();
    const title = await page.title();
    expect(title).toMatch('Homepage');

    // Check targeted api succeeded
    expect(requestsUrls).toEqual(expect.arrayContaining(targetRequestsUrls));
    // Expect zero request fail
    expect(requestsFailed.length).toEqual(0);
    // Expect zero page error
    expect(pageErrors.length).toEqual(0);

  });

  test('Workspace cards have same size on page', async () => {

    await page.goto(configs.uiBaseUrl, {waitUntil: 'networkidle0'});
    const home = new HomePage(page);
    await home.waitForReady();

    const workspaceCards = new WorkspaceResourceCard(page);
    const cards = await workspaceCards.getAllCards();
    let width;
    let height;
    for (const card of cards) {
      const cardElem = new WebElement(page, card);
      expect(await cardElem.isVisible()).toBe(true);
      const size = await cardElem.size();
      if (width === undefined) {
        width = size.width; // Initialize width and height with first card element's size, compare with rest cards
        height = size.height;
      } else {
        expect(size.height).toEqual(height);
        expect(size.width).toEqual(width);
      }
    }

  });

  test('See All Workspace link works', async () => {

    const home = new HomePage(page);
    await home.navigateToURL();

    // Verify See all Workspaces link
    const seeAllWorkspacesLink = new Link(page);
    await seeAllWorkspacesLink.withLabel('See all Workspaces');
    expect(await seeAllWorkspacesLink.isVisible()).toBe(true);
    // Click it to verify works
    await seeAllWorkspacesLink.click();
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.isLoaded();

  });

  test('Application NavBar links works', async () => {

    const home = new HomePage(page);
    await home.navigateToURL();

    // Verify app-nav-bar link works
    await home.navigation.navToMyWorkspaces();
    const workspacesPage = new WorkspacesPage(page);
    expect(await workspacesPage.isLoaded()).toBe(true);

    await home.navigation.navToHome();
    expect(await home.isLoaded()).toBe(true);

  });

});
