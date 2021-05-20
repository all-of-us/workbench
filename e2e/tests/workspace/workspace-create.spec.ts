import WorkspaceDataPage from 'app/page/workspace-data-page';
import WorkspacesPage, { FieldSelector } from 'app/page/workspaces-page';
import { signInWithAccessToken, performActions } from 'utils/test-utils';
import Button from 'app/element/button';
import * as testData from 'resources/data/workspace-data';
import { makeWorkspaceName } from 'utils/str-utils';
import { UseFreeCredits } from 'app/page/workspace-base';
import WorkspaceEditPage, { AccessTierDisplayNames } from 'app/page/workspace-edit-page';
import { config } from 'resources/workbench-config';

describe('Creating new workspaces', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('Create workspace - NO request for review', async () => {
    const newWorkspaceName = makeWorkspaceName();
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    // create workspace with "No Review Requested" radiobutton selected
    const modalTextContent = await workspacesPage.createWorkspace(newWorkspaceName);

    // Pick out few sentences to verify
    expect(modalTextContent).toContain('Create Workspace');
    expect(modalTextContent).toContain(
      'Will be displayed publicly to inform All of Us research participants. ' +
        'Therefore, please verify that you have provided sufficiently detailed responses in plain language.'
    );
    expect(modalTextContent).toContain('You can also make changes to your answers after you create your workspace.');

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.verifyWorkspaceNameOnDataPage(newWorkspaceName);

    // Verify the CDR version displays in the workspace navigation bar
    expect(await dataPage.getCdrVersion()).toBe(config.defaultCdrVersionName);

    // cleanup
    await dataPage.deleteWorkspace();
  });

  test('User can create a workspace using all inputs', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    const createNewWorkspaceButton = Button.findByName(page, FieldSelector.CreateNewWorkspaceButton.textOption);
    await createNewWorkspaceButton.clickAndWait();

    const workspaceEditPage = new WorkspaceEditPage(page);

    // fill out new workspace name
    const newWorkspaceName = await workspaceEditPage.fillOutWorkspaceName();

    // select the default CDR Version
    await workspaceEditPage.selectCdrVersion();

    // select Billing Account
    await workspaceEditPage.selectBillingAccount(UseFreeCredits);

    // fill out question #1 - What is the primary purpose of your project?
    await workspaceEditPage.expandResearchPurposeGroup(true); // First, expand accordion: "Research purpose"
    await performActions(page, testData.defaultAnswersPrimaryPurpose);

    // fill out question #2 - Please provide a summary of your research purpose by responding to the questions.
    await performActions(page, testData.defaultAnswersResearchPurposeSummary);

    // fill out question #3 - The All of Us Research Program encourages researchers to disseminate their research findings...
    await performActions(page, testData.defaultAnswersDisseminateResearchFindings);

    // fill out question #4 - select all of the statements below that describe the outcomes you anticipate from your research.
    await performActions(page, testData.defaultAnswersAnticipatedOutcomesFromResearch);

    // fill out question #5 - Population interest
    await performActions(page, testData.defaultAnswersPopulationOfInterest);

    // fill out question #6 - Request for Review of Research Purpose Description
    // -- No Review Required
    await performActions(page, testData.defaultAnswersRequestForReview);

    const finishButton = workspaceEditPage.getCreateWorkspaceButton();
    await finishButton.waitUntilEnabled();
    await workspaceEditPage.clickCreateFinishButton(finishButton);

    const dataPage1 = new WorkspaceDataPage(page);
    await dataPage1.verifyWorkspaceNameOnDataPage(newWorkspaceName);
  });

  // TODO for Controlled Tier Beta: ensure the user has CT access first

  test('User can create a workspace in the Controlled Tier', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    const name = makeWorkspaceName();
    const createPage = await workspacesPage.fillOutRequiredCreationFields(name);

    const cdrVersionSelect = createPage.getCdrVersionSelect();
    expect(await cdrVersionSelect.getSelectedValue()).toBe(config.defaultCdrVersionName);

    await createPage.selectAccessTier(AccessTierDisplayNames.Controlled);

    // observe that the CDR Version default has changed
    const selectedCdrVersion = await cdrVersionSelect.getSelectedValue();
    expect(selectedCdrVersion).not.toBe(config.defaultCdrVersionName);

    // click CREATE WORKSPACE button
    const createButton = createPage.getCreateWorkspaceButton();
    await createButton.waitUntilEnabled();
    await createPage.clickCreateFinishButton(createButton);

    const dataPage = new WorkspaceDataPage(page);

    // Verify that the CDR version is what we expect
    expect(await dataPage.getCdrVersion()).toBe(selectedCdrVersion);

    // cleanup
    await dataPage.deleteWorkspace();
  });

  // // helper function to check visible workspace link on Data page
  // async function verifyWorkspaceLinkOnDataPage(workspaceName: string): Promise<WorkspaceDataPage> {
  //   const dataPage = new WorkspaceDataPage(page);
  //   await dataPage.waitForLoad();

  //   const workspaceLink = new Link(page, `//a[text()='${workspaceName}']`);
  //   await workspaceLink.waitForXPath({visible: true});
  //   expect(await workspaceLink.isVisible()).toBe(true);
  //   return dataPage;
  // }
});
