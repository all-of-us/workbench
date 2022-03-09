import { createWorkspace, findWorkspaceCard, openTab, signInWithAccessToken } from 'utils/test-utils';
import { config } from 'resources/workbench-config';
import { makeRandomName } from 'utils/str-utils';
import WorkspaceEditPage, { AccessTierDisplayNames } from 'app/page/workspace-edit-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import { LinkText, MenuOption, Tabs, WorkspaceAccessLevel } from 'app/text-labels';
import ShareModal from 'app/modal/share-modal';
import { waitWhileLoading } from 'utils/waits-utils';
import WorkspacesPage from 'app/page/workspaces-page';
import HomePage from 'app/page/home-page';
import UnavailableTierModal from 'app/modal/unavailable-tier-modal';
import DataAccessRequirementsPage from 'app/page/data-access-requirements-page';

describe('Share Controlled Tier Workspace', () => {
  const hasCTAccessUser = config.WRITER_USER;
  const notCTUser = config.EGRESS_TEST_USER; // Without CT access

  const workspaceName = makeRandomName('e2eShareControlledTierWorkspace');

  test('User without CT access cannot create CT workspace', async () => {
    await signInWithAccessToken(page, notCTUser);

    const homePage = new HomePage(page);
    const createNewWorkspace = homePage.getCreateNewWorkspaceLink();
    await createNewWorkspace.clickAndWait();

    const editPage = new WorkspaceEditPage(page);
    await editPage.selectAccessTier(AccessTierDisplayNames.Controlled);

    const modal = new UnavailableTierModal(page);
    await modal.waitForLoad();

    const modalText = await modal.getTextContent();
    expect(modalText).toContain(
      'Before creating your workspace, please complete the data access requirements to gain access.'
    );

    const getStartedButton = await modal.getGetStartedButton();
    await getStartedButton.clickAndWait();

    const dataAccessRequirementsPage = new DataAccessRequirementsPage(page);
    await dataAccessRequirementsPage.waitForLoad();
  });

  test('Can share CT workspace only with user with CT access', async () => {
    await signInWithAccessToken(page);
    await createWorkspace(page, {
      workspaceName,
      dataAccessTier: AccessTierDisplayNames.Controlled,
      cdrVersionName: config.CONTROLLED_TIER_CDR_VERSION_NAME
    });
    const aboutPage = new WorkspaceAboutPage(page);
    await openTab(page, Tabs.About, aboutPage);

    await aboutPage.selectWorkspaceAction(MenuOption.Share, { waitForNav: false });
    let shareModal = new ShareModal(page);
    await shareModal.waitForLoad();

    // Search for user without CT access returns empty result in Share modal.
    const userFound = await shareModal.userExists(notCTUser);
    expect(userFound).toBe(false);

    await shareModal.clickButton(LinkText.Cancel, { waitForClose: true });
    await waitWhileLoading(page);

    // Share with a WRITER
    await aboutPage.selectWorkspaceAction(MenuOption.Share, { waitForNav: false });
    shareModal = new ShareModal(page);
    await shareModal.shareWithUser(hasCTAccessUser, WorkspaceAccessLevel.Writer);
  });

  test('Writer can open and duplicate CT workspace', async () => {
    await signInWithAccessToken(page, hasCTAccessUser);

    // Find workspace created by previous test. If not found, test will fail.
    const workspaceCard = await findWorkspaceCard(page, workspaceName);

    expect(await workspaceCard.controlledTiersIconExists()).toBe(true);

    const accessLevel = await workspaceCard.getAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Writer);

    // Verify Snowman menu: Share, Edit and Delete actions are disabled.
    await workspaceCard.verifyWorkspaceCardMenuOptions(WorkspaceAccessLevel.Writer);

    const aboutPage = await new WorkspacesPage(page).openAboutPage(workspaceCard);

    const collaboratorList = await aboutPage.findUsersInCollaboratorList();
    // Verify OWNER exists in Collaborators list
    expect(collaboratorList.get(WorkspaceAccessLevel.Owner).some((item) => item.includes(process.env.USER_NAME))).toBe(
      true
    );
    // Verify WRITER exists Collaborators list
    expect(collaboratorList.get(WorkspaceAccessLevel.Writer).some((item) => item.includes(hasCTAccessUser))).toBe(true);
  });
});
