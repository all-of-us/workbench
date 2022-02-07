import { LinkText, MenuOption, WorkspaceAccessLevel, Tabs } from 'app/text-labels';
import { findOrCreateWorkspace, findWorkspaceCard, openTab, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import { config } from 'resources/workbench-config';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeWorkspaceName } from 'utils/str-utils';

describe('Workspace Reader and Writer Permission Test', () => {
  const assignAccess = [
    {
      accessRole: WorkspaceAccessLevel.Writer,
      userEmail: config.WRITER_USER
    },
    {
      accessRole: WorkspaceAccessLevel.Reader,
      userEmail: config.READER_USER
    }
  ];

  const workspace = makeWorkspaceName();

  test('Share workspace to READER and WRITER', async () => {
    await signInWithAccessToken(page);
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    const aboutPage = new WorkspaceAboutPage(page);
    await openTab(page, Tabs.About, aboutPage);

    for (const assign of assignAccess) {
      await aboutPage.shareWorkspaceWithUser(assign.userEmail, assign.accessRole);
      await aboutPage.waitForLoad();
    }

    await reloadAboutPage();
    const collaborators = await aboutPage.findUsersInCollaboratorList();
    // Verify OWNER (login user) information.
    expect(collaborators.get(WorkspaceAccessLevel.Owner).some((item) => item.includes(process.env.USER_NAME))).toBe(
      true
    );
    // Verify WRITER or READER information.
    for (const assign of assignAccess) {
      expect(collaborators.get(assign.accessRole).some((item) => item.includes(assign.userEmail))).toBe(true);
    }
  });

  // Test depends on previous test: Will fail when workspace is not found and share didn't work.
  test.each(assignAccess)('Verify WRITER and READER cannot share, edit or delete workspace', async (assign) => {
    await signInWithAccessToken(page, assign.userEmail);

    // Find workspace created by previous test. If not found, test will fail.
    const workspaceCard = await findWorkspaceCard(page, workspace);
    const accessLevel = await workspaceCard.getWorkspaceAccessLevel();
    // Verify Snowman menu: Share, Edit and Delete actions are not available for click.
    expect(accessLevel).toBe(assign.accessRole);
    await workspaceCard.verifyWorkspaceCardMenuOptions(assign.accessRole);

    const aboutPage = await new WorkspacesPage(page).openAboutPage(workspaceCard);

    const collaborators = await aboutPage.findUsersInCollaboratorList();
    // Verify OWNER information in Collaborator list.
    expect(collaborators.get(WorkspaceAccessLevel.Owner).some((item) => item.includes(process.env.USER_NAME))).toBe(
      true
    );
    // Verify WRITER or READER information in Collaborator list.
    expect(collaborators.get(assign.accessRole).some((item) => item.includes(assign.userEmail))).toBe(true);

    // Verify Share modal: the Search input and Save button are disabled.
    const modal = await aboutPage.openShareModal();
    const searchInput = modal.waitForSearchBox();
    expect(await searchInput.isDisabled()).toBe(true);
    expect(await modal.getSaveButton().isCursorNotAllowed()).toBe(true);
    await modal.clickButton(LinkText.Cancel, { waitForClose: true });
    await aboutPage.waitForLoad();

    // Verify Workspace Actions menu: READER or WRITER cannot Share, Edit or Delete workspace.
    await verifyWorkspaceActionMenuOptions();
  });

  // Test depends on previous test: Will fail when workspace is not found and share didn't work.
  test('Stop share workspace', async () => {
    await signInWithAccessToken(page);

    // Find workspace created by previous test. If not found, test will fail.
    const workspaceCard = await findWorkspaceCard(page, workspace);
    const aboutPage = await new WorkspacesPage(page).openAboutPage(workspaceCard);

    // Stop share.
    for (const assign of assignAccess) {
      await aboutPage.removeCollaborator(assign.userEmail);
      await aboutPage.waitForLoad();
    }
    await reloadAboutPage();

    // Verify WRITER and READER are gone in Collaborator list.
    const collaborators = await aboutPage.findUsersInCollaboratorList();
    for (const assign of assignAccess) {
      expect(collaborators.has(assign.accessRole)).toBe(false);
    }
  });

  // Open Data page then back to About page in order to refresh Collaborators list in page.
  async function reloadAboutPage() {
    const dataPage = new WorkspaceDataPage(page);
    await openTab(page, Tabs.Data, dataPage);
    const aboutPage = new WorkspaceAboutPage(page);
    await openTab(page, Tabs.About, aboutPage);
  }
  async function verifyWorkspaceActionMenuOptions(): Promise<void> {
    const dataPage = new WorkspaceDataPage(page);
    const snowmanMenu = await dataPage.getWorkspaceActionMenu();
    const links = await snowmanMenu.getAllOptionTexts();
    expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));

    expect(await snowmanMenu.isOptionDisabled(MenuOption.Share)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
  }
});
