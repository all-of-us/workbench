import Link from 'app/aou-elements/link';
import DataPage from 'app/data-page';
import WorkspacesPage, {FIELD} from 'app/workspaces-page';
import {signIn} from 'tests/app';
import Button from 'app/aou-elements/button';
import * as testData from 'resources/workspace-data';
import {performActions} from 'utils/test-utils';

describe('Creating new workspaces', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Create workspace - NO request for review', async () => {
    const newWorkspaceName = `aoutest-${Math.floor(Math.random() * 1000)}-${Math.floor(Date.now() / 1000)}`;
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    // create workspace with "No Review Requested" radiobutton selected
    await workspacesPage.createWorkspace(newWorkspaceName, 'Use All of Us free credits',);
    await verifyWorkspaceLinkOnDataPage(newWorkspaceName);
  });

  test('User can create a workspace using all inputs', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    const createNewWorkspaceButton  = await Button.forLabel(page, FIELD.createNewWorkspaceButton.textOption );
    await workspacesPage.clickAndWait(createNewWorkspaceButton);

    // fill out new workspace name
    const newWorkspaceName = await workspacesPage.fillOutWorkspaceName();

    // select Synthetic DataSet 2
    await workspacesPage.selectDataSet('2');

    // select Billing Account
    await workspacesPage.selectBillingAccount('Use All of Us free credits');

    // fill out question #1 - What is the primary purpose of your project?
    await workspacesPage.expandResearchPurposeGroup(true); // First, expand accordion: "Research purpose"
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

    const finishButton = await workspacesPage.getCreateWorkspaceButton();
    await finishButton.waitUntilEnabled();

    await finishButton.focus(); // bring button into viewport
    await workspacesPage.clickAndWait(finishButton);
    await workspacesPage.waitUntilNoSpinner();

    await verifyWorkspaceLinkOnDataPage(newWorkspaceName);
  });

  // helper function to check visible workspace link on Data page
  async function verifyWorkspaceLinkOnDataPage(workspaceName: string) {
    const dataPage = new DataPage(page);
    await dataPage.waitForLoad();

    const workspaceLink = new Link(page);
    await workspaceLink.withXpath(`//a[text()='${workspaceName}']`, {visible: true});
    expect(await workspaceLink.isVisible()).toBe(true);
  }

});
