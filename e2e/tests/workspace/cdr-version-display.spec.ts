import WorkspaceCard from 'app/component/workspace-card';
import {createWorkspace, signIn} from 'utils/test-utils';
import {config} from 'resources/workbench-config';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspaceBase from 'app/page/workspace-base';

describe('Workspace CDR Version display', () => {

    beforeEach(async () => {
        await signIn(page);
    });

    test('The CDR version upgrade flag appears in the workspace navigation bar for an old CDR', async () => {
        const workspaceCard: WorkspaceCard = await createWorkspace(page, config.altCdrVersionName);
        await workspaceCard.clickWorkspaceName();

        const workspacePage: WorkspaceBase = new WorkspaceDataPage(page);
        expect(await workspacePage.getCdrVersion()).toBe(config.altCdrVersionName)
        expect(await workspacePage.getNewCdrVersionFlag()).toBeTruthy();

        // cleanup
        await workspacePage.deleteWorkspace()
    });

});
