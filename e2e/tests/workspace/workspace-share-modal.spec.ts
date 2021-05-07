import { LinkText, WorkspaceAccessLevel } from 'app/text-labels';
import { findOrCreateWorkspace, findWorkspaceCard, signIn, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import { makeWorkspaceName } from 'utils/str-utils';
import { config } from 'resources/workbench-config';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';

describe('Workspace Share Modal', () => {
  const assignAccess = [
    { accessRole: WorkspaceAccessLevel.Writer, userEmail: config.writerUserName },
    { accessRole: WorkspaceAccessLevel.Reader, userEmail: config.readerUserName }
  ];

  // Create new workspace with default CDR version
  const workspace = makeWorkspaceName();

  test.each(assignAccess)('Share %s', async (assign) => {
    await signInWithAccessToken(page);
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    const shareWorkspaceModal = await dataPage.shareWorkspace();
    await shareWorkspaceModal.shareWithUser(assign.userEmail, assign.accessRole);

    await dataPage.openAboutPage();
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();

    const collaborators = await aboutPage.findUsersInCollaboratorList();

    // Verify OWNER (login user) information.
    expect(collaborators.get(WorkspaceAccessLevel.Owner)).toEqual(process.env.USER_NAME);

    // Verify WRITER or READER information.
    expect(collaborators.get(assign.accessRole)).toEqual(assign.userEmail);
  });

  test.each(assignAccess)('WRITER and READER cannot share, edit or delete workspace', async (assign) => {
    // Log in could fail if encounters Google Captcha
    await signIn(page, assign.userEmail, config.userPassword);

    // Find workspace created by previous test. If not found, test will fail.
    const workspaceCard = await findWorkspaceCard(page, workspace);

    const accessLevel = await workspaceCard.getWorkspaceAccessLevel();

    // Verify: Share, Edit and Delete actions are not available for click.
    expect(accessLevel).toBe(assign.accessRole);
    await workspaceCard.verifyWorkspaceCardMenuOptions(assign.accessRole);

    const aboutPage = await new WorkspacesPage(page).openAboutPage(workspaceCard);

    const collaborators = await aboutPage.findUsersInCollaboratorList();

    // Verify OWNER information in Collaborator list.
    expect(collaborators.get(WorkspaceAccessLevel.Owner)).toEqual(process.env.USER_NAME);

    // Verify WRITER or READER information in Collaborator list.
    expect(collaborators.get(assign.accessRole)).toEqual(assign.userEmail);

    switch (assign.accessRole) {
      case WorkspaceAccessLevel.Reader:
      case WorkspaceAccessLevel.Writer:
        {
          // Verify: the Search input in Share modal is disabled.
          const modal = await aboutPage.openShareModal();
          const searchInput = modal.waitForSearchBox();
          expect(await searchInput.isDisabled()).toBe(true);
          await modal.clickButton(LinkText.Cancel);
          await aboutPage.waitForLoad();
        }
        break;
      default:
        break;
    }
  });
});
