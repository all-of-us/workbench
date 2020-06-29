import Link from 'app/element/link';
import {findWorkspace, signIn, waitWhileLoading} from 'utils/test-utils';
import {WorkspaceAccessLevel} from 'app/text-labels';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import {config} from 'resources/workbench-config';

describe('Share workspace', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  // Assume there is at least one workspace preexist
  describe('From the workspace about page', () => {
    test('As OWNER, user can share a workspace', async () => {
      // Using the share modal requires a viewport be set, due to react-select weirdness.
      await page.setViewport({height: 1280, width: 1280});

      const workspaceCard = await findWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const notebooksLink = await Link.findByName(page, {name: 'About'});
      await notebooksLink.clickAndWait();

      const aboutPage = new WorkspaceAboutPage(page);
      await aboutPage.waitForLoad();

      // This test is not hermetic - if the collaborator is already on this
      // workspace, just remove them before continuing.
      let accessLevel = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
      if (accessLevel !== null) {
        await (await aboutPage.openShareModal()).removeUser(config.collaboratorUsername);
        await waitWhileLoading(page);
      }

      let shareModal = await aboutPage.openShareModal();
      await shareModal.shareWithUser(
        config.collaboratorUsername, WorkspaceAccessLevel.Owner);
      // Collab list is refreshed.
      await waitWhileLoading(page);

      accessLevel = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
      expect(accessLevel).toBe(WorkspaceAccessLevel.Owner);

      shareModal = await aboutPage.openShareModal();
      await shareModal.removeUser(config.collaboratorUsername);
      await waitWhileLoading(page);

      accessLevel = await aboutPage.findUserInCollaboratorList(config.collaboratorUsername);
      expect(accessLevel).toBeNull();
    });
  });
});
