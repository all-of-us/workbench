import WorkspaceCard from 'app/component/workspace-card';
import HomePage from 'app/page/home-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspacesPage from 'app/page/workspaces-page';
import {LinkText, WorkspaceAccessLevel} from 'app/text-labels';
import {config} from 'resources/workbench-config';
import {findOrCreateWorkspace, signIn, signInAs, signOut} from 'utils/test-utils';
import {makeWorkspaceName} from 'utils/str-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {waitWhileLoading} from 'utils/waits-utils';


describe('Share workspace', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  // Assume there is at least one workspace preexist
  describe('From the workspace about or data page via workspace actions menu', () => {

    test('As OWNER, user can share a workspace', async () => {
      
      const workspaceCard = await findOrCreateWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      await (new WorkspaceDataPage(page)).openAboutPage();
      const aboutPage = new WorkspaceAboutPage(page);
      await aboutPage.waitForLoad();
      
      // if the collaborator is already on this workspace, just remove them before continuing.
      await aboutPage.removeCollab();

      const shareWorkspaceModal = await aboutPage.shareWorkspace();
      await shareWorkspaceModal.shareWithUser(config.collaboratorUsername, WorkspaceAccessLevel.Owner);
      // Collab list is refreshed.
      await page.reload({waitUntil: ['networkidle0', 'domcontentloaded']});
      await waitWhileLoading(page);
      
      let accessLevel = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
      expect(accessLevel).toBe(WorkspaceAccessLevel.Owner);

      await aboutPage.shareWorkspace();
      await shareWorkspaceModal.removeUser(config.collaboratorUsername);
      await page.reload({waitUntil: ['networkidle0', 'domcontentloaded']});
      await waitWhileLoading(page);

      accessLevel = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
      expect(accessLevel).toBeNull();
    });


    /**
     * Test:
     * - Create a new workspace.
     * - Share with another user with role Writer.
     * - Log in as another user.
     * - Workspace share action should be disabled.
     */
    test('Workspace WRITER cannot share edit or delete workspace', async () => {

      const newWorkspaceName = makeWorkspaceName();
      const workspacesPage1 = new WorkspacesPage(page);
      await workspacesPage1.load();
  
      // create workspace with "No Review Requested" radiobutton selected
       await workspacesPage1.createWorkspace(newWorkspaceName);
  
      const dataPage = new WorkspaceDataPage(page);
      await dataPage.verifyWorkspaceNameOnDataPage(newWorkspaceName);
     
      const shareWorkspaceModal = await dataPage.shareWorkspace();
      await shareWorkspaceModal.shareWithUser(config.collaboratorUsername, WorkspaceAccessLevel.Writer);
     
      await waitWhileLoading(page);
      await signOut(page);

      // To verify WRITER role is assigned correctly, user with WRITER role will sign in in new Incognito page.
      const newPage = await signInAs(page, config.collaboratorUsername, config.userPassword);

      const homePage = new HomePage(newPage);
      await homePage.getSeeAllWorkspacesLink().then((link) => link.click());

      const workspacesPage = new WorkspacesPage(newPage);
      await workspacesPage.waitForLoad();

      // Verify Workspace Access Level is WRITER.
      const workspaceCard2 = await WorkspaceCard.findCard(newPage, newWorkspaceName);
      const accessLevel = await workspaceCard2.getWorkspaceAccessLevel();
      expect(accessLevel).toBe(WorkspaceAccessLevel.Writer);

      // Share, Edit and Delete actions are disabled.
      await workspaceCard2.workspaceCardMenuOptions();

      // Make sure the Search input-field in Share modal is disabled.
      await workspaceCard2.clickWorkspaceName();
      await (new WorkspaceDataPage(newPage)).openAboutPage();
      const aboutPage = new WorkspaceAboutPage(newPage);
      await aboutPage.waitForLoad();

      const accessLevel2 = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
      expect(accessLevel2).toBe(WorkspaceAccessLevel.Writer);

      const modal2 = await aboutPage.openShareModal();
      const searchInput = await modal2.waitForSearchBox();
      expect(await searchInput.isDisabled()).toBe(true);
      await modal2.clickButton(LinkText.Cancel);

      await signOut(newPage);
    });

    /**
     * Test:
     * - Create a new workspace.
     * - Share with another user with role READER.
     * - Log in as another user.
     * - Workspace share action should be disabled.
     * - Open Share modal to validate that the searchInput is disabled
     */
    test('Workspace READER cannot share edit or delete workspace', async () => {

      const newWorkspaceName = makeWorkspaceName();
      const workspacesPage1 = new WorkspacesPage(page);
      await workspacesPage1.load();
  
      // create workspace with "No Review Requested" radiobutton selected
       await workspacesPage1.createWorkspace(newWorkspaceName);
  
      const dataPage = new WorkspaceDataPage(page);
      await dataPage.verifyWorkspaceNameOnDataPage(newWorkspaceName);
     
      const shareWorkspaceModal = await dataPage.shareWorkspace();
      await shareWorkspaceModal.shareWithUser(config.collaboratorUsername, WorkspaceAccessLevel.Reader);
     
      await waitWhileLoading(page);
      await signOut(page);

      // To verify WRITER role is assigned correctly, user with READER role will sign in in new Incognito page.
      const newPage = await signInAs(page, config.collaboratorUsername, config.userPassword);

      const homePage = new HomePage(newPage);
      await homePage.getSeeAllWorkspacesLink().then((link) => link.click());

      const workspacesPage = new WorkspacesPage(newPage);
      await workspacesPage.waitForLoad();

      // Verify Workspace Access Level is READER.
      const workspaceCard2 = await WorkspaceCard.findCard(newPage, newWorkspaceName);
      const accessLevel = await workspaceCard2.getWorkspaceAccessLevel();
      expect(accessLevel).toBe(WorkspaceAccessLevel.Reader);

      // Share, Edit and Delete actions are disabled.
      await workspaceCard2.workspaceCardMenuOptions();

      // Make sure the Search input-field in Share modal is disabled.
      await workspaceCard2.clickWorkspaceName();
      await (new WorkspaceDataPage(newPage)).openAboutPage();
      const aboutPage = new WorkspaceAboutPage(newPage);
      await aboutPage.waitForLoad();

      const accessLevel1 = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
      expect(accessLevel1).toBe(WorkspaceAccessLevel.Reader);

      const modal2 = await aboutPage.openShareModal();
      const searchInput = await modal2.waitForSearchBox();
      expect(await searchInput.isDisabled()).toBe(true);
      await modal2.clickButton(LinkText.Cancel);

      await signOut(newPage);
    });

  });

});
