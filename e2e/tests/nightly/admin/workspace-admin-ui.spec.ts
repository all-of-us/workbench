import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import WorkspaceAdminPage from 'app/page/admin-workspace-page';
import { WorkspaceHeadings } from 'app/text-labels';
import RuntimePanel from 'app/component/runtime-panel';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceCard from 'app/component/workspace-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import AdminNotebookPreviewPage from 'app/page/admin-notebook-preview-page';

describe('Workspace Admin', () => {
  const workspaceNamespace = 'aou-rw-test-8c5cdbaf';
  const workspaceName = 'e2eWorkspaceAdmin';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_USER);
    await navigation.navMenu(page, NavLink.WORKSPACE_ADMIN);
  });

  test('check the Workspace Admin page UI', async () => {
    const workspaceAdminPage = new WorkspaceAdminPage(page);
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.getWorkspaceNamespaceInput().type(workspaceNamespace);
    workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();
    expect(await workspaceAdminPage.getWorkspaceHeader()).toEqual('Workspace');
    const headings3 = await workspaceAdminPage.getAllHeadings3();
    expect(headings3).toEqual(
      expect.arrayContaining([
        WorkspaceHeadings.BasicInformation,
        WorkspaceHeadings.Collaborators,
        WorkspaceHeadings.CohortBuilder,
        WorkspaceHeadings.CloudStorageObjects,
        WorkspaceHeadings.ResearchPurpose,
      ])
    );
    const headings2 = await workspaceAdminPage.getAllHeadings2();
    expect(headings2).toEqual(
      expect.arrayContaining([
        WorkspaceHeadings.CloudStorageTraffic,
        WorkspaceHeadings.Runtimes,
      ])
    );
    expect(await workspaceAdminPage.getRuntimeDeleteButton().exists()).toBeFalsy();
    const noActiveRuntimeText = 'No active runtimes exist for this workspace';
    expect(await workspaceAdminPage.getNoActiveRuntimeText()).toEqual(noActiveRuntimeText);
  });

  test('Verify that admin is able to preview the Notebook', async () => {
    const workspaceAdminPage = new WorkspaceAdminPage(page);
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.getWorkspaceNamespaceInput().type(workspaceNamespace);
    workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();
    //verify that the Notebook Preview button is disabled
    expect(await workspaceAdminPage.getNotebookPreviewButton().isCursorNotAllowed()).toBe(true);
    const accessReasonText = 'for auditing purposes';
    await workspaceAdminPage.getAccessReasonInput().type(accessReasonText);
    //verify that the Notebook Preview button is now enabled
    expect(await workspaceAdminPage.getNotebookPreviewButton().isCursorNotAllowed()).toBe(false);
    await workspaceAdminPage.clickNotebookPreviewButton();
    const adminNotebookPreviewPage = new AdminNotebookPreviewPage(page);
    await adminNotebookPreviewPage.waitForLoad();
    const previewCode = await adminNotebookPreviewPage.getFormattedCode(); 
    expect(previewCode.some((item) => item.includes('import pandas'))).toBe(true);
    expect(previewCode.some((item) => item.includes('import os'))).toBe(true);
    await adminNotebookPreviewPage.clickNamespaceLink();
    await workspaceAdminPage.waitForLoad();
  });

  test('Verify that admin is able to delete runtime', async () => {
    const workspaceAdminPage = new WorkspaceAdminPage(page);
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.getWorkspaceNamespaceInput().type(workspaceNamespace);
    workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();
    const noActiveRuntimeText = 'No active runtimes exist for this workspace';
    expect(await workspaceAdminPage.getWorkspaceHeader()).toEqual(noActiveRuntimeText);
    await new WorkspacesPage(page).load();
    const workspaceCard = await WorkspaceCard.findCard(page, workspaceName);
    await workspaceCard.clickWorkspaceName();
    await new WorkspaceDataPage(page).waitForLoad();
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.open();
    // Create runtime
    await runtimePanel.createRuntime();
    await page.waitForTimeout(2000);
    await navigation.navMenu(page, NavLink.WORKSPACE_ADMIN);
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.getWorkspaceNamespaceInput().type(workspaceNamespace);
    workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();
    expect(await workspaceAdminPage.getRuntimeDeleteButton().exists()).toBeTruthy();
    let deleteRuntimeModal1 = await workspaceAdminPage.clickRuntimeDeleteButton();
    await deleteRuntimeModal1.clickCancelButton();
    await workspaceAdminPage.waitForLoad();
    let deleteRuntimeModal2 = await workspaceAdminPage.clickRuntimeDeleteButton();
    await deleteRuntimeModal2.clickDeleteButton();
    await workspaceAdminPage.waitForLoad();
    expect(await workspaceAdminPage.getRuntimeStatus()).toEqual('Deleting');
  });
});
