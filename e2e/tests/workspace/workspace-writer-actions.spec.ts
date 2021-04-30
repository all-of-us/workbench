import WorkspacesPage from 'app/page/workspaces-page';
import { signIn } from 'utils/test-utils';
import WorkspaceCard from 'app/component/workspace-card';
import { config } from 'resources/workbench-config';
import { LinkText, WorkspaceAccessLevel } from 'app/text-labels';
import HomePage from 'app/page/home-page';
import * as fp from 'lodash/fp';

describe('Workspace WRITER actions tests', () => {
  beforeEach(async () => {
    await signIn(page, config.writerUserName, config.userPassword);
  });

  // Tests don't require creating new workspace
  test('WRITER cannot share, edit or delete workspace', async () => {
    const homePage = new HomePage(page);
    await homePage.getSeeAllWorkspacesLink().click();

    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.waitForLoad();

    // Verify Workspace Access Level is WRITER.
    const workspaceCards = await WorkspaceCard.findAllCards(page, WorkspaceAccessLevel.Writer);
    if (workspaceCards.length === 0) {
      return; // end test because no Workspace card available for checking
    }

    const workspaceCard = fp.shuffle(workspaceCards)[0];
    const accessLevel = await workspaceCard.getWorkspaceAccessLevel();
    // Verify: Share, Edit and Delete actions are not available for click.
    expect(accessLevel).toBe(WorkspaceAccessLevel.Writer);

    await workspaceCard.verifyWorkspaceCardMenuOptions(WorkspaceAccessLevel.Writer);

    const aboutPage = await workspacesPage.openAboutPage(workspaceCard);
    const modal = await aboutPage.openShareModal();
    const searchInput = modal.waitForSearchBox();
    // Verify: the Search input in Share modal is disabled.
    expect(await searchInput.isDisabled()).toBe(true);
    await modal.clickButton(LinkText.Cancel);
  });

  /*
      // TODO add new test
      test('WRITER cannot edit or delete workspace notebook, dataset or conceptset', async () => {
        // ADD HERE
      });
      */
});
