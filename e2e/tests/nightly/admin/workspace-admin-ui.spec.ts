import { signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import navigation, { NavLink } from 'app/component/navigation';
import WorkspaceAdminPage from 'app/page/admin-workspace-page';
import { WorkspaceHeadings } from 'app/text-labels';

describe('Workspace Admin', () => {
  const workspaceNamespace = 'aou-rw-test-8c5cdbaf';

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
    const noActiveRuntimeText = 'No active runtimes exist for this workspace';
    expect(await workspaceAdminPage.getWorkspaceHeader()).toEqual(noActiveRuntimeText);
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

  });

  test('Verify that admin is able to delete runtime', async () => {
    
    const workspaceAdminPage = new WorkspaceAdminPage(page);
    await workspaceAdminPage.waitForLoad();
    await workspaceAdminPage.getWorkspaceNamespaceInput().type(workspaceNamespace);
    workspaceAdminPage.clickLoadWorkspaceButton();
    await workspaceAdminPage.waitForLoad();

  });
});
