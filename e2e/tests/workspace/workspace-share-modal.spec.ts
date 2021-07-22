import { LinkText, MenuOption, WorkspaceAccessLevel } from 'app/text-labels';
import { findOrCreateWorkspace, findWorkspaceCard } from 'utils/test-utils';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import { config } from 'resources/workbench-config';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeWorkspaceName } from 'utils/str-utils';
import { withSignInTest } from 'libs/page-manager';

describe('Workspace Share Modal', () => {
  const assignAccess = [
    {
      accessRole: WorkspaceAccessLevel.Writer,
      userEmail: config.WRITER_USER,
      userAccessTokenFilename: config.WRITER_ACCESS_TOKEN_FILE
    },
    {
      accessRole: WorkspaceAccessLevel.Reader,
      userEmail: config.READER_USER,
      userAccessTokenFilename: config.READER_ACCESS_TOKEN_FILE
    }
  ];

  // Create new workspace with default CDR version
  const workspace = makeWorkspaceName();

  test.each(assignAccess)('Share workspace %s', async (assign) => {
    await withSignInTest()(async (page) => {
      await findOrCreateWorkspace(page, { workspaceName: workspace });

      const dataPage = new WorkspaceDataPage(page);
      await dataPage.waitForLoad();

      await dataPage.openAboutPage();
      const aboutPage = new WorkspaceAboutPage(page);
      await aboutPage.waitForLoad();

      let collaborators = await aboutPage.findUsersInCollaboratorList();

      // Verify No WRITER or READER exists.
      expect(collaborators.has(assign.accessRole)).toBeFalsy();

      const shareWorkspaceModal = await aboutPage.shareWorkspace();
      await shareWorkspaceModal.shareWithUser(assign.userEmail, assign.accessRole);
      await aboutPage.waitForLoad();

      await reloadAboutPage();
      collaborators = await aboutPage.findUsersInCollaboratorList();
      // Verify OWNER (login user) information.
      expect(collaborators.get(WorkspaceAccessLevel.Owner).some((item) => item.includes(process.env.USER_NAME))).toBe(
        true
      );
      // Verify WRITER or READER information.
      expect(collaborators.get(assign.accessRole).some((item) => item.includes(assign.userEmail))).toBe(true);
    });
  });

  // Test depends on previous test: Will fail when workspace is not found and share didn't work.
  test.each(assignAccess)('Verify WRITER and READER cannot share, edit or delete workspace', async (assign) => {
    await withSignInTest(assign.userAccessTokenFilename)(async (page) => {
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
  });

  // Test depends on previous test: Will fail when workspace is not found and share didn't work.
  test.each(assignAccess)('Stop share workspace %s', async (assign) => {
    await withSignInTest()(async (page) => {
      // Find workspace created by previous test. If not found, test will fail.
      const workspaceCard = await findWorkspaceCard(page, workspace);
      const aboutPage = await new WorkspacesPage(page).openAboutPage(workspaceCard);

      // Verify WRITER or READER exists in Collaborator list.
      let collaborators = await aboutPage.findUsersInCollaboratorList();
      expect(collaborators.get(assign.accessRole).some((item) => item.includes(assign.userEmail))).toBe(true);
      collaborators.clear();

      await aboutPage.removeCollaborator(assign.userEmail);
      await aboutPage.waitForLoad();

      await reloadAboutPage();

      // Verify WRITER or READER is gone in Collaborator list.
      collaborators = await aboutPage.findUsersInCollaboratorList();
      expect(collaborators.has(assign.accessRole)).toBe(false);
    });
  });

  // Open Data page then back to About page in order to refresh Collaborators list in page.
  async function reloadAboutPage() {
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();
    await aboutPage.openDataPage({ waitPageChange: true });
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();
    await dataPage.openAboutPage({ waitPageChange: true });
    await aboutPage.waitForLoad();
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
