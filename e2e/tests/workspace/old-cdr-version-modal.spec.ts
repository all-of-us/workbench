import { findOrCreateWorkspaceCard, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import { MenuOption } from 'app/text-labels';
import WorkspacesPage from 'app/page/workspaces-page';
import { makeWorkspaceName } from 'utils/str-utils';
import OldCdrVersionModal from 'app/modal/old-cdr-version-modal';
import WorkspaceEditPage from 'app/page/workspace-edit-page';

describe('OldCdrVersion Modal restrictions', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('User cannot create a workspace with an old CDR Version without consenting to the restrictions', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    const editPage = await workspacesPage.fillOutRequiredCreationFields(makeWorkspaceName());

    // select an old CDR Version
    await editPage.selectCdrVersion(config.altCdrVersionName);

    const createButton = editPage.getCreateWorkspaceButton();
    expect(await createButton.isCursorNotAllowed()).toBe(true);

    // fill out the modal checkboxes
    const modal = new OldCdrVersionModal(page);
    await modal.consentToOldCdrRestrictions();

    // now we can continue
    await createButton.waitUntilEnabled();
    await editPage.clickCreateFinishButton(createButton);
  });

  const workspace = 'e2eCloneWorkspaceCDRConsentTest';

  test('OWNER cannot duplicate workspace to an older CDR Version without consenting to restrictions', async () => {
    const workspaceCard = await findOrCreateWorkspaceCard(page, { workspaceName: workspace });
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    // Fill out Workspace Name should be just enough for successful duplication
    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.getWorkspaceNameTextbox().clear();
    await workspaceEditPage.fillOutWorkspaceName();

    // change CDR Version
    await workspaceEditPage.selectCdrVersion(config.altCdrVersionName);

    const finishButton = workspaceEditPage.getDuplicateWorkspaceButton();
    expect(await finishButton.isCursorNotAllowed()).toBe(true);
  });

  test('OWNER cannot bypass older CDR Version restrictions by clicking cancel', async () => {
    const workspaceCard = await findOrCreateWorkspaceCard(page, { workspaceName: workspace });
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    const workspaceEditPage = new WorkspaceEditPage(page);

    // fill out the fields required for duplication and observe that duplication is enabled
    await workspaceEditPage.fillOutRequiredDuplicationFields();
    const duplicateButton = workspaceEditPage.getDuplicateWorkspaceButton();
    await duplicateButton.waitUntilEnabled();

    // change CDR Version
    await workspaceEditPage.selectCdrVersion(config.altCdrVersionName);
    expect(await duplicateButton.isCursorNotAllowed()).toBe(true);

    const modal = new OldCdrVersionModal(page);
    const cancelButton = await modal.getCancelButton();
    await cancelButton.click();

    // the CDR version is forcibly reverted back to the default
    const cdrVersionSelect = await workspaceEditPage.getCdrVersionSelect();
    expect(await cdrVersionSelect.getSelectedValue()).toBe(config.defaultCdrVersionName);
  });
});
