import NewClrIconLink from '../../app/aou-elements/clr-icon-link';
import Link from '../../app/aou-elements/link';
import WebElement from '../../app/aou-elements/web-element';
import GoogleLoginPage from '../../app/google-login';
import {FIELD_LABEL as editPageFieldLabel} from '../../app/home-page';
import WorkspaceCard from '../../app/workspace-card';
import WorkspaceEditPage from '../../app/workspace-edit-page';
import WorkspacesPage from '../../app/workspaces-page';
import launchBrowser from '../../driver/puppeteer-launch';

jest.setTimeout(2 * 60 * 1000);

const configs = require('../../resources/workbench-config');

describe('Home page ui tests', () => {
  let browser;
  let page;

  beforeEach(async () => {
    browser = await launchBrowser();
    page = await browser.newPage();
    await page.setUserAgent(configs.puppeteerUserAgent);
  });

  afterEach(async () => {
    await browser.close();
  });

  test('Homepage is the landing page after Sign In', async () => {
    // Enable networks requests inspection
    await page.setRequestInterception(true);

    // Following network requests expected to happen
    const targetRequestsUrls = [
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

    await GoogleLoginPage.logIn(page);
    const title = await page.title();
    expect(title).toMatch('Homepage');

    // Check targeted api succeeded
    expect(requestsUrls).toEqual(expect.arrayContaining(targetRequestsUrls));
    // Expect zero request fail
    expect(requestsFailed.length).toEqual(0);
    // Expect zero page error
    expect(pageErrors.length).toEqual(0);

    await page.setRequestInterception(false);
  });

  test('Workspace cards have same ui size', async () => {
    await GoogleLoginPage.logIn(page);

    const cards = await WorkspaceCard.getAllCards(page);
    let width;
    let height;
    for (const card of cards) {
      const cardElem = new WebElement(page, card.asElementHandle());
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
    expect(height).toBeGreaterThan(1);
    expect(width).toBeGreaterThan(1);
  });

  // Click See All Workspaces link => Opens Your Workspaces page
  test('Click on See All Workspace link', async () => {
    await GoogleLoginPage.logIn(page);

    const seeAllWorkspacesLink = new Link(page);
    await seeAllWorkspacesLink.withLabel(editPageFieldLabel.SEE_ALL_WORKSPACES);
    await seeAllWorkspacesLink.click();
    const workspaces = new WorkspacesPage(page);
    await workspaces.waitForLoad();
    expect(await workspaces.isLoaded()).toBe(true);
    await seeAllWorkspacesLink.dispose();
  });

  // Click Create New Workspace link => Opens Create Workspace page
  test('Click on Create New Workspace link', async () => {
    const home = await GoogleLoginPage.logIn(page);
    await home.getCreateNewWorkspaceLink().then((link) => link.click());

    const workspaceEdit = new WorkspaceEditPage(page);
    await workspaceEdit.waitForLoad();
    // expect Workspace name Input textfield exists and NOT disabled
    const workspaceNameTextbox = await workspaceEdit.getWorkspaceNameTextbox();
    expect(await workspaceNameTextbox.isVisible()).toBe(true);
  });

  test('Check Create New Workspace link on Home page', async () => {
    await GoogleLoginPage.logIn(page);

    const anyLink = new NewClrIconLink(page);
    const linkHandle = await anyLink.withLabel(editPageFieldLabel.CREATE_NEW_WORKSPACE, 'plus-circle');
    expect(linkHandle).toBeTruthy();
    const classname = await anyLink.getProperty('className');
    expect(classname).toBe('is-solid');
    const shape = await anyLink.getAttribute('shape');
    expect(shape).toBe('plus-circle');
    const hasShape = await anyLink.hasAttribute('shape');
    expect(hasShape).toBe(true);
    const disabled = await anyLink.isDisabled();
    expect(disabled).toBe(false);
    const cursor = await anyLink.getComputedStyle('cursor');
    expect(cursor).toBe('pointer');
    expect(await anyLink.isVisible()).toBe(true);

    await anyLink.dispose();
    expect(await anyLink.isVisible()).toBe(false);
  });

  test('Check Workspace card on Home page', async () => {
    await GoogleLoginPage.logIn(page);

    await WorkspaceCard.getAllCards(page);
    const anyCard = await WorkspaceCard.getAnyCard(page);
    const cardName = await anyCard.getResourceCardName();
    expect(cardName).toMatch(new RegExp(/^[a-zA-Z]+/));
    expect(await anyCard.getEllipsisIcon()).toBeTruthy();
    const links = await anyCard.getPopupLinkTextsArray();
    expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));
  });

});
