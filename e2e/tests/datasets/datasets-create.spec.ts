import * as fp from 'lodash/fp';
import {signIn} from 'utils/app-utils';
import WorkspaceCard from '../../app/component/workspace-card';
import {WorkspaceAccessLevel} from '../../app/page-identifiers';
import WorkspaceDataPage from '../../app/page/workspace-data-page';
import WorkspacesPage from '../../app/page/workspaces-page';


describe('Workspace DataSets tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('User can create a dataset in a workspace', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    // choose one existing workspace on "Your Workspaces" page
    const workspaceCard = new WorkspaceCard(page);
    const retrievedWorkspaces = await workspaceCard.getWorkspaceMatchAccessLevel(WorkspaceAccessLevel.OWNER);
    const oneWorkspaceCard: WorkspaceCard = fp.shuffle(retrievedWorkspaces)[0];
    await oneWorkspaceCard.clickWorkspaceName();

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();



  });

});
