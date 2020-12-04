import WorkspaceCard from 'app/component/workspace-card';
import {createWorkspace, signIn} from 'utils/test-utils';
import {config} from 'resources/workbench-config';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspaceBase from 'app/page/workspace-base';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import Navigation, {NavLink} from 'app/component/navigation';
import CdrVersionUpgradeModal from 'app/page/cdr-version-upgrade-modal';

describe('Workspace CDR Version upgrade', () => {
    beforeEach(async () => {
        await signIn(page);
    });

   test('Clicking the CDR version upgrade flag pops up the upgrade modal', async () => {
        const workspaceCard: WorkspaceCard = await createWorkspace(page, config.altCdrVersionName);
        await workspaceCard.clickWorkspaceName();

        const workspacePage: WorkspaceBase = new WorkspaceDataPage(page);
        expect(await workspacePage.getCdrVersion()).toBe(config.altCdrVersionName);
        const newVersionFlag = await workspacePage.getNewCdrVersionFlag();
        await newVersionFlag.click();

        const modal = new CdrVersionUpgradeModal(page);
        expect(await modal.isLoaded()).toBe(true);

        // hit cancel
        const cancelButton = await modal.getCancelButton();
        await cancelButton.click();

        // new version flag remains
        await workspacePage.getNewCdrVersionFlag();

        // cleanup
        await workspacePage.deleteWorkspace()
    });

    test('Clicking the CDR version upgrade button opens the Duplicate Workspace Page', async () => {
        const workspaceCard: WorkspaceCard = await createWorkspace(page, config.altCdrVersionName);
        const workspaceName = await workspaceCard.clickWorkspaceName();

        const workspacePage: WorkspaceBase = new WorkspaceDataPage(page);

        const newVersionFlag = await workspacePage.getNewCdrVersionFlag();
        await newVersionFlag.click();

        const modal = new CdrVersionUpgradeModal(page);
        expect(await modal.isLoaded()).toBe(true);

        const upgradeButton = await modal.getUpgradeButton();
        await upgradeButton.click();

        const duplicationPage = new WorkspaceEditPage(page);
        const upgradeMessage = await duplicationPage.getCdrVersionUpgradeMessage();
        expect(upgradeMessage).toContain(workspaceName);
        expect(upgradeMessage).toContain(`${config.altCdrVersionName} to ${config.defaultCdrVersionName}.`);

        // cleanup
        const cancelButton = await duplicationPage.getCancelButton();
        await cancelButton.clickAndWait();

        await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
        const workspacesPage = new WorkspacesPage(page);
        await workspacesPage.waitForLoad();

        await WorkspaceCard.deleteWorkspace(page, workspaceName);
    });
});
