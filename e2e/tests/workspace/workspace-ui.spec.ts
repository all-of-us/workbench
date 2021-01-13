import BaseElement from 'app/element/base-element';
import HomePage from 'app/page/home-page';
import WorkspaceCard from 'app/component/workspace-card';
import WorkspacesPage from 'app/page/workspaces-page';
import {signIn} from 'utils/test-utils';
import Navigation, {NavLink} from 'app/component/navigation';

describe('Workspace ui tests', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('Workspace cards all have same ui size', async () => {
    // on Home page
    const homePage = new HomePage(page);
    await homePage.waitForLoad();
    expect(await homePage.isLoaded()).toBe(true);

    const cards = await WorkspaceCard.findAllCards(page);
    let width;
    let height;
    for (const card of cards) {
      const cardElem = BaseElement.asBaseElement(page, card.asElementHandle());
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

    await WorkspaceCard.findAllCards(page);
    const card = await WorkspaceCard.findAnyCard(page);

    // check workspace name string is made of english characters
    const workspaceName = await card.getWorkspaceName();
    await expect(workspaceName).toMatch(new RegExp(/^[a-zA-Z]+/));

    const levels = ['WRITER', 'READER', 'OWNER'];
    const accessLevel = await card.getWorkspaceAccessLevel();
    expect(levels).toContain(accessLevel);

    const snowmanMenu = await card.getSnowmanMenu();
    const links = await snowmanMenu.getAllOptionTexts();
    expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));
  });

  test('Click CANCEL button bring user back to the Workspaces page', async () => {
    const workspaces = new WorkspacesPage(page);
    await workspaces.load();

    // Click Create New Workspace link on the Workspaces page
    const editPage = await workspaces.clickCreateNewWorkspace();

    await (await editPage.getWorkspaceNameTextbox()).type('I-love-my-new-workspace');
    await (await editPage.getWorkspaceNameTextbox()).pressTab();

    // No Confirm to Cancel confirmation dialog
    const cancelButton = await editPage.getCancelButton();
    await cancelButton.clickAndWait();

    await workspaces.waitForLoad();
    expect(await workspaces.isLoaded()).toBe(true);
  });

});
