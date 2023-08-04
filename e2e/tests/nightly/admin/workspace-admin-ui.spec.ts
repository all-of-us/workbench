import { createDatasetNotebook, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import WorkspaceAdminPage, { workspaceStatus } from 'app/page/admin/admin-workspace-page';
import { CloudStorageHeader } from 'app/text-labels';
import RuntimePanel from 'app/sidebar/runtime-panel';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceCard from 'app/component/card/workspace-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import AdminNotebookPreviewPage from 'app/page/admin/admin-notebook-preview-page';

describe.skip('Workspace Admin', () => {
  const workspaceName = 'e2eAdminWorkspace';
  const pyNotebookName = 'e2eAdminNotebook';
  const reasonText = 'locking this workspace';
  let workspaceNamespace = '';

  beforeEach(async () => {
    await signInWithAccessToken(page, config.ADMIN_TEST_USER);
  });

  test('Verify the lock-Workspace feature and the Workspace Admin page UI', async () => {
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

    //verify if the admin navigated to correct workspace-Namespace-admin page
    expect(await workspaceAdminPage.getWorkspaceNamespaceText()).toEqual(workspaceNamespace);

    const cloudStorageCols = await workspaceAdminPage.getcloudStorageColNames();
    expect(cloudStorageCols).toEqual(
      expect.arrayContaining([CloudStorageHeader.Location, CloudStorageHeader.Filename, CloudStorageHeader.FileSize])
    );
    //verify that this newly created workspace has NO active runtime and also expect the RUNTIME DELETE button is falsy.
    expect(await workspaceAdminPage.getRuntimeDeleteButton().exists()).toBeFalsy();

    //click on the LOCK WORKSPACE button
    const lockWorkspaceModal = await workspaceAdminPage.clickLockWorkspaceButton(workspaceStatus.Lock);
    expect(await lockWorkspaceModal.getLockWorkspaceButton().isCursorNotAllowed()).toBe(true);

    await lockWorkspaceModal.createLockWorkspaceReason(reasonText);
    expect(await lockWorkspaceModal.getLockWorkspaceButton().isCursorNotAllowed()).toBe(false);
    await lockWorkspaceModal.clickCancelButton();
    await workspaceAdminPage.waitForLoad();
  });

  test('Verify that admin is able to preview the Notebook', async () => {
    await navigation.navMenu(page, NavLink.WORKSPACE_ADMIN);
    const workspaceAdminPage = new WorkspaceAdminPage(page);
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.getWorkspaceNamespaceInput().type(workspaceNamespace);
    await workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();

    //verify that the Notebook Preview button is disabled
    expect(await workspaceAdminPage.getNotebookPreviewButton().isCursorNotAllowed()).toBe(true);
    const accessReasonText = 'testing purposes';
    await workspaceAdminPage.getAccessReasonTextArea().type(accessReasonText);

    //verify that the Notebook Preview button is now enabled
    expect(await workspaceAdminPage.getNotebookPreviewButton().isCursorNotAllowed()).toBe(false);
    await workspaceAdminPage.clickNotebookPreviewButton();
    const adminNotebookPreviewPage = new AdminNotebookPreviewPage(page);
    await adminNotebookPreviewPage.waitForLoad();

    // verify that the notebook preview page loaded
    const previewCode = await adminNotebookPreviewPage.getFormattedCode();

    // verify that the notebook file is in Python programming language
    expect(previewCode.some((item) => item.includes('import pandas'))).toBe(true);
    expect(previewCode.some((item) => item.includes('import os'))).toBe(true);
    expect(await adminNotebookPreviewPage.getNamespaceText()).toEqual(workspaceNamespace);

    // click on the namespace link to navigate back to the workpsace-admin page
    await adminNotebookPreviewPage.clickNamespaceLink();
    await workspaceAdminPage.waitForLoad();
  });

  test('Verify that admin is able to delete runtime', async () => {
    await new WorkspacesPage(page).load();
    const workspaceCard = await new WorkspaceCard(page).findCard({ name: workspaceName });
    await workspaceCard.clickName();
    const dataPage = await new WorkspaceDataPage(page).waitForLoad();
    //extract the Workspace-Namespace
    workspaceNamespace = await dataPage.extractWorkspaceNamespace();
    const runtimePanel = new RuntimePanel(page);
    await runtimePanel.open();
    // Create runtime
    await runtimePanel.createRuntime();
    await page.waitForTimeout(2000);

    const workspaceAdminPage = new WorkspaceAdminPage(page);
    // navigate back to workspace admin page
    await navigation.navMenu(page, NavLink.WORKSPACE_ADMIN);
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.getWorkspaceNamespaceInput().type(workspaceNamespace);
    await workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();

    // verify the runtime status is running
    expect(await workspaceAdminPage.getRuntimeStatus()).toEqual('Running');
    const deleteRuntimeModal = await workspaceAdminPage.clickRuntimeDeleteButton();
    await deleteRuntimeModal.clickCancelButton();
    await workspaceAdminPage.waitForLoad();

    await workspaceAdminPage.clickRuntimeDeleteButton();
    // delete the runtime
    await deleteRuntimeModal.clickDeleteButton();
    await page.waitForTimeout(50000);
    await workspaceAdminPage.getDeleteStatus();

    //verify the runtime status is deleting
    expect(await workspaceAdminPage.getRuntimeStatus()).toEqual('Deleting');
  });
});
