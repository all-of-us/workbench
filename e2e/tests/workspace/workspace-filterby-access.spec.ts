import HomePage from 'app/page/home-page';
import WorkspacesPage from 'app/page/workspaces-page';
import {signIn} from 'utils/test-utils';
import {WorkspaceAccessLevel} from 'app/text-labels';
import WorkspaceCard from 'app/component/workspace-card';

describe('Filter workspaces in Workspaces page', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Choose access level in Filter by menu', async () => {
    await new HomePage(page).getSeeAllWorkspacesLink().then(link => link.click());
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.waitForLoad();

    const levels = [
      WorkspaceAccessLevel.Owner,
      WorkspaceAccessLevel.Writer,
      WorkspaceAccessLevel.Reader];

    for (const level of levels) {
      await workspacesPage.filterByAccessLevel(level);
      const cards = await WorkspaceCard.findAllCards(page);
      for (const card of cards) {
        const cardLevel = await card.getWorkspaceAccessLevel();
        expect(cardLevel).toBe(level);
      }
    }

  });


});
