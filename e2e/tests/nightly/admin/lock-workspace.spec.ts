import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import HomePage from 'app/page/home-page';
import WorkspaceAdminPage, {workspaceStatus} from 'app/page/admin-workspace-page';
import { ConceptSetSelectValue, Language } from 'app/text-labels';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceCard from 'app/component/workspace-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { Page } from 'puppeteer';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { MenuOption } from 'app/text-labels';


describe('Workspace Admin lock-workspace', () => {
  let workspaceName = 'e2eLockWorkspace';
  let reasonText = 'locking this workspace';
  let pyNotebookName = 'e2eLockWorkspaceNotebook';
  let workspaceNamespace = '';
  let workspaceEditedName = '';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_USER);
  });

  test('verify if workspace is locked', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();
    await workspacesPage.createWorkspace(workspaceName);
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();
    //extract the Workspace-Namespace
    workspaceNamespace = await dataPage.extractWorkspaceNamespace();
    //create the dataset and notebook
    await createDatasetNotebook(page, pyNotebookName);
    await navigation.navMenu(page, NavLink.WORKSPACE_ADMIN);
    const workspaceAdminPage = new WorkspaceAdminPage(page);
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.getWorkspaceNamespaceInput().type(workspaceNamespace);
    await workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();
    //click on the LOCK WORKSPACE button
    const lockWorkspaceModal = await workspaceAdminPage.clickLockWorkspaceButton(workspaceStatus.Lock);
    expect(await lockWorkspaceModal.getLockWorkspaceButton().isCursorNotAllowed()).toBe(true);
    await lockWorkspaceModal.createLockWorkspaceReason(reasonText);
    //const lockWorkspaceReason = await lockWorkspaceModal.createLockWorkspaceReason();
    expect(await lockWorkspaceModal.getLockWorkspaceButton().isCursorNotAllowed()).toBe(false);
    await lockWorkspaceModal.clickModalLockWorkspace();
    await workspaceAdminPage.waitForLoad();
    // verify the button now displays label "UNLOCK WORKSPACE"
    expect(workspaceAdminPage.getLockWorkspaceButton(workspaceStatus.Unlock));
    //await new WorkspacesPage(page).load();
    await new HomePage(page).load();
    const workspaceCard = await WorkspaceCard.findCard(page, workspaceName);
    const lockedIcon = workspaceCard.getWorkspaceLockedIcon();
    expect(lockedIcon).toBeTruthy();
    await workspaceCard.verifyLockedWorkspaceMenuOptions();
    await workspaceCard.clickLockedWorkspaceName(true);
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();
    // verify the workspace-locked banner displays
    const aboutLockReason = await aboutPage.extractReasonMessage();
    expect(aboutLockReason).toEqual(reasonText);
    const aboutLockedIcon = aboutPage.getAboutLockedWorkspaceIcon();
    expect(aboutLockedIcon).toBeTruthy();
    // verify DATA & ANALYSIS tabs are disabled 
    await aboutPage.openDataPage({ waitPageChange: false });
    await aboutPage.openAnalysisPage({ waitPageChange: false });
    // verify share button is disabled
    expect(await aboutPage.getShareButton().isCursorNotAllowed()).toBe(true);
    await aboutPage.verifyLockedWorkspaceActionOptions();
    // verify the user is able to navigate to edit page to update the description
    await aboutPage.getAboutEditIcon();
    const workspaceEdit = new WorkspaceEditPage(page);
    await workspaceEdit.waitForLoad();
    // verify user is able to edit the locked workspace
    workspaceEditedName = await workspaceEdit.fillOutRequiredDuplicationFields();
    const updateButton = workspaceEdit.getUpdateWorkspaceButton();
    await updateButton.waitUntilEnabled();
    await workspaceEdit.clickCreateFinishButton(updateButton);
    await aboutPage.waitForLoad();
  });

  test('Verify the workspace is unlocked', async () => {
    await navigation.navMenu(page, NavLink.WORKSPACE_ADMIN);
    const workspaceAdminPage = new WorkspaceAdminPage(page);
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.getWorkspaceNamespaceInput().type(workspaceNamespace);
    workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.clickUnlockWorkspaceButton(workspaceStatus.Unlock);
    await workspaceAdminPage.waitForLoad();
    expect(workspaceAdminPage.getLockWorkspaceButton(workspaceStatus.Lock));
    await new HomePage(page).load();
    const workspaceCard = await WorkspaceCard.findCard(page, workspaceEditedName);
    expect(await workspaceCard.getWorkspaceLockedIcon()).toBeFalsy();
    await workspaceCard.verifyWorkspaceCardMenuOptions();
    await workspaceCard.clickWorkspaceName(true);
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();
    // verify DATA & ANALYSIS tabs are accessible
    await dataPage.openAnalysisPage({ waitPageChange: true});
    await dataPage.openAboutPage({ waitPageChange: true});
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();
    expect(await aboutPage.getShareButton().isCursorNotAllowed()).toBe(false);
    expect(await aboutPage.getAboutLockedWorkspaceIcon()).toBeFalsy();
    const snowmanMenu = await aboutPage.getWorkspaceActionMenu();
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Share)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(false);
  });

  

    async function createDatasetNotebook(page: Page, pyNotebookName: string): Promise<NotebookPreviewPage> {
      const dataPage = new WorkspaceDataPage(page);
      await dataPage.waitForLoad();
      const datasetBuildPage = await dataPage.clickAddDatasetButton();

      // Step 1 Select Cohort: Choose "All Participants"
      await datasetBuildPage.selectCohorts(['All Participants']);

      // Step 2 Select Concept Sets (Rows): select Demographics checkbox.
      await datasetBuildPage.selectConceptSets([ConceptSetSelectValue.Demographics]);

      const createModal = await datasetBuildPage.clickCreateButton();
      await createModal.createDataset();

      const exportModal = await datasetBuildPage.clickAnalyzeButton();

      await exportModal.enterNotebookName(pyNotebookName);
      await exportModal.pickLanguage(Language.Python);
      await exportModal.clickExportButton();
      const notebookPreviewPage = new NotebookPreviewPage(page);
      return await notebookPreviewPage.waitForLoad();
    }

});


