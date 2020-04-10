import {Page} from 'puppeteer';
import {pageUrl} from '../resources/enums';
import {findButton} from './aou-elements/xpath-finder';
import AuthenticatedPage from './authenticated-page';
import WorkspaceEditPage from './workspace-edit-page';

const faker = require('faker/locale/en_US');

export const PAGE = {
  TITLE: 'View Workspace',
};

export const FIELD_LABEL = {
  CREATE_NEW_WORKSPACE: 'Create a New Workspace',
};

export default class WorkspacesPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await this.waitUntilTitleMatch(PAGE.TITLE);
      await this.page.waitForXPath('//h3[normalize-space(text())="Workspaces"]', {visible: true});
      return true;
    } catch (e) {
      return false;
    }
  }

  /**
   * Load 'Your Workspaces' page and ensure page load is completed.
   */
  async load(): Promise<this> {
    await this.loadPageUrl(pageUrl.WORKSPACES);
    return this;
  }

  async getCreateNewWorkspaceButton() {
    return findButton(this.page, {normalizeSpace: FIELD_LABEL.CREATE_NEW_WORKSPACE}, {visible: true});
  }

  // tests helpers: combined a number of steps in one function

 /**
  * Perform following steps:
  *
  * 1: go to My Workspaces page
  * 2: click Create New Workspace link (button)
  * 3: wait until Edit page is loaded and ready
  * 4: return
  */
  async clickCreateNewWorkspace(): Promise<WorkspaceEditPage> {
    const link = await this.getCreateNewWorkspaceButton();
    await this.clickAndWait(link);
    const workspaceEdit = new WorkspaceEditPage(this.page);
    await workspaceEdit.waitForLoad();
    return workspaceEdit;
  }

  /**
   * Create a simple and basic new workspace end-to-end.
   */
  async createWorkspace(workspaceName: string, billingAccount: string) {

    const editPage = await this.clickCreateNewWorkspace();
    // wait for Billing Account default selected value
    await editPage.waitForTextExists('Use All of Us free credits');

    await (await editPage.getWorkspaceNameTextbox()).type(workspaceName);
    await (await editPage.getWorkspaceNameTextbox()).tabKey();

    // select Synthetic Data Set 2
    await editPage.selectDataSet('2');

    // select Billing Account
    await editPage.selectBillingAccount(billingAccount);

    // expand Disease purpose section
    await editPage.expandResearchPurposeGroup(true);

    // Enter value in 'Disease-focused research'
    await editPage.fillOutDiseaseFocusedResearch();

    // check Educational Purpose checkbox
    const educationPurpose = editPage.question1_educationalPurpose();
    await (await educationPurpose.asCheckBox()).check();

    // check For-Profit Purpose checkbox
    const forProfitPurpose = editPage.question1_forProfitPurpose();
    await (await forProfitPurpose.asCheckBox()).check();

    // check Other-Purpose checkbox
    await editPage.fillOutOtherPurpose();

    // 2. Please provide a summary of your research purpose by responding to the questions below.
    const scientificQuestions = editPage.question2_scientificQuestionsIntendToStudy();
    await (await scientificQuestions.asTextArea()).type(faker.lorem.word());

    const scientificApproaches = editPage.question2_scientificApproaches();
    await (await scientificApproaches.asTextArea()).type(faker.lorem.word());

    const anticipatedFindings = editPage.question2_anticipatedFindings();
    await (await anticipatedFindings.asTextArea()).type(faker.lorem.word());

    // 3. The All of Us Research Program encourages researchers to disseminate ....
    const publicationInJournal = editPage.publicationInJournal();
    await (await publicationInJournal.asCheckBox()).check();

    // 4. The All of Us Research Program would like to understand how ....
    const increaseWellness = editPage.increaseWellnessResilience();
    await (await increaseWellness.asCheckBox()).check();

    // 5. Population of interest: use default values. Using default value

    // 6. Request for Review of Research Purpose Description. Using default value
    await editPage.requestForReviewRadiobutton(false);

    // click CREATE WORKSPACE button
    await editPage.clickCreateFinishButton();
  }

}
