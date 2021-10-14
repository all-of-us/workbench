import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import WorkspaceAdminPage from 'app/page/admin-workspace-page';
import { WorkspaceHeadings, CloudStorageHeader } from 'app/text-labels';
import RuntimePanel from 'app/component/runtime-panel';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceCard from 'app/component/workspace-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import AdminNotebookPreviewPage from 'app/page/admin-notebook-preview-page';

describe('Workspace Admin', () => {
  const workspaceNamespace = 'aou-rw-test-8c5cdbaf';
  const workspaceName = 'e2eWorkspaceAdmin';
  const noActiveRuntimeText = 'No active runtimes exist for this workspace.';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_USER);
    await navigation.navMenu(page, NavLink.WORKSPACE_ADMIN);
  });

  test('check the Workspace Admin page UI', async () => {
    const workspaceAdminPage = new WorkspaceAdminPage(page);
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.getWorkspaceNamespaceInput().type(workspaceNamespace);
    await workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();
    expect(await workspaceAdminPage.getWorkspaceHeader()).toEqual('Workspace');
    const headings3 = await workspaceAdminPage.getAllHeadings3();
    expect(headings3).toEqual(
      expect.arrayContaining([
        WorkspaceHeadings.BasicInformation,
        WorkspaceHeadings.Collaborators,
        WorkspaceHeadings.CohortBuilder,
        WorkspaceHeadings.CloudStorageObjects,
        WorkspaceHeadings.ResearchPurpose
      ])
    );
    const headings2 = await workspaceAdminPage.getAllHeadings2();
    expect(headings2).toEqual(
      expect.arrayContaining([WorkspaceHeadings.CloudStorageTraffic, WorkspaceHeadings.Runtimes])
    );
    const cloudStorageCols = await workspaceAdminPage.getcloudStorageColNames();
    expect(cloudStorageCols).toEqual(
      expect.arrayContaining([CloudStorageHeader.Location, CloudStorageHeader.Filename, CloudStorageHeader.FileSize])
    );
    expect(await workspaceAdminPage.getRuntimeDeleteButton().exists()).toBeFalsy();
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
    // verify that the notebook preview page loaded
    const previewCode = await adminNotebookPreviewPage.getFormattedCode();
    expect(previewCode.some((item) => item.includes('import pandas'))).toBe(true);
    expect(previewCode.some((item) => item.includes('import os'))).toBe(true);
    expect(await adminNotebookPreviewPage.getNamespaceText()).toEqual(workspaceNamespace);
    // click on the namespace link to navigate back to the workpsace admin page
    await adminNotebookPreviewPage.clickNamespaceLink();
    await workspaceAdminPage.waitForLoad();
  });

  test('Verify that admin is able to delete runtime', async () => {
    const workspaceAdminPage = new WorkspaceAdminPage(page);
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.getWorkspaceNamespaceInput().type(workspaceNamespace);
    workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();
    expect(await workspaceAdminPage.getNoActiveRuntimeText()).toEqual(noActiveRuntimeText);
    await new WorkspacesPage(page).load();
    const workspaceCard = await WorkspaceCard.findCard(page, workspaceName);
    await workspaceCard.clickWorkspaceName();
    await new WorkspaceDataPage(page).waitForLoad();
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.open();
    // Create runtime
    await runtimePanel.createRuntime();
    await page.waitForTimeout(2000);
    // navigate back to workspace admin page to delete the workspace
    await navigation.navMenu(page, NavLink.WORKSPACE_ADMIN);
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.getWorkspaceNamespaceInput().type(workspaceNamespace);
    workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();
    //verify the runtime status is running
    expect(await workspaceAdminPage.getRuntimeStatus()).toEqual('Running');
    const deleteRuntimeModal1 = await workspaceAdminPage.clickRuntimeDeleteButton();
    await deleteRuntimeModal1.clickCancelButton();
    await workspaceAdminPage.waitForLoad();
    const deleteRuntimeModal2 = await workspaceAdminPage.clickRuntimeDeleteButton();
    // delete the runtime
    await deleteRuntimeModal2.clickDeleteButton();
    await workspaceAdminPage.waitForLoad();
    //verify the runtime status is deleting
    expect(await workspaceAdminPage.getRuntimeDeleteStatus()).toEqual('Deleting');
  });
});
