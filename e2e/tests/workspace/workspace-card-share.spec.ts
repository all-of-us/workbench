import ShareModal from 'app/component/share-modal';
import WorkspaceCard from 'app/component/workspace-card';
import Link from 'app/element/link';
import HomePage from 'app/page/home-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspacesPage from 'app/page/workspaces-page';
import {Option, LinkText, WorkspaceAccessLevel} from 'app/text-labels';
import {config} from 'resources/workbench-config';
import {createWorkspace, findOrCreateWorkspace, signInWithAccessToken, signInAs, signOut} from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {waitWhileLoading} from 'utils/waits-utils';

describe('Share workspace', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Assume there is at least one workspace preexist
  describe('From the workspace about page', () => {

    test('As OWNER, user can share a workspace', async () => {
      
      const workspaceCard = await findOrCreateWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const notebooksLink = await Link.findByName(page, {name: 'About'});
      await notebooksLink.clickAndWait();

      const aboutPage = new WorkspaceAboutPage(page);
      await aboutPage.waitForLoad();
       // if the collaborator is already on this workspace, just remove them before continuing.
       await aboutPage.removeCollab();

      let shareModal = await aboutPage.openShareModal();
      await shareModal.shareWithUser(config.collaboratorUsername, WorkspaceAccessLevel.Owner);
      // Collab list is refreshed.
      await waitWhileLoading(page);

      let accessLevel = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
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

      const workspaceCard = await createWorkspace(page);
      const workspaceName = await workspaceCard.getWorkspaceName();

      // Open the Share modal
      await workspaceCard.selectSnowmanMenu(Option.Share, {waitForNav: false});

      const shareModal = new ShareModal(page);
      await shareModal.waitUntilVisible();

      await shareModal.shareWithUser(config.collaboratorUsername, WorkspaceAccessLevel.Reader);
      await waitWhileLoading(page);
      await signOut(page);

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
      await workspaceCard2.verifyWorkspaceCardMenuOptions();

      // Make sure the Search input-field in Share modal is disabled.
      await workspaceCard2.clickWorkspaceName();
      await (new WorkspaceDataPage(newPage)).openAboutPage();
      const aboutPage = new WorkspaceAboutPage(newPage);
      await aboutPage.waitForLoad();
      const modal2 = await aboutPage.openShareModal();
      const searchInput = await modal2.waitForSearchBox();
      expect(await searchInput.isDisabled()).toBe(true);
      await modal2.clickButton(LinkText.Cancel);

      await signOut(newPage);
    });

  });

});
