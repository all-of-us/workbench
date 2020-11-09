import WorkspacesPage from 'app/page/workspaces-page';
import {createWorkspace, findOrCreateWorkspace, signIn} from 'utils/test-utils';
import {Option} from 'app/text-labels';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import Navigation, {NavLink} from 'app/component/navigation';
import WorkspaceCard from 'app/component/workspace-card';
import {config} from 'resources/workbench-config';

describe('Duplicate workspace', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Select "Duplicate" thru the Ellipsis menu located inside the Workspace card.
   * - Enter a new workspace name and save the duplicate.
   * - Delete duplicate workspace.
   */
    test('OWNER can duplicate workspace via Workspace card', async () => {
      const workspaceCard = await findOrCreateWorkspace(page);

      await workspaceCard.asElementHandle().hover();
      // Click on Ellipsis menu "Duplicate" option.
      await workspaceCard.selectSnowmanMenu(Option.Duplicate);

      // Fill out Workspace Name should be just enough for successful duplication
      const workspacesPage = new WorkspacesPage(page);
      await (await workspacesPage.getWorkspaceNameTextbox()).clear();
      const duplicateWorkspaceName = await workspacesPage.fillOutWorkspaceName();

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
    });

    test('OWNER can duplicate workspace via Workspace action menu', async () => {
      const workspaceCard = await findOrCreateWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const dataPage = new WorkspaceDataPage(page);
      await dataPage.selectWorkspaceAction(Option.Duplicate);

      const workspacesPage = new WorkspacesPage(page);

      // Fill out Workspace Name
      await (await workspacesPage.getWorkspaceNameTextbox()).clear();
      const duplicateWorkspaceName = await workspacesPage.fillOutWorkspaceName();
      // select "Share workspace with same set of collaborators radiobutton
      await workspacesPage.clickShareWithCollaboratorsCheckbox();

      const finishButton = await workspacesPage.getDuplicateWorkspaceButton();
      await finishButton.waitUntilEnabled();
      await workspacesPage.clickCreateFinishButton(finishButton);

      // Duplicate workspace Data page is loaded.
      await dataPage.waitForLoad();
      expect(page.url()).toContain(duplicateWorkspaceName.replace(/-/g, '')); // Remove dash from workspace name

      // Delete duplicate workspace via Workspace action dropdown menu.
      await dataPage.deleteWorkspace();
    });

  test('OWNER can duplicate workspace to an older CDR Version via Workspace card', async () => {
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

    expect(await workspacesPage.getCdrVersionWarningMessage())
        .toContain('You’ve selected a version that isn’t the most recent.');

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
    expect(upgradeMessage).toContain(`You're duplicating the workspace "${originalWorkspaceName}" to upgrade from`);
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
