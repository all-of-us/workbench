import WorkspaceDataPage from 'app/page/workspace-data-page';
import { WorkspaceAccessLevel } from 'app/text-labels';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import { makeWorkspaceName } from 'utils/str-utils';
import { config } from 'resources/workbench-config';
import { waitWhileLoading } from 'utils/waits-utils';
import { logger } from 'libs/logger';

describe('Workspace Share Modal', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Create new workspace with default CDR version
  const workspace = makeWorkspaceName();

  test('Can share to another owner', async () => {
    logger.info('Running test: Can share to another owner');

    await findOrCreateWorkspace(page, { workspaceName: workspace });
    const ownerName = config.collaboratorUsername;
    await removeCollaborator(ownerName);

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.openDataPage();
    await dataPage.waitForLoad();
    await dataPage.verifyWorkspaceNameOnDataPage(workspace);

    const shareWorkspaceModal = await dataPage.shareWorkspace();
    await shareWorkspaceModal.shareWithUser(ownerName, WorkspaceAccessLevel.Owner);

    const aboutPage = new WorkspaceAboutPage(page);
    await waitWhileLoading(page);

    const accessLevel = await aboutPage.findUserInCollaboratorList(ownerName);
    expect(accessLevel).toBe(WorkspaceAccessLevel.Owner);
  });

  test('Can share to reader', async () => {
    logger.info('Running test name: Can share to reader');

    await findOrCreateWorkspace(page, { workspaceName: workspace });
    const readerName = config.readerUserName;
    await removeCollaborator(readerName);

    const aboutPage = new WorkspaceAboutPage(page);
    const shareModal = await aboutPage.shareWorkspace();
    await shareModal.shareWithUser(readerName, WorkspaceAccessLevel.Reader);
    await waitWhileLoading(page);

    const accessLevel = await aboutPage.findUserInCollaboratorList(readerName);
    expect(accessLevel).toBe(WorkspaceAccessLevel.Reader);
  });

  test('Can share to writer', async () => {
    logger.info('Running test name: Can share to writer');

    await findOrCreateWorkspace(page, { workspaceName: workspace });
    const writerName = config.writerUserName;
    await removeCollaborator(writerName);

    const aboutPage = new WorkspaceAboutPage(page);
    const shareModal = await aboutPage.shareWorkspace();
    await shareModal.shareWithUser(writerName, WorkspaceAccessLevel.Writer);
    await waitWhileLoading(page);

    const accessLevel = await aboutPage.findUserInCollaboratorList(writerName);
    expect(accessLevel).toBe(WorkspaceAccessLevel.Writer);
  });

  // Remove existing collaborator in Workspace About page.
  async function removeCollaborator(name: string) {
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();
    await dataPage.openAboutPage();

    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();

    let accessLevel = await aboutPage.findUserInCollaboratorList(name);
    if (accessLevel !== null) {
      await aboutPage.removeCollaborator(name);
      await aboutPage.waitForLoad();
      accessLevel = await aboutPage.findUserInCollaboratorList(name);
    }
    expect(accessLevel).toBeNull();
  }
});
