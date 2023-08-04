import { findOrCreateWorkspace, findOrCreateWorkspaceCard, signInWithAccessToken } from 'utils/test-utils';
import { MenuOption } from 'app/text-labels';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import Navigation, { NavLink } from 'app/component/navigation';
import WorkspaceCard from 'app/component/card/workspace-card';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import WorkspacesPage from 'app/page/workspaces-page';
import { config } from 'resources/workbench-config';
import OldCdrVersionModal from 'app/modal/old-cdr-version-modal';

describe.skip('Duplicate workspace', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eCloneWorkspaceTest';

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Select "Duplicate" thru the Ellipsis menu located inside the Workspace card.
   * - Enter a new workspace name and save the duplicate.
   * - Delete duplicate workspace.
   */
  test('Duplicate workspace', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    // Access Workspace Duplicate page via Workspace action menu.
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.selectWorkspaceAction(MenuOption.Duplicate);

    // Fill out Workspace Name should be just enough for successful duplication
    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.getWorkspaceNameTextbox().clear();
    const duplicateWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();

    // observe that we cannot change the Data Access Tier.
    const accessTierSelect = workspaceEditPage.getDataAccessTierSelect();
    expect(await accessTierSelect.isDisabled()).toEqual(true);

    const finishButton = workspaceEditPage.getDuplicateWorkspaceButton();
    await workspaceEditPage.requestForReviewRadiobutton(false);
    await finishButton.waitUntilEnabled();
    await workspaceEditPage.clickCreateFinishButton(finishButton);

    // Duplicate workspace Data page is loaded.
    await dataPage.waitForLoad();
    expect(page.url()).toContain(duplicateWorkspaceName.replace(/-/g, '')); // Remove dash from workspace name

    // Delete duplicate workspace via Workspace card in Your Workspaces page.
    await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.waitForLoad();

    await new WorkspaceCard(page).delete({ name: duplicateWorkspaceName });
  });

  test('Cannot duplicate workspace with older CDR version without consent to restrictions', async () => {
    const workspaceCard = await findOrCreateWorkspaceCard(page, { workspaceName });
    await (await workspaceCard.asElement()).hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    const workspaceEditPage = new WorkspaceEditPage(page);

    // Fill out the fields required for duplication and observe that duplication is enabled
    const duplicateWorkspaceName = await workspaceEditPage.fillOutRequiredDuplicationFields();
    const duplicateButton = workspaceEditPage.getDuplicateWorkspaceButton();
    await duplicateButton.waitUntilEnabled();

    // Change CDR version to an old CDR version.
    await workspaceEditPage.selectCdrVersion(config.OLD_CDR_VERSION_NAME);
    expect(await duplicateButton.isCursorNotAllowed()).toBe(true);

    const modal = new OldCdrVersionModal(page);
    const cancelButton = modal.getCancelButton();
    await cancelButton.click();

    // The CDR version is forcibly reverted back to the default
    const cdrVersionSelect = workspaceEditPage.getCdrVersionSelect();
    expect(await cdrVersionSelect.getSelectedValue()).toBe(config.DEFAULT_CDR_VERSION_NAME);

    // Try again. This time consent to restriction.
    // Duplicate workspace with an older CDR Version can proceed after consenting to restrictions.
    await workspaceEditPage.selectCdrVersion(config.OLD_CDR_VERSION_NAME);
    await modal.consentToOldCdrRestrictions();

    // Finish creation of workspace.
    await workspaceEditPage.requestForReviewRadiobutton(false);
    await duplicateButton.waitUntilEnabled();
    await workspaceEditPage.clickCreateFinishButton(duplicateButton);

    // Duplicate workspace Data page is loaded.
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();
    expect(page.url()).toContain(`/${duplicateWorkspaceName.toLowerCase()}/data`);

    // Delete duplicate workspace via Workspace card in Your Workspaces page.
    await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.waitForLoad();

    await new WorkspaceCard(page).delete({ name: duplicateWorkspaceName });
  });
});
