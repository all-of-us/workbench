import WorkspaceDataPage from 'app/page/workspace-data-page';
import { MenuOption, WorkspaceAccessLevel } from 'app/text-labels';
import * as testData from 'resources/data/workspace-data';
import {
  findOrCreateWorkspace,
  findOrCreateWorkspaceCard,
  performActions,
  signInWithAccessToken,
  signOut
} from 'utils/test-utils';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { makeWorkspaceName } from 'utils/str-utils';
import Navigation, { NavLink } from 'app/component/navigation';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceCard from 'app/component/workspace-card';
import { config } from 'resources/workbench-config';
import { waitWhileLoading } from 'utils/waits-utils';
import { logger } from 'libs/logger';

describe('Workspace actions tests', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Workspace name with default CDR version
  const workspace = makeWorkspaceName();

  test('Can edit workspace via snowman menu', async () => {
    logger.info('Running test name: Can edit workspace via snowman menu');
    const workspaceCard = await findOrCreateWorkspaceCard(page, { workspaceName: workspace });
    await workspaceCard.selectSnowmanMenu(MenuOption.Edit, { waitForNav: true });

    const workspaceEditPage = new WorkspaceEditPage(page);

    // CDR Version Select is readonly. Get selected value.
    const cdrVersionSelect = workspaceEditPage.getCdrVersionSelect();
    const selectedValue = await cdrVersionSelect.getSelectedValue();

    // Change question #2 answer
    await performActions(page, testData.defaultAnswersResearchPurposeSummary);

    const updateButton = workspaceEditPage.getUpdateWorkspaceButton();
    await workspaceEditPage.clickCreateFinishButton(updateButton);

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    // Check Workspace Information in About page
    // Check CDR version
    await dataPage.openAboutPage();
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();
    const cdrValue = await aboutPage.getCdrVersion();
    expect(cdrValue).toEqual(expect.stringContaining(selectedValue));

    // Check CreationDate string is a valid date and compare year string.
    const creationDate = await aboutPage.getCreationDate();
    // Current AoU Date format: "Tue Jun 16 2020"
    const splitDate = creationDate.split(' ');
    const year = splitDate[splitDate.length - 1];
    const utcYear = new Date(creationDate).getFullYear();
    expect(Number(year)).toBe(utcYear);
    expect(!isNaN(Date.parse(creationDate))).toBe(true);

    // Check LastUpdatedDate (Same date format as in Creation Date) string is a valid date format.
    const lastUpdatedDate = await aboutPage.getLastUpdatedDate();
    expect(!isNaN(Date.parse(lastUpdatedDate))).toBe(true);

    // LastUpdatedDate should be today date. See format option https://www.w3schools.com/jsref/jsref_tolocalestring.asp
    const todayWeekday = new Date().toLocaleString('en-us', { weekday: 'short' });
    const todayYear = new Date().toLocaleString('en-us', { year: 'numeric' });
    const todayDay = new Date().toLocaleString('en-us', { day: '2-digit' });

    const lastUpdatedWeekday = new Date(lastUpdatedDate).toLocaleString('en-us', { weekday: 'short' });
    const lastUpdatedYear = new Date(lastUpdatedDate).toLocaleString('en-us', { year: 'numeric' });
    const lastUpdatedDay = new Date(lastUpdatedDate).toLocaleString('en-us', { day: '2-digit' });

    expect(todayWeekday).toBe(lastUpdatedWeekday);
    expect(todayYear).toBe(lastUpdatedYear);
    expect(todayDay).toBe(lastUpdatedDay);

    await signOut(page);
  });

  test('Can access Edit Workspace page via workspace-action menu', async () => {
    logger.info('Running test name: Can access Edit Workspace page via workspace-action menu');
    const workspaceCard = await findOrCreateWorkspaceCard(page, { workspaceName: workspace });

    // Verify Workspace Access Level is showing OWNER.
    const accessLevel = await workspaceCard.getWorkspaceAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Owner);

    // Open Workspace Data page
    await workspaceCard.clickWorkspaceName();

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();
    await dataPage.openAboutPage();
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();
    await aboutPage.editWorkspace();

    const workspaceEditPage = new WorkspaceEditPage(page);

    // Verify CDR Version Select is readonly
    const cdrVersionSelect = workspaceEditPage.getCdrVersionSelect();
    expect(await cdrVersionSelect.isDisabled()).toBe(true);

    // Verify Update button is readonly
    const updateButton = workspaceEditPage.getUpdateWorkspaceButton();
    expect(await updateButton.isCursorNotAllowed()).toBe(false);
    // Finish update is not needed
    await signOut(page);
  });

  test('Can duplicate workspace via Workspace card', async () => {
    logger.info('Running test name: Can duplicate workspace via Workspace card');
    const workspaceCard = await findOrCreateWorkspaceCard(page, { workspaceName: workspace });
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    // Fill out Workspace Name should be just enough for successful duplication
    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.getWorkspaceNameTextbox().clear();
    const duplicateWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();

    const finishButton = workspaceEditPage.getDuplicateWorkspaceButton();
    await workspaceEditPage.requestForReviewRadiobutton(false);
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
    expect(await WorkspaceCard.findCard(page, duplicateWorkspaceName)).toBeFalsy();
    await signOut(page);
  });

  test('Can access Duplicate Workspace page via Workspace-action menu', async () => {
    logger.info('Running test name: Can access Duplicate Workspace page via Workspace-action menu');
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.selectWorkspaceAction(MenuOption.Duplicate);

    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.waitForLoad();

    const finishButton = workspaceEditPage.getDuplicateWorkspaceButton();
    expect(await finishButton.isCursorNotAllowed()).toBe(true);

    // Fill out Workspace Name
    await workspaceEditPage.getWorkspaceNameTextbox().clear();
    await workspaceEditPage.fillOutWorkspaceName();
    // select "Share workspace with same set of collaborators radiobutton
    await workspaceEditPage.clickShareWithCollaboratorsCheckbox();

    await workspaceEditPage.requestForReviewRadiobutton(false);
    await finishButton.waitUntilEnabled();
    expect(await finishButton.isCursorNotAllowed()).toBe(false);
    // Click Finish button to clone workspace is not needed.
    await signOut(page);
  });

  test('Can share workspace to another owner', async () => {
    logger.info('Running test name: Can share workspace to another owner');
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.openAboutPage();
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();

    const collaborator = config.collaboratorUsername;
    let shareModal = await aboutPage.openShareModal();
    await shareModal.shareWithUser(collaborator, WorkspaceAccessLevel.Owner);
    // Collab list is refreshed.
    await waitWhileLoading(page);

    let accessLevel = await aboutPage.findUserInCollaboratorList(collaborator);
    expect(accessLevel).toBe(WorkspaceAccessLevel.Owner);

    shareModal = await aboutPage.openShareModal();
    await shareModal.removeUser(collaborator);
    await waitWhileLoading(page);

    accessLevel = await aboutPage.findUserInCollaboratorList(collaborator);
    expect(accessLevel).toBeNull();
    await signOut(page);
  });

  test('Can share workspace to reader', async () => {
    logger.info('Running test name: Can share workspace to reader');
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    await removeCollaborators();

    const collaborator = config.collaboratorUsername;
    const aboutPage = new WorkspaceAboutPage(page);
    const shareModal = await aboutPage.openShareModal();
    await shareModal.shareWithUser(collaborator, WorkspaceAccessLevel.Reader);
    // Collab list is refreshed.
    await waitWhileLoading(page);

    const accessLevel = await aboutPage.findUserInCollaboratorList(collaborator);
    expect(accessLevel).toBe(WorkspaceAccessLevel.Reader);
    await signOut(page);
  });

  test('Can share workspace to writer', async () => {
    logger.info('Running test name: Can share workspace to writer');
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    await removeCollaborators();

    const collaborator = config.writerUserName;
    const aboutPage = new WorkspaceAboutPage(page);
    const shareModal = await aboutPage.openShareModal();
    await shareModal.shareWithUser(collaborator, WorkspaceAccessLevel.Writer);
    // Collab list is refreshed.
    await waitWhileLoading(page);

    const accessLevel = await aboutPage.findUserInCollaboratorList(collaborator);
    expect(accessLevel).toBe(WorkspaceAccessLevel.Writer);
    await signOut(page);
  });
});

// Remove existing collaborators in Workspace About page.
async function removeCollaborators() {
  const dataPage = new WorkspaceDataPage(page);
  await dataPage.waitForLoad();
  await dataPage.openAboutPage();
  const aboutPage = new WorkspaceAboutPage(page);
  await aboutPage.waitForLoad();
  await aboutPage.removeCurrentCollaborators();
  await waitWhileLoading(page);
  await aboutPage.waitForLoad();
}
