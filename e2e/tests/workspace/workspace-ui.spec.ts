import BaseElement from 'app/aou-elements/base-element';
import HomePage from 'app/home-page';
import WorkspaceCard from 'app/workspace-card';
import WorkspacesPage from 'app/workspaces-page';
import {signIn} from 'tests/app';
import Navigation, {NavLink} from 'app/navigation';

describe('Workspace ui tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Workspace cards all have same ui size', async () => {
    // on Home page
    expect(await new HomePage(page).isLoaded()).toBe(true);

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

  test('Check Workspace cards on the Workspaces page', async () => {
    const home = new HomePage(page);
    await home.load();
    
    await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
    await new WorkspacesPage(page).waitForLoad();

    await WorkspaceCard.getAllCards(page);
    const card = await WorkspaceCard.getAnyCard(page);

    // check workspace name string is made of english characters
    const workspaceName = await card.getWorkspaceName();
    expect(workspaceName).toMatch(new RegExp(/^[a-zA-Z]+/));

    const levels = ['WRITER', 'READER', 'OWNER'];
    const accessLevel = await card.getWorkspaceAccessLevel();
    expect(levels).toContain(accessLevel);

    const ellipsis = await card.getEllipsis();
    const links = await ellipsis.getAvaliableActions();
    expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));
  });

  test('Click CANCEL button bring user back to the Workspaces page', async () => {
    const workspaces = new WorkspacesPage(page);
    await workspaces.load();

    // Click Create New Workspace link on the Workspaces page
    const editPage = await workspaces.clickCreateNewWorkspace();

    await (await editPage.getWorkspaceNameTextbox()).type('I-love-my-new-workspace');
    await (await editPage.getWorkspaceNameTextbox()).tabKey();

    // No Confirm to Cancel confirmation dialog
    const cancelButton = await editPage.getCancelButton();
    await editPage.clickAndWait(cancelButton);

    await workspaces.waitForLoad();
    expect(await workspaces.isLoaded()).toBe(true);
  });

});
