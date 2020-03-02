import Link from '../../app/aou-elements/Link';
import WebElement from '../../app/aou-elements/WebElement';
import GoogleLoginPage from '../../app/GoogleLoginPage';
import HomePage, {FIELD_LABEL as editPageFieldLabel} from '../../app/HomePage';
import WorkspaceResourceCard from '../../app/page-mixin/WorkspaceCard';
import WorkspaceEditPage from "../../app/WorkspaceEditPage";
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
    // Enable networks requests inspection
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

  test('Workspace cards have same UI size', async () => {
    const home = new HomePage(page);
    await home.goToURL();

    const workspaceCards = new WorkspaceResourceCard(page);
    const cards = await workspaceCards.getAllCardsElements();
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
      await card.dispose();
    }
  });

  // Click See All Workspaces link => Opens Your Workspaces page
  test('Click See All Workspace link', async () => {
    const home = new HomePage(page);
    await home.goToURL();

    const seeAllWorkspacesLink = new Link(page);
    await seeAllWorkspacesLink.withLabel(editPageFieldLabel.SEE_ALL_WORKSPACES);
    await seeAllWorkspacesLink.click();
    const workspaces = new WorkspacesPage(page);
    await workspaces.waitForReady();
    expect(await workspaces.isLoaded()).toBe(true);
    await seeAllWorkspacesLink.dispose();
  });

  // Click Create New Workspace link => Opens Create Workspace page
  test('Click Create New Workspace link', async () => {
    const home = new HomePage(page);
    await home.goToURL();

    await home.getCreateNewWorkspaceLink()
      .then((link) => link.click());

    const workspaceEdit = new WorkspaceEditPage(page);
    await workspaceEdit.waitForReady();
    // expect Workspace name Input textfield exists and NOT disabled
    const workspaceNameTextbox = await workspaceEdit.getWorkspaceNameTextbox();
    expect(await workspaceNameTextbox.isVisible()).toBe(true);
  });


});
