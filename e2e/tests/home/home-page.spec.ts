import WorkspaceCard from 'app/component/workspace-card';
import {signIn} from 'utils/test-utils';

describe('Home page ui tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Check visibility of Workspace cards', async () => {
    const allCards = await WorkspaceCard.findAllCards(page);
    for (const card of allCards) {
      await card.getDateTime();
    }
  });

});
