import {Page} from 'puppeteer';
import WebElement from '../../app/aou-elements/web-element';
import {NavLink} from '../../app/authenticated-page';
import HomePage from '../../app/home-page';
import WorkspaceCard from '../../app/workspace-card';
import WorkspacesPage from '../../app/workspaces-page';

const Chrome = require('../../driver/chrome-driver');

jest.setTimeout(2 * 60 * 1000);
describe('Workspace ui tests', () => {
  let page: Page;

  beforeEach(async () => {
    page = await Chrome.setup();
  });

  afterEach(async () => {
    await Chrome.teardown();
  });


  test('Workspace cards all have same ui size', async () => {
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
  });

  // Click CreateNewWorkspace link on My Workpsaces page => Open Create Workspace page
  test('Click Create New Workspace link on My Workspaces page', async () => {
    const workspaces = new WorkspacesPage(page);
    await workspaces.goToUrl();
    const workspaceEdit = await workspaces.clickCreateNewWorkspace();
    const workspaceNameTextbox = await workspaceEdit.getWorkspaceNameTextbox();
    expect(await workspaceNameTextbox.isVisible()).toBe(true);
  });

  test('Check Workspace card on Your Workspaces page', async () => {
    const home = new HomePage(page);
    await home.waitForReady();
    await home.goTo(NavLink.YOUR_WORKSPACES);
    await new WorkspacesPage(page).waitForReady();

    await WorkspaceCard.getAllCards(page);
    const anyCard = await WorkspaceCard.getAnyCard(page);
    const cardName = await anyCard.getResourceCardName();
    expect(cardName).toMatch(new RegExp(/^[a-zA-Z]+/));
    expect(await anyCard.getEllipsisIcon()).toBeTruthy();
    const links = await anyCard.getPopupLinkTextsArray();
    expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));
  });


});
