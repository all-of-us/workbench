import {createWorkspace, signInWithAccessToken} from 'utils/test-utils';
import WorkspaceCard from 'app/component/workspace-card';
import {config} from 'resources/workbench-config';
import {Option} from 'app/text-labels';
import OldCdrVersionModal from 'app/page/old-cdr-version-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import Navigation, {NavLink} from 'app/component/navigation';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import WorkspacesPage from 'app/page/workspaces-page';

describe('Duplicate workspace, changing CDR versions', () => {
    beforeEach(async () => {
        await signInWithAccessToken(page);
    });

    test('OWNER can duplicate workspace to an older CDR Version after consenting to restrictions', async () => {
        const workspaceCard: WorkspaceCard = await createWorkspace(page, config.defaultCdrVersionName);
        const originalWorkspaceName = await workspaceCard.getWorkspaceName();

        await workspaceCard.asElementHandle().hover();
        // Click on Ellipsis menu "Duplicate" option.
        await workspaceCard.selectSnowmanMenu(Option.Duplicate, {waitForNav: true});

        // Fill out Workspace Name should be just enough for successful duplication
        const workspaceEditPage = new WorkspaceEditPage(page);
        await (await workspaceEditPage.getWorkspaceNameTextbox()).clear();
        const duplicateWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();

        // change CDR Version
        await workspaceEditPage.selectCdrVersion(config.altCdrVersionName);

        // wait for the warning modal and consent to the required restrictions
        const modal = new OldCdrVersionModal(page);
        await modal.consentToOldCdrRestrictions();

        const finishButton = await workspaceEditPage.getDuplicateWorkspaceButton();
        await finishButton.waitUntilEnabled();
        await workspaceEditPage.clickCreateFinishButton(finishButton);

        // Duplicate workspace Data page is loaded.
        const dataPage = new WorkspaceDataPage(page);
        await dataPage.waitForLoad();
        expect(page.url()).toContain(duplicateWorkspaceName.replace(/-/g, '')); // Remove dash from workspace name

        // Delete duplicate workspace via Workspace card in Your Workspaces page.
        await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
        const workspacesPage = new WorkspacesPage(page);
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
        await workspaceCard.selectSnowmanMenu(Option.Duplicate, {waitForNav: true});

        // Fill out Workspace Name should be just enough for successful duplication
        const workspaceEditPage = new WorkspaceEditPage(page);
        await (await workspaceEditPage.getWorkspaceNameTextbox()).clear();
        const duplicateWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();

        // change CDR Version
        await workspaceEditPage.selectCdrVersion(config.defaultCdrVersionName);

        const upgradeMessage = await workspaceEditPage.getCdrVersionUpgradeMessage();
        expect(upgradeMessage).toContain(originalWorkspaceName);
        expect(upgradeMessage).toContain(`${config.altCdrVersionName} to ${config.defaultCdrVersionName}.`);

        const finishButton = await workspaceEditPage.getDuplicateWorkspaceButton();
        await finishButton.waitUntilEnabled();
        await workspaceEditPage.clickCreateFinishButton(finishButton);

        // Duplicate workspace Data page is loaded.
        const dataPage = new WorkspaceDataPage(page);
        await dataPage.waitForLoad();
        expect(page.url()).toContain(duplicateWorkspaceName.replace(/-/g, '')); // Remove dash from workspace name

        // Delete duplicate workspace via Workspace card in Your Workspaces page.
        await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
        const workspacesPage = new WorkspacesPage(page);
        await workspacesPage.waitForLoad();

        await WorkspaceCard.deleteWorkspace(page, duplicateWorkspaceName);

        // Verify Delete action was successful.
        expect(await WorkspaceCard.findCard(page, duplicateWorkspaceName)).toBeFalsy();

        // Delete original workspace via Workspace card
        await WorkspaceCard.deleteWorkspace(page, originalWorkspaceName);
        expect(await WorkspaceCard.findCard(page, originalWorkspaceName)).toBeFalsy();
    });
});
