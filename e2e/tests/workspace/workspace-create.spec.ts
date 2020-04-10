import Button from 'app/aou-elements/button';
import Link from 'app/aou-elements/link';
import DataPage from 'app/data-page';
import WorkspacesPage, {fields} from 'app/workspaces-page';
import * as testData from 'resources/workspace-test-data';
import {signIn} from 'tests/app';


describe('Creating new workspaces', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  afterEach(async () => {
    await jestPuppeteer.resetBrowser();
  });

  test('User can create a simple workspace using minimally required inputs', async () => {
    const newWorkspaceName = `aoutest-${Math.floor(Math.random() * 1000)}-${Math.floor(Date.now() / 1000)}`;
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();
    await workspacesPage.createWorkspace(newWorkspaceName, 'Use All of Us free credits',);
    await verifyWorkspaceLinkOnDataPage(newWorkspaceName);
  });

  test('User can create a workspace using all inputs', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    const createNewWorkspaceButton  = await Button.forLabel(page, fields.createNewWorkspaceButton.textOption );
    await workspacesPage.clickAndWait(createNewWorkspaceButton);

    // fill out new workspace name
    const newWorkspaceName = await workspacesPage.fillOutWorkspaceName();

    // select Synthetic DataSet 2
    await workspacesPage.selectDataSet('2');

    // select Billing Account
    await workspacesPage.selectBillingAccount('Use All of Us free credits');

    // fill out question #1 - What is the primary purpose of your project?
    await workspacesPage.expandResearchPurposeGroup(true); // First, expand accordion: "Research purpose"
    await workspacesPage.performUiActions(testData.defaultAnswersPrimaryPurpose);

    // fill out question #2 - Please provide a summary of your research purpose by responding to the questions.
    await workspacesPage.performUiActions(testData.defaultAnswersResearchPurposeSummary);

    // fill out question #3 - The All of Us Research Program encourages researchers to disseminate their research findings...
    await workspacesPage.performUiActions(testData.defaultAnswersDisseminateResearchFindings);

    // fill out question #4 - select all of the statements below that describe the outcomes you anticipate from your research.
    await workspacesPage.performUiActions(testData.defaultAnswersAnticipatedOutcomesFromResearch);

    // fill out question #5 - Population interest
    await workspacesPage.performUiActions(testData.defaultAnswersPopulationOfInterest);

    // fill out question #6 - Request for Review of Research Purpose Description
    await workspacesPage.performUiActions(testData.defaultAnswersRequestForReview);

    const finishButton = await workspacesPage.getCreateWorkspaceButton();
    expect(await finishButton.waitUntilEnabled()).toBe(true);

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
