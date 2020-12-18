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



const collab = [
   {accessLevel: WorkspaceAccessLevel.Writer},
   {accessLevel: WorkspaceAccessLevel.Reader}
]

describe('Share workspace', () => {

    beforeEach(async () => {
      await signIn(page);
    });
  
    // Assume there is at least one workspace preexist
    describe('From workspace about or data page via workspace action menu', () => {

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


      test.each(collab)('AS %s, user cannot share, edit or delete workspace', async(collab) => {
        const newWorkspaceName = makeWorkspaceName();
        const workspacesPage1 = new WorkspacesPage(page);
        await workspacesPage1.load();
    
        // create workspace with "No Review Requested" radiobutton selected
         await workspacesPage1.createWorkspace(newWorkspaceName);
    
        const dataPage = new WorkspaceDataPage(page);
        await dataPage.verifyWorkspaceNameOnDataPage(newWorkspaceName);
    
        const shareWorkspaceModal = await dataPage.shareWorkspace();
       
        await shareWorkspaceModal.shareWithUser(config.collaboratorUsername, collab.accessLevel);
    
        await waitWhileLoading(page);
        await signOut(page);
    
        // To verify access level is assigned correctly, user with WRITER/READER role will sign in in new Incognito page.
        const newPage = await signInAs(config.collaboratorUsername, config.userPassword);
    
        const homePage = new HomePage(newPage);
        await homePage.getSeeAllWorkspacesLink().then((link) => link.click());
    
        const workspacesPage = new WorkspacesPage(newPage);
        await workspacesPage.waitForLoad();
    
        // Verify Workspace Access Level is wRITER/READER.
        const workspaceCard2 = await WorkspaceCard.findCard(newPage, newWorkspaceName);
        const accessLevel = await workspaceCard2.getWorkspaceAccessLevel();
        expect(accessLevel).toBe(collab.accessLevel);
    
        // Share, Edit and Delete actions are disabled.
        await workspaceCard2.workspaceCardMenuOptions();
    
        // Make sure the Search input-field in Share modal is disabled.
        await workspaceCard2.clickWorkspaceName();
        await (new WorkspaceDataPage(newPage)).openAboutPage();
        const aboutPage = new WorkspaceAboutPage(newPage);
        await aboutPage.waitForLoad();
    
        const accessLevel1 = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
        expect(accessLevel1).toBe(collab.accessLevel);
    
        const modal2 = await aboutPage.openShareModal();
        const searchInput = await modal2.waitForSearchBox();
        expect(await searchInput.isDisabled()).toBe(true);
        await modal2.clickButton(LinkText.Cancel);
    
        await signOut(newPage);
    
      });
    });
  
  });



