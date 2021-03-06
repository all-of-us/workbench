import WorkspacesPage from 'app/page/workspaces-page';
import {signInWithAccessToken} from 'utils/test-utils';

describe('Sidebar Navigation', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('SideNav menu', async () => {

    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.waitForLoad();
    expect(await workspacesPage.isLoaded()).toBe(true);

  });

});
