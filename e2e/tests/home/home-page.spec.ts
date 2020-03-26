import NewClrIconLink from '../../app/aou-elements/clr-icon-link';
import Link from '../../app/aou-elements/link';
import BaseElement from '../../app/aou-elements/base-element';
import GoogleLoginPage from '../../app/google-login';
import {FIELD_LABEL as editPageFieldLabel} from '../../app/home-page';
import WorkspaceCard from '../../app/workspace-card';
import WorkspaceEditPage from '../../app/workspace-edit-page';
import WorkspacesPage from '../../app/workspaces-page';
import launchBrowser from '../../driver/puppeteer-launch';

// set timeout globally per suite, not per test.
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


  test('Workspace cards have same ui size', async () => {
    await GoogleLoginPage.logIn(page);

    const cards = await WorkspaceCard.getAllCards(page);
    let width;
    let height;
    for (const card of cards) {
      const cardElem = new BaseElement(page, card.asElementHandle());
      expect(await cardElem.isVisible()).toBe(true);
      const size = await cardElem.getSize();
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

    const seeAllWorkspacesLink = await Link.forLabel(page, editPageFieldLabel.SEE_ALL_WORKSPACES);
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

    const anyLink = await NewClrIconLink.forLabel(page, editPageFieldLabel.CREATE_NEW_WORKSPACE, 'plus-circle');
    expect(anyLink).toBeTruthy();
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
    page
    .on('request', request =>
      console.log(`request initiated: ${request.url()}`))
    // .on('console', message =>
    //   console.log(`console error: ${message.type().substr(0, 3).toUpperCase()} ${message.text()}`))
    // .on('pageerror', ({ message }) => console.log(`page error: ${message}`))
    .on('response', response =>
       console.log(`response: ${response.status()} ${response.url()}`))
    .on('requestfinished', request =>
       console.log(`request finished: ${request.url()} `))
    .on('requestfailed', request =>
       console.log(`request failed: ${request.url()} ${request.failure().errorText} `));

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
