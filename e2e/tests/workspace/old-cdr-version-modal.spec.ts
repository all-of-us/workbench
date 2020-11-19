import {createWorkspace, signIn} from 'utils/test-utils';
import WorkspaceCard from 'app/component/workspace-card';
import {config} from 'resources/workbench-config';
import {Option} from 'app/text-labels';
import WorkspacesPage from 'app/page/workspaces-page';
import {makeWorkspaceName} from 'utils/str-utils';
import OldCdrVersionModal from 'app/page/old-cdr-version-modal';

describe('OldCdrVersion Modal restrictions', () => {
    beforeEach(async () => {
        await signIn(page);
    });

    test('User cannot create a workspace with an old CDR Version without consenting to the restrictions', async () => {
        const workspacesPage = new WorkspacesPage(page);
        await workspacesPage.load();

        const editPage = await workspacesPage.fillOutRequiredCreationFields(makeWorkspaceName());

        // select an old CDR Version
        await editPage.selectCdrVersion(config.altCdrVersionName);

        const createButton = await editPage.getCreateWorkspaceButton();
        expect(await createButton.isCursorNotAllowed()).toBe(true);

        // fill out the modal checkboxes
        const modal = new OldCdrVersionModal(page);
        await modal.consentToOldCdrRestrictions();

        // now we can continue
        await createButton.waitUntilEnabled();
        await workspacesPage.clickCreateFinishButton(createButton);
    });

    test('OWNER cannot duplicate workspace to an older CDR Version without consenting to restrictions', async () => {
        const workspaceCard: WorkspaceCard = await createWorkspace(page, config.defaultCdrVersionName);
        await workspaceCard.getWorkspaceName();

        await workspaceCard.asElementHandle().hover();
        // Click on Ellipsis menu "Duplicate" option.
        await workspaceCard.selectSnowmanMenu(Option.Duplicate);

        // Fill out Workspace Name should be just enough for successful duplication
        const workspacesPage = new WorkspacesPage(page);
        await (await workspacesPage.getWorkspaceNameTextbox()).clear();
        await workspacesPage.fillOutWorkspaceName();

        // change CDR Version
        await workspacesPage.selectCdrVersion(config.altCdrVersionName);

        const finishButton = await workspacesPage.getDuplicateWorkspaceButton();
        expect(await finishButton.isCursorNotAllowed()).toBe(true);
    });
});