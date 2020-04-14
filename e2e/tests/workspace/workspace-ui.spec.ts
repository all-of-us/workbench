import BaseElement from 'app/aou-elements/base-element';
import HomePage from 'app/home-page';
import WorkspaceCard from 'app/workspace-card';
import WorkspacesPage from 'app/workspaces-page';
import {signIn} from 'tests/app';
import {NavLink} from 'app/page-identifiers';


describe('Workspace ui tests', () => {

  beforeEach(async () => {
    await signIn(page);
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
    await workspaces.load();
    const workspaceEdit = await workspaces.clickCreateNewWorkspace();
    const workspaceNameTextbox = await workspaceEdit.getWorkspaceNameTextbox();
    expect(await workspaceNameTextbox.isVisible()).toBe(true);
  });

  test('Check Workspace card on Your Workspaces page', async () => {
    const home = new HomePage(page);
    await home.load();
    await home.navTo(NavLink.YOUR_WORKSPACES);
    await new WorkspacesPage(page).waitForLoad();

    await WorkspaceCard.getAllCards(page);
    const card = await WorkspaceCard.getAnyCard(page);
    const workspaceName = await card.getWorkspaceName();
    expect(workspaceName).toMatch(new RegExp(/^[a-zA-Z]+/));

    const links = await card.getPopupLinkTextsArray();
    expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));
  });


});
