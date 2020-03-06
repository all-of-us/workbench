import {Page} from 'puppeteer';
import select from './aou-elements/select';
import {findButton} from './aou-elements/xpath-finder';
import AuthenticatedPage from './authenticated-page';
import WorkspaceEditPage, {FIELD_LABEL as EditPageLabel} from './workspace-edit-page';

const configs = require('../resources/workbench-config.js');
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

   /**
    * navigate to My Workspaces URL
    */
  async goToUrl(): Promise<void> {
    const pageUrl = configs.uiBaseUrl + configs.workspacesUrlPath;
    await this.page.goto(pageUrl, {waitUntil: ['domcontentloaded','networkidle0']});
    await this.waitForReady();
  }

  async isLoaded(): Promise<boolean> {
    await this.waitUntilTitleMatch(PAGE.TITLE);
    await this.page.waitForXPath('//h3[normalize-space(text())="Workspaces"]', {visible: true});
    return true;
  }

  async waitForReady(): Promise<this> {
    super.waitForReady();
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  async getCreateNewWorkspaceLink() {
    return findButton(this.page, {normalizeSpace: FIELD_LABEL.CREATE_NEW_WORKSPACE}, {visible: true});
  }


   // tests helpers: combined a number of steps in one function

   /**
    * Perform following steps:
    *
    * 1: go to My Workspaces page
    * 2: click Create New Workspace link
    * 3: wait until Edit page is loaded and ready
    * 4: return
    */
  async clickCreateNewWorkspace(): Promise<WorkspaceEditPage> {
    await this.goToUrl();
    const link = await this.getCreateNewWorkspaceLink();
    await Promise.all([
      this.page.waitForNavigation( { waitUntil: ['domcontentloaded','networkidle0']} ),
      link.click()
    ]);
    const workspaceEdit = new WorkspaceEditPage(this.page);
    await workspaceEdit.waitForReady();
    return workspaceEdit;
  }

  /**
   * Create a simple and basic new workspace end-to-end.
   */
  async createWorkspace(workspaceName: string, billingAccount: string) {

    const workspaceEditPage = await this.clickCreateNewWorkspace();
    // wait for Billing Account default selected value
    await workspaceEditPage.waitForTextExists('Use All of Us free credits');

    await (await workspaceEditPage.getWorkspaceNameTextbox()).type(workspaceName);
    await (await workspaceEditPage.getWorkspaceNameTextbox()).pressKeyboard('Tab', { delay: 100 });

    const dataSetSelect = new select(this.page);
    await dataSetSelect.withLabel({text: EditPageLabel.SYNTHETIC_DATASET});
    await dataSetSelect.selectOption('2');

    const billingAccountSelect = new select(this.page);
    await billingAccountSelect.withLabel({text: EditPageLabel.SELECT_BILLING});
    await billingAccountSelect.selectOption(billingAccount);

    // expand Disease purpose section
    await workspaceEditPage.expandResearchPurposeSection();

    // Enter value in 'Disease-focused research'
    const diseaseName = workspaceEditPage.question1_diseaseFocusedResearch();
    await (await diseaseName.asLabel()).click(); // click on label to toggle checkbox
    await (await diseaseName.asTextBox()).type('diabetes');
    await (await diseaseName.asTextBox()).pressKeyboard('Tab', { delay: 100 });

    const educationPurpose = workspaceEditPage.question1_educationalPurpose();
    await (await educationPurpose.asCheckBox()).check();

    const forProfitPurpose = workspaceEditPage.question1_forProfitPurpose();
    await (await forProfitPurpose.asCheckBox()).check();

    const otherPurpose = workspaceEditPage.question1_otherPurpose();
    await (await otherPurpose.asCheckBox()).check(); // enables textarea
    await (await otherPurpose.asTextArea()).type(faker.lorem.word());

    // 2. Please provide a summary of your research purpose by responding to the questions below.
    const scientificQuestions = workspaceEditPage.question2_scientificQuestionsIntendToStudy();
    await (await scientificQuestions.asTextArea()).type(faker.lorem.word());

    const scientificApproaches = workspaceEditPage.question2_scientificApproaches();
    await (await scientificApproaches.asTextArea()).type(faker.lorem.word());

    const anticipatedFindings = workspaceEditPage.question2_anticipatedFindings();
    await (await anticipatedFindings.asTextArea()).type(faker.lorem.word());

    // 3. The All of Us Research Program encourages researchers to disseminate ....
    const publicationInPeerReviewedJournal = workspaceEditPage.publicationInJournal();
    await (await publicationInPeerReviewedJournal.asCheckBox()).check();

    // 4. The All of Us Research Program would like to understand how ....
    const increaseWellness = workspaceEditPage.increaseWellnessResilience();
    await (await increaseWellness.asCheckBox()).check();

    // 5. Population of interest: use default values
    // Leave alone default values

    // 6. Request for Review of Research Purpose Description
    // Leave alone default values
    const noRadiobutton = workspaceEditPage.noConcernAboutStigmatization();
    await (await noRadiobutton.asRadioButton()).select();

    // CREATE WORKSPACE button
    const createButton = await workspaceEditPage.getCreateWorkspaceButton();
    await createButton.focus(); // bring into viewport
    await createButton.click();
    await workspaceEditPage.waitForSpinner();
  }

}
