import {createWorkspace, signIn} from 'utils/test-utils';
import WorkspaceCard from 'app/component/workspace-card';
import {config} from 'resources/workbench-config';
import {Option} from 'app/text-labels';
import WorkspacesPage from 'app/page/workspaces-page';
import OldCdrVersionModal from 'app/page/old-cdr-version-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import Navigation, {NavLink} from 'app/component/navigation';

describe('Duplicate workspace, changing CDR versions', () => {
    beforeEach(async () => {
        await signIn(page);
    });

    test('OWNER can duplicate workspace to an older CDR Version after consenting to restrictions', async () => {
        const workspaceCard: WorkspaceCard = await createWorkspace(page, config.defaultCdrVersionName);
        const originalWorkspaceName = await workspaceCard.getWorkspaceName();

        await workspaceCard.asElementHandle().hover();
        // Click on Ellipsis menu "Duplicate" option.
        await workspaceCard.selectSnowmanMenu(Option.Duplicate);

        // Fill out Workspace Name should be just enough for successful duplication
        const workspacesPage = new WorkspacesPage(page);
        await (await workspacesPage.getWorkspaceNameTextbox()).clear();
        const duplicateWorkspaceName = await workspacesPage.fillOutWorkspaceName();

        // change CDR Version
        await workspacesPage.selectCdrVersion(config.altCdrVersionName);

        // wait for the warning modal and consent to the required restrictions
        const modal = new OldCdrVersionModal(page);
        await modal.consentToOldCdrRestrictions();

        const finishButton = await workspacesPage.getDuplicateWorkspaceButton();
        await finishButton.waitUntilEnabled();
        await workspacesPage.clickCreateFinishButton(finishButton);

        // Duplicate workspace Data page is loaded.
        const dataPage = new WorkspaceDataPage(page);
        await dataPage.waitForLoad();
        expect(page.url()).toContain(duplicateWorkspaceName.replace(/-/g, '')); // Remove dash from workspace name

        // Delete duplicate workspace via Workspace card in Your Workspaces page.
        await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
        await workspacesPage.waitForLoad();

        await WorkspaceCard.deleteWorkspace(page, duplicateWorkspaceName);

        // Verify Delete action was successful.
        expect(await WorkspaceCard.findCard(page, duplicateWorkspaceName)).toBeFalsy();

        // Delete original workspace via Workspace card
        await WorkspaceCard.deleteWorkspace(page, originalWorkspaceName);
        expect(await WorkspaceCard.findCard(page, originalWorkspaceName)).toBeFalsy();
    });

    test('OWNER can duplicate workspace to a newer CDR Version via Workspace card', async () => {
        const workspaceCard: WorkspaceCard = await createWorkspace(page, config.altCdrVersionName);
        const originalWorkspaceName = await workspaceCard.getWorkspaceName();

        await workspaceCard.asElementHandle().hover();
        // Click on Ellipsis menu "Duplicate" option.
        await workspaceCard.selectSnowmanMenu(Option.Duplicate);

        // Fill out Workspace Name should be just enough for successful duplication
        const workspacesPage = new WorkspacesPage(page);
        await (await workspacesPage.getWorkspaceNameTextbox()).clear();
        const duplicateWorkspaceName = await workspacesPage.fillOutWorkspaceName();

        // change CDR Version
        await workspacesPage.selectCdrVersion(config.defaultCdrVersionName);

        const upgradeMessage = await workspacesPage.getCdrVersionUpgradeMessage();
        expect(upgradeMessage).toContain(originalWorkspaceName);
        expect(upgradeMessage).toContain(`${config.altCdrVersionName} to ${config.defaultCdrVersionName}.`);

        const finishButton = await workspacesPage.getDuplicateWorkspaceButton();
        await finishButton.waitUntilEnabled();
        await workspacesPage.clickCreateFinishButton(finishButton);

        // Duplicate workspace Data page is loaded.
        const dataPage = new WorkspaceDataPage(page);
        await dataPage.waitForLoad();
        expect(page.url()).toContain(duplicateWorkspaceName.replace(/-/g, '')); // Remove dash from workspace name

        // Delete duplicate workspace via Workspace card in Your Workspaces page.
        await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
        await workspacesPage.waitForLoad();

        await WorkspaceCard.deleteWorkspace(page, duplicateWorkspaceName);

        // Verify Delete action was successful.
        expect(await WorkspaceCard.findCard(page, duplicateWorkspaceName)).toBeFalsy();

        // Delete original workspace via Workspace card
        await WorkspaceCard.deleteWorkspace(page, originalWorkspaceName);
        expect(await WorkspaceCard.findCard(page, originalWorkspaceName)).toBeFalsy();
    });
});
