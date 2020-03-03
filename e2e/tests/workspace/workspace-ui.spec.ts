import {Page} from 'puppeteer';
import WebElement from '../../app/aou-elements/WebElement';
import WorkspaceCard from '../../app/WorkspaceCard';
import WorkspacesPage from '../../app/WorkspacesPage';

const Chrome = require('../../driver/ChromeDriver');
jest.setTimeout(60 * 1000);

describe('Workspace', () => {

  let page: Page;

  beforeEach(async () => {
    page = await Chrome.setup();
  });

  afterEach(async () => {
    await Chrome.teardown();
  });

  test('Workspace cards have same UI size', async () => {
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
    await workspaces.goToURL();
    const workspaceEdit = await workspaces.clickCreateNewWorkspace();
    const workspaceNameTextbox = await workspaceEdit.getWorkspaceNameTextbox();
    expect(await workspaceNameTextbox.isVisible()).toBe(true);
  });


});
