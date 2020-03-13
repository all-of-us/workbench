import BaseElement from '../../app/aou-elements/base-element';
import {PageUrl, SideNavLink} from '../../app/authenticated-page';
import HomePage from '../../app/home-page';
import WorkspaceCard from '../../app/workspace-card';
import WorkspacesPage from '../../app/workspaces-page';
import {signIn} from '../app';


// set timeout globally per suite, not per test.
jest.setTimeout(2 * 60 * 1000);

describe('Workspace ui tests', () => {

  beforeAll(async () => {
    // tests are using same incognito page. sign-in is only required once at the beginning.
    await signIn(page);
  });

  afterEach(async () => {
    await jestPuppeteer.resetPage();
  });

  test('Workspace cards all have same ui size', async () => {
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
  });

  // Click CreateNewWorkspace link on My Workpsaces page => Open Create Workspace page
  test('Click Create New Workspace link on My Workspaces page', async () => {
    const workspaces = new WorkspacesPage(page);
    await workspaces.loadPageUrl(PageUrl.WORKSPACES);
    const workspaceEdit = await workspaces.clickCreateNewWorkspace();
    const workspaceNameTextbox = await workspaceEdit.getWorkspaceNameTextbox();
    expect(await workspaceNameTextbox.isVisible()).toBe(true);
    await workspaceEdit.navTo(SideNavLink.HOME);
  });

  test('Check Workspace card on Your Workspaces page', async () => {
    const home = new HomePage(page);
    await home.load();
    await home.navTo(SideNavLink.YOUR_WORKSPACES);
    await new WorkspacesPage(page).waitForLoad();

    await WorkspaceCard.getAllCards(page);
    const anyCard = await WorkspaceCard.getAnyCard(page);
    const cardName = await anyCard.getResourceCardName();
    expect(cardName).toMatch(new RegExp(/^[a-zA-Z]+/));
    expect(await anyCard.getEllipsisIcon()).toBeTruthy();
    const links = await anyCard.getPopupLinkTextsArray();
    expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));
  });


});
