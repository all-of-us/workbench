import { Page } from 'puppeteer';
import { createWorkspace, findWorkspaceCard, openTab, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import { makeRandomName } from 'utils/str-utils';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import { AccessTierDisplayNames, LinkText, MenuOption, Tabs, WorkspaceAccessLevel } from 'app/text-labels';
import ShareModal from 'app/modal/share-modal';
import { waitForFn, waitWhileLoading } from 'utils/waits-utils';
import WorkspacesPage from 'app/page/workspaces-page';
import HomePage from 'app/page/home-page';
import UnavailableTierModal from 'app/modal/unavailable-tier-modal';
import DataAccessRequirementsPage from 'app/page/data-access/data-access-requirements-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';

describe.skip('Share Controlled Tier Workspace', () => {
  const hasCTAccessUser = config.WRITER_USER;
  const notCTUser = config.EGRESS_TEST_USER; // Without CT access

  const workspaceName = makeRandomName('e2eShareControlledTierWorkspace', { includeHyphen: false });

  async function selectControlledTier(page: Page, createPage: WorkspaceEditPage): Promise<UnavailableTierModal> {
    await createPage.selectAccessTier(AccessTierDisplayNames.Controlled);
    const modal = new UnavailableTierModal(page);
    return modal.waitForLoad();
  }

  test('User without CT access cannot create CT workspace', async () => {
    await signInWithAccessToken(page, notCTUser);

    const homePage = new HomePage(page);
    const createNewWorkspace = homePage.getCreateNewWorkspaceLink();
    await createNewWorkspace.clickAndWait();

    const createPage = new WorkspaceEditPage(page);
    let modal = await selectControlledTier(page, createPage);

    const modalText = await modal.getTextContent();
    expect(modalText).toContain(
      'Before creating your workspace, please complete the data access requirements to gain access.'
    );

    await modal.clickButton(LinkText.Cancel);
    await modal.waitUntilClose();

    // Create Workspace page forces the value of Access Tier back to Registered Tier.
    const dataAccessSelect = createPage.getDataAccessTierSelect();
    const dataAccessSelectValue = await dataAccessSelect.getSelectedValue();
    expect(dataAccessSelectValue).toBe(AccessTierDisplayNames.Registered);

    // Value of CDR Version is unchanged.
    const cdrVersionSelect = createPage.getCdrVersionSelect();
    expect(await cdrVersionSelect.getSelectedValue()).toBe(config.DEFAULT_CDR_VERSION_NAME);

    // Click Get Started button takes user to Data Access Requirements page.
    modal = await selectControlledTier(page, createPage);
    const getStartedButton = modal.getGetStartedButton();
    await getStartedButton.clickAndWait();

    const dataAccessRequirementsPage = new DataAccessRequirementsPage(page);
    await dataAccessRequirementsPage.waitForLoad();
  });

  test('Can share CT workspace only with user with CT access', async () => {
    await signInWithAccessToken(page);

    // Create a workspace in the Controlled Tier
    await createWorkspace(page, {
      workspaceName,
      dataAccessTier: AccessTierDisplayNames.Controlled,
      cdrVersionName: config.CONTROLLED_TIER_CDR_VERSION_NAME
    });

    const dataPage = new WorkspaceDataPage(page);
    // Verify that the CDR version is CT CDR version.
    expect(await dataPage.getCdrVersion()).toBe(config.CONTROLLED_TIER_CDR_VERSION_NAME);

    const aboutPage = new WorkspaceAboutPage(page);
    await openTab(page, Tabs.About, aboutPage);

    await aboutPage.selectWorkspaceAction(MenuOption.Share, { waitForNav: false });
    let shareModal = new ShareModal(page);
    await shareModal.waitForLoad();

    // Search for user without CT access returns empty result in Share modal.
    const userFound: boolean = await shareModal.userExists(notCTUser);
    expect(userFound).toBe(false);

    await shareModal.clickButton(LinkText.Cancel, { waitForClose: true });
    await waitWhileLoading(page);

    // Share with a WRITER
    await aboutPage.selectWorkspaceAction(MenuOption.Share, { waitForNav: false });
    shareModal = new ShareModal(page);
    await shareModal.shareWithUser(hasCTAccessUser, WorkspaceAccessLevel.Writer);
  });

  // Depends on previous test
  test('Writer can open and duplicate CT workspace', async () => {
    await signInWithAccessToken(page, hasCTAccessUser);

    // Find workspace created by previous test. If not found, test will fail.
    const workspaceCard = await findWorkspaceCard(page, workspaceName);

    expect(await workspaceCard.controlledTierIconExists()).toBe(true);

    const accessLevel = await workspaceCard.getAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Writer);

    // Verify Snowman menu: Share, Edit and Delete actions are disabled.
    await workspaceCard.verifyWorkspaceCardMenuOptions(WorkspaceAccessLevel.Writer);

    const aboutPage = await new WorkspacesPage(page).openAboutPage(workspaceCard);
    let collaboratorList = await aboutPage.findUsersInCollaboratorList();
    // Verify OWNER exists in Collaborators list
    expect(collaboratorList.get(WorkspaceAccessLevel.Owner).some((item) => item.includes(process.env.USER_NAME))).toBe(
      true
    );
    // Verify WRITER exists Collaborators list
    expect(collaboratorList.get(WorkspaceAccessLevel.Writer).some((item) => item.includes(hasCTAccessUser))).toBe(true);

    // Access Workspace Duplicate page via Workspace action menu.
    await aboutPage.selectWorkspaceAction(MenuOption.Duplicate);

    // Fill out Workspace name
    const duplicateWorkspaceName = makeRandomName('e2eDuplicateControlledTierWorkspace', { includeHyphen: false });
    const duplicatePage = new WorkspaceEditPage(page);
    await duplicatePage.getWorkspaceNameTextbox().clear();
    await duplicatePage.fillOutWorkspaceName(duplicateWorkspaceName);

    // Data Access Tier Select is readonly.
    const accessTierSelect = duplicatePage.getDataAccessTierSelect();
    expect(await accessTierSelect.expectEnabled(false));

    // Fill out fields and click Finish button.
    await duplicatePage.getShareWithCollaboratorsCheckbox().check();
    await duplicatePage.requestForReviewRadiobutton(false);

    const finishButton = duplicatePage.getDuplicateWorkspaceButton();
    await finishButton.waitUntilEnabled();
    await duplicatePage.clickCreateFinishButton(finishButton);

    // Duplicate workspace Data page is loaded.
    await new WorkspaceDataPage(page).waitForLoad();
    const regx = new RegExp(duplicateWorkspaceName, 'i');
    expect(regx.exec(page.url())).not.toBeNull();

    await openTab(page, Tabs.About, aboutPage);
    // Verify 2 owners in Collaborators list
    await waitForFn(async () => (await aboutPage.findUsersInCollaboratorList()).size === 2);
    collaboratorList = await aboutPage.findUsersInCollaboratorList();

    expect(collaboratorList.get(WorkspaceAccessLevel.Owner).some((item) => item.includes(process.env.USER_NAME))).toBe(
      true
    );
    expect(collaboratorList.get(WorkspaceAccessLevel.Owner).some((item) => item.includes(hasCTAccessUser))).toBe(true);
  });
});
