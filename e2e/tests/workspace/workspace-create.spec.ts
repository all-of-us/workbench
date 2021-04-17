import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspacesPage, { FieldSelector } from 'app/page/workspaces-page';
import { findOrCreateWorkspaceCard, performActions, signInWithAccessToken } from 'utils/test-utils';
import Button from 'app/element/button';
import * as testData from 'resources/data/workspace-data';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { config } from 'resources/workbench-config';
import { LinkText, MenuOption } from 'app/text-labels';
import OldCdrVersionModal from 'app/modal/old-cdr-version-modal';
import { makeWorkspaceName } from 'utils/str-utils';
import Navigation, { NavLink } from 'app/component/navigation';
import WorkspaceCard from 'app/component/workspace-card';

describe('Workspace tests', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Workspace name with new CDR version
  const workspace = makeWorkspaceName();

  test('Create with default CDR version', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    const modalTexts = await workspacesPage.createWorkspace(workspace);

    // Pick out few sentences to verify
    expect(modalTexts).toContain('Create Workspace');
    expect(modalTexts).toContainEqual(
      expect.stringContaining('please verify that you have provided sufficiently detailed responses in plain language')
    );
    expect(modalTexts).toContain('You can also make changes to your answers after you create your workspace.');

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.verifyWorkspaceNameOnDataPage(workspace);

    // Verify the CDR version displays in the workspace navigation bar
    const cdrVersion = await dataPage.getCdrVersion();
    expect(cdrVersion).toBe(config.defaultCdrVersionName);
  });

  test('Cannot duplicate workspace to an older CDR version without consenting to restrictions', async () => {
    const workspaceCard = await findOrCreateWorkspaceCard(page, { workspaceName: workspace });
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    // Fill out Workspace Name should be just enough for successful duplication
    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.waitForLoad();

    // Change CDR Version to an older CDR version
    await workspaceEditPage.selectCdrVersion(config.altCdrVersionName);

    // Cancel Consenting to restriction modal
    const modal = new OldCdrVersionModal(page);
    expect(await modal.isLoaded()).toBe(true);
    // CLose modal without consenting to restrictions
    await modal.clickButton(LinkText.Cancel);

    // CDR version value is unchanged
    const select = await workspaceEditPage.getCdrVersionSelect();
    const selectValue = await select.getSelectedValue();
    expect(selectValue).toBe(config.defaultCdrVersionName);

    // Duplicate button is still disabled
    const finishButton = workspaceEditPage.getDuplicateWorkspaceButton();
    expect(await finishButton.isCursorNotAllowed()).toBe(true);

    // Fill out question #6 - Request for Review of Research Purpose Description
    // -- No Review Required
    await performActions(page, testData.defaultAnswersRequestForReview);

    // Duplicate button is now enabled
    expect(await finishButton.isCursorNotAllowed()).toBe(false);
    // Finish create is not needed
  });

  test('Can duplicate workspace to an older CDR version after consenting to restrictions', async () => {
    const workspaceCard = await findOrCreateWorkspaceCard(page, { workspaceName: workspace });
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    // Fill out Workspace Name should be just enough for successful duplication
    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.getWorkspaceNameTextbox().clear();
    const duplicateWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();

    // Change CDR Version
    await workspaceEditPage.selectCdrVersion(config.altCdrVersionName);

    // Wait for the warning modal and consent to the required restrictions
    const modal = new OldCdrVersionModal(page);
    await modal.consentToOldCdrRestrictions();

    await workspaceEditPage.requestForReviewRadiobutton(false);
    const finishButton = workspaceEditPage.getDuplicateWorkspaceButton();
    await workspaceEditPage.clickCreateFinishButton(finishButton);

    // Duplicate workspace Data page is loaded
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();
    // Remove dash from workspace name
    const formattedUrl = duplicateWorkspaceName.replace(/-/g, '');
    expect(page.url()).toContain(formattedUrl);

    // Delete duplicate workspace via Workspace card in Your Workspaces page.
    await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.waitForLoad();

    await WorkspaceCard.deleteWorkspace(page, duplicateWorkspaceName);
    expect(await WorkspaceCard.findCard(page, duplicateWorkspaceName)).toBeFalsy();
  });

  test('Cannot create when a required field is blank', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    const createWorkspaceButton = Button.findByName(page, FieldSelector.CreateNewWorkspaceButton.textOption);
    await createWorkspaceButton.clickAndWait();

    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.waitForLoad();

    // Fill out new workspace name
    await workspaceEditPage.fillOutWorkspaceName();

    // Choose default CDR Version
    // Choose default Billing Account

    // Leave question #1 answer blank - What is the primary purpose of your project?

    // Fill out question #2 - Please provide a summary of your research purpose by responding to the questions.
    await performActions(page, testData.defaultAnswersResearchPurposeSummary);

    // Fill out question #3 - The All of Us Research Program encourages researchers to disseminate their research findings...
    await performActions(page, testData.defaultAnswersDisseminateResearchFindings);

    // Fill out question #4 - select all of the statements below that describe the outcomes you anticipate from your research.
    await performActions(page, testData.defaultAnswersAnticipatedOutcomesFromResearch);

    // Fill out question #5 - Population interest
    await performActions(page, testData.defaultAnswersPopulationOfInterest);

    // Fill out question #6 - Request for Review of Research Purpose Description
    // -- No Review Required
    await performActions(page, testData.defaultAnswersRequestForReview);

    const finishButton = workspaceEditPage.getCreateWorkspaceButton();
    expect(await finishButton.isCursorNotAllowed()).toBe(true);
  });
});
