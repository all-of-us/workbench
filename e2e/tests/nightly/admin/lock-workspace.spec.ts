import { createDatasetNotebook, openTab, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import HomePage from 'app/page/home-page';
import WorkspaceAdminPage, { workspaceStatus } from 'app/page/admin/admin-workspace-page';
import { Tabs } from 'app/text-labels';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceCard from 'app/component/card/workspace-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { MenuOption } from 'app/text-labels';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import { logger } from 'libs/logger';

describe('Workspace Admin lock-workspace', () => {
  const workspaceName = 'e2eLockWorkspace';
  const reasonText = 'locking this workspace';
  const pyNotebookName = 'e2eLockWorkspaceNotebook';
  let workspaceNamespace = 'aou-rw-test-94cfc175';
  let workspaceEditedName = '';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_USER);
  });

  test('verify if workspace is locked', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();
    await workspacesPage.createWorkspace(workspaceName);
    const dataPage = new WorkspaceDataPage(page);
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
    // verify the lock-workspace is enables only after the locking-reason is typed
    expect(await lockWorkspaceModal.getLockWorkspaceButton().isCursorNotAllowed()).toBe(true);
    await lockWorkspaceModal.createLockWorkspaceReason(reasonText);
    expect(await lockWorkspaceModal.getLockWorkspaceButton().isCursorNotAllowed()).toBe(false);

    await lockWorkspaceModal.clickModalLockWorkspace();
    await workspaceAdminPage.waitForLoad();

    // verify the button now displays label "UNLOCK WORKSPACE"
    expect(workspaceAdminPage.getLockWorkspaceButton(workspaceStatus.Unlock));
    await new HomePage(page).load();
    const workspaceCard = await new WorkspaceCard(page).findCard({ name: workspaceName });
    const lockedIcon = workspaceCard.getWorkspaceLockedIcon();
    expect(lockedIcon).toBeTruthy();
    // verify only the edit option is enabled on the snowmenu
    await workspaceCard.verifyLockedWorkspaceMenuOptions();
    const aboutPage = new WorkspaceAboutPage(page);
    await workspaceCard.clickName({ pageExpected: aboutPage });

    // verify the banner includes the locking reason
    const aboutLockReason = await aboutPage.extractReasonMessage();
    expect(aboutLockReason).toEqual(reasonText);
    // verify the lock icon is displaying for the locked workspace
    const aboutLockedIcon = aboutPage.getAboutLockedWorkspaceIcon();
    expect(aboutLockedIcon).toBeTruthy();

    // verify DATA & ANALYSIS tabs are inactive for locked workspace
    const analysisTab = await aboutPage.getTabState(Tabs.Analysis);
    expect(analysisTab).toBe(false);
    const dataTab = await aboutPage.getTabState(Tabs.Data);
    expect(dataTab).toBe(false);

    // verify share button is disabled
    expect(await aboutPage.getShareButton().isCursorNotAllowed()).toBe(true);
    await aboutPage.verifyLockedWorkspaceActionOptions();
    // verify the user is able to navigate to edit page to update the description
    await aboutPage.getAboutEditIcon();
    const workspaceEdit = new WorkspaceEditPage(page);

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
    await workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.clickUnlockWorkspaceButton(workspaceStatus.Unlock);
    await workspaceAdminPage.waitForLoad();
    expect(workspaceAdminPage.getLockWorkspaceButton(workspaceStatus.Lock));
    await new HomePage(page).load();
    // find the edited-unlocked workspace
    const workspaceCard = await new WorkspaceCard(page).findCard({ name: workspaceEditedName });
    expect(await workspaceCard.getWorkspaceLockedIcon()).toBeFalsy();
    await workspaceCard.verifyWorkspaceCardMenuOptions();

    const dataPage = new WorkspaceDataPage(page);
    await workspaceCard.clickName({ pageExpected: dataPage });

    // verify DATA & ANALYSIS tabs are active and accessible
    const analysisPage = new WorkspaceAnalysisPage(page);
    await openTab(page, Tabs.Analysis, analysisPage);
    const aboutPage = new WorkspaceAboutPage(page);
    await openTab(page, Tabs.About, aboutPage);
    // the share button is enabled
    expect(await aboutPage.getShareButton().isCursorNotAllowed()).toBe(false);
    expect(await aboutPage.getAboutLockedWorkspaceIcon()).toBeFalsy();
    const snowmanMenu = await aboutPage.getWorkspaceActionMenu();
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Share)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(false);
    await aboutPage.getAboutEditIcon();
    const workspaceEdit = new WorkspaceEditPage(page);
    await workspaceEdit.waitForLoad();
    await workspaceEdit.clickCancelButton();
    // cleanup
    try {
      await dataPage.deleteWorkspace();
    } catch (error) {
      // we'll proceed, but let's report it
      logger.warn('fail to delete workspace');
    }
  });
});
