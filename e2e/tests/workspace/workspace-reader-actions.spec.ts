import WorkspacesPage from 'app/page/workspaces-page';
import { signIn } from 'utils/test-utils';
import WorkspaceCard from 'app/component/workspace-card';
import { config } from 'resources/workbench-config';
import { LinkText, WorkspaceAccessLevel } from 'app/text-labels';
import HomePage from 'app/page/home-page';
import * as fp from 'lodash/fp';

describe('Workspace READER actions menu', () => {
  beforeEach(async () => {
    await signIn(page, config.readerUserName, config.userPassword);
  });

  // Tests don't require creating new workspace
  test('READER cannot share, edit or delete workspace', async () => {
    const homePage = new HomePage(page);
    await homePage.getSeeAllWorkspacesLink().click();

    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.waitForLoad();

    // Verify Workspace Access Level is READER.
    const workspaceCards = await WorkspaceCard.findAllCards(page, WorkspaceAccessLevel.Reader);
    if (workspaceCards.length === 0) {
      return; // end test because no Workspace card available for checking
    }

    const workspaceCard = fp.shuffle(workspaceCards)[0];
    const accessLevel = await workspaceCard.getWorkspaceAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Reader);

    // Verify: Share, Edit and Delete actions are not available for click.
    await workspaceCard.verifyWorkspaceCardMenuOptions(WorkspaceAccessLevel.Reader);

    const aboutPage = await workspacesPage.openAboutPage(workspaceCard);
    await aboutPage.waitForLoad();

    const accessLevel1 = await aboutPage.findUserInCollaboratorList(config.readerUserName);
    expect(accessLevel1).toBe(WorkspaceAccessLevel.Reader);

    // Verify: the Search input in Share modal is disabled.
    const modal = await aboutPage.openShareModal();
    const searchInput = modal.waitForSearchBox();
    expect(await searchInput.isDisabled()).toBe(true);
    await modal.clickButton(LinkText.Cancel);
    await aboutPage.waitForLoad();
  });

  /*
      // TODO add new test
      test('READER cannot edit or delete workspace notebook, dataset or conceptset', async () => {
        // ADD HERE
      });
       */
});
