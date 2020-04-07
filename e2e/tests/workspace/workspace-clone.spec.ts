import Button from '../../app/aou-elements/button';
import Link from '../../app/aou-elements/link';
import Textbox from '../../app/aou-elements/textbox';
import DataPage from '../../app/data-page';
import {FIELD_FINDER} from '../../app/workspace-edit-page';
import WorkspacesPage, {FIELD_FINDER as WORKSPACE_FIELD_FINDER} from '../../app/workspaces-page';
import {signIn} from '../app';
import * as testData from '../../resources/workspace-test-data';



describe('User can clone workspace', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  afterEach(async () => {
    await jestPuppeteer.resetBrowser();
  });

  test.skip('ttest out', async () => {
    const newWorkspaceName = `aoutest-${Math.floor(Math.random() * 1000)}-${Math.floor(Date.now() / 1000)}`;

    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    const button1  = await Button.forLabel(page,  WORKSPACE_FIELD_FINDER.CREATE_A_NEW_WORKSPACE_BUTTON.textOption );
    await workspacesPage.clickAndWait(button1);

    const textbox1 = await Textbox.forLabel(page, FIELD_FINDER.WORKSPACE_NAME.textOption);
    await textbox1.type(newWorkspaceName);

    await jestPuppeteer.debug();

  });

  test('create', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    const createNewWorkspaceButton  = await Button.forLabel(page,  WORKSPACE_FIELD_FINDER.CREATE_A_NEW_WORKSPACE_BUTTON.textOption );
    await workspacesPage.clickAndWait(createNewWorkspaceButton);

      // fill out new workspace name
    const newWorkspaceName = await workspacesPage.fillOutWorkspaceName();
    console.log(newWorkspaceName);

      // select Synthetic DataSet 2
    await workspacesPage.selectDataSet('2');

      // select Billing Account
    await workspacesPage.selectBillingAccount('Use All of Us free credits');

      // fill out question #1 - What is the primary purpose of your project?
    await workspacesPage.expandResearchPurposeGroup(true); // First, expand accordion: "Research purpose"
    await workspacesPage.performUiActions(testData.defaultPrimaryPurposeAnswers);

      // fill out question #2 - Please provide a summary of your research purpose by responding to the questions.
    await workspacesPage.performUiActions(testData.defaultResearchPurposeSummaryAnswers);

      // fill out question #3 - The All of Us Research Program encourages researchers to disseminate their research findings...
    await workspacesPage.performUiActions(testData.defaultDisseminateResearchFindingsAnswers);

      // fill out question #4 - select all of the statements below that describe the outcomes you anticipate from your research.
    await workspacesPage.performUiActions(testData.defaultAnticipatedOutcomesFromResearchAnswers);

      // fill out question #5 - Population interest
    await workspacesPage.performUiActions(testData.defaultPopulationOfInterestAnswers);

    // fill out question #6 - Request for Review of Research Purpose Description
    await workspacesPage.performUiActions(testData.defaultRequestForReviewAnswers);

    await page.waitFor(10000);
    
    const finishButton = await workspacesPage.getCreateWorkspaceButton();
    expect(await finishButton.waitUntilEnabled()).toBe(true);

    await finishButton.focus(); // bring button into viewport
    await this.clickAndWait(finishButton);
    await this.waitUntilNoSpinner();

    // check Data page is loaded
    const dataPage = new DataPage(page);
    await dataPage.waitForLoad();

    // checking new workspace link is found
    const workspaceLink = new Link(page);
    await workspaceLink.withXpath(`//a[text()='${newWorkspaceName}']`, {visible: true});
    expect(await workspaceLink.isVisible()).toBeTruthy();

  });


});
