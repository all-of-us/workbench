import WorkspaceCard from 'app/component/workspace-card';
import {findOrCreateWorkspace, signIn} from 'utils/test-utils';

describe('Home page ui tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Check visibility of Workspace cards', async () => {
   await WorkspaceCard.getAllCardDetails(page);
    const workspaceCard = await findOrCreateWorkspace(page);
    console.log(`oldest workspace card found is: ${await workspaceCard.getWorkspaceName()}`);
  });

});
