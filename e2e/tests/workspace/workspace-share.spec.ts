import Navigation, {NavLink} from 'app/component/navigation';
import ShareModal from 'app/component/share-modal';
import WorkspaceCard from 'app/component/workspace-card';
import Link from 'app/element/link';
import HomePage from 'app/page/home-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspacesPage from 'app/page/workspaces-page';
import {EllipsisMenuAction, LinkText, WorkspaceAccessLevel} from 'app/text-labels';
import {config} from 'resources/workbench-config';
import {findWorkspace, signIn, signInAs, waitWhileLoading} from 'utils/test-utils';
import DataPage, {TabLabelAlias} from 'app/page/data-page';

describe('Share workspace', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  // Assume there is at least one workspace preexist
  describe('From the workspace about page', () => {
    test('As OWNER, user can share a workspace', async () => {
      // Using the share modal requires a viewport be set, due to react-select weirdness.
      await page.setViewport({height: 1280, width: 1280});

      const workspaceCard = await findWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const notebooksLink = await Link.findByName(page, {name: 'About'});
      await notebooksLink.clickAndWait();

      const aboutPage = new WorkspaceAboutPage(page);
      await aboutPage.waitForLoad();

      // This test is not hermetic - if the collaborator is already on this
      // workspace, just remove them before continuing.
      let accessLevel = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
      if (accessLevel !== null) {
        await (await aboutPage.openShareModal()).removeUser(config.collaboratorUsername);
        await waitWhileLoading(page);
      }

      let shareModal = await aboutPage.openShareModal();
      await shareModal.shareWithUser(
        config.collaboratorUsername, WorkspaceAccessLevel.Owner);
      // Collab list is refreshed.
      await waitWhileLoading(page);

      accessLevel = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
      expect(accessLevel).toBe(WorkspaceAccessLevel.Owner);

      shareModal = await aboutPage.openShareModal();
      await shareModal.removeUser(config.collaboratorUsername);
      await waitWhileLoading(page);

      accessLevel = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
      expect(accessLevel).toBeNull();
    });


    /**
     * Test:
     * - Create a new workspace.
     * - Share with another user with role READER.
     * - Log in as another user.
     * - Workspace share action should be disabled.
     */
    test('Workspace READER cannot share edit or delete workspace', async () => {

      const workspaceCard = await findWorkspace(page, {create: true});
      const workspaceName = await workspaceCard.getWorkspaceName();

      // Open the Share modal
      await workspaceCard.clickEllipsisAction(EllipsisMenuAction.Share, {waitForNav: false});

      const shareModal = new ShareModal(page);
      await shareModal.waitUntilVisible();

      await shareModal.shareWithUser(config.collaboratorUsername, WorkspaceAccessLevel.Reader);
      await waitWhileLoading(page);
      await Navigation.navMenu(page, NavLink.SIGN_OUT);

      // browser and page reset.
      await page.deleteCookie(...await page.cookies());
      await jestPuppeteer.resetPage();
      await jestPuppeteer.resetBrowser();

      // To verify READER role assigned correctly, user with READER role will sign in in new Incognito page.
      const newPage = await signInAs(config.collaboratorUsername, config.userPassword);

      const homePage = new HomePage(newPage);
      await homePage.getSeeAllWorkspacesLink().then((link) => link.click());

      const workspacesPage = new WorkspacesPage(newPage);
      await workspacesPage.waitForLoad();

      // Verify Workspace Access Level is READER.
      const workspaceCard2 = await WorkspaceCard.findCard(newPage, workspaceName);
      const accessLevel = await workspaceCard2.getWorkspaceAccessLevel();
      expect(accessLevel).toBe(WorkspaceAccessLevel.Reader);

      // Share, Edit and Delete actions are not available for click.
      const menu2 = workspaceCard2.getEllipsis();
      await menu2.clickEllipsis();
      expect(await menu2.isDisabled(EllipsisMenuAction.Share)).toBe(true);
      expect(await menu2.isDisabled(EllipsisMenuAction.Edit)).toBe(true);
      expect(await menu2.isDisabled(EllipsisMenuAction.Delete)).toBe(true);

      // Duplicate action is available for click.
      expect(await menu2.isDisabled(EllipsisMenuAction.Duplicate)).toBe(false);

      // Make sure the Search input-field in Share modal is disabled.
      await workspaceCard2.clickWorkspaceName();
      await (new DataPage(newPage)).openTab(TabLabelAlias.About);
      const aboutPage = new WorkspaceAboutPage(newPage);
      await aboutPage.waitForLoad();
      const modal2 = await aboutPage.openShareModal();
      const searchInput = await modal2.waitForSearchBox();
      expect(await searchInput.isDisabled()).toBe(true);
      await modal2.clickButton(LinkText.Cancel);

      await Navigation.navMenu(newPage, NavLink.SIGN_OUT);
      await newPage.close();
    });

  });

});
