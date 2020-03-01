import {Page} from 'puppeteer';
import {waitUntilFindTexts, waitUntilTitleMatch} from '../driver/waitFuncs';
import Link from './aou-elements/Link';
import select from './aou-elements/Select';
import {findButton} from './aou-elements/xpath-finder';
import DataPage from './DataPage';
import AuthenticatedPage from './page-mixin/AuthenticatedPage';
import WorkspaceEditPage, {FIELD_LABEL as EditPageFieldLabel} from './WorkspaceEditPage';
const configs = require('../resources/workbench-config.js');
const faker = require('faker/locale/en_US');

export const FIELD_LABEL = {
  TITLE: 'View Workspaces',
  CREATE_NEW_WORKSPACE: 'Create a New Workspace',
};

export default class WorkspacesPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

   /**
    * navigate to My Workspaces URL
    */
  public async navigateToURL(): Promise<void> {
    const pageUrl = configs.uiBaseUrl + configs.workspacesUrlPath;
    await this.puppeteerPage.goto(pageUrl, {waitUntil: ['domcontentloaded','networkidle0']});
    await this.waitForSpinner();
  }

  public async isLoaded(): Promise<boolean> {
    await waitUntilTitleMatch(this.puppeteerPage, FIELD_LABEL.TITLE);
    await this.puppeteerPage.waitForXPath('//h3[normalize-space(text())="Workspaces"]', {visible: true});
    await this.waitForSpinner();
    return true;
  }

  public async waitForReady(): Promise<WorkspacesPage> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  public async getCreateNewWorkspaceLink() {
    return findButton(this.puppeteerPage, {normalizeSpace: FIELD_LABEL.CREATE_NEW_WORKSPACE});
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
  public async clickCreateNewWorkspace(): Promise<WorkspaceEditPage> {
    await this.navigateToURL();
    const link = await this.getCreateNewWorkspaceLink();
    await Promise.all([
      this.puppeteerPage.waitForNavigation( { waitUntil: ['domcontentloaded','networkidle0']} ),
      link.click()
    ]);
    const workspaceEdit = new WorkspaceEditPage(this.puppeteerPage);
    await workspaceEdit.waitForReady();
    return workspaceEdit;
  }


  /**
   * Create a simple and basic new workspace end-to-end.
   */
  public async createWorkspace(workspaceName: string, billingAccount: string) {

    const workspaceEditPage = await this.clickCreateNewWorkspace();
    // wait for Billing Account default selected value
    await waitUntilFindTexts(this.puppeteerPage, 'Use All of Us free credits');

    await (await workspaceEditPage.getWorkspaceNameTextbox()).type(workspaceName);
    await (await workspaceEditPage.getWorkspaceNameTextbox()).pressKeyboard('Tab', { delay: 100 });

    const dataSetSelect = new select(this.puppeteerPage);
    await dataSetSelect.withLabel({text: EditPageFieldLabel.SYNTHETIC_DATASET});
    await dataSetSelect.selectOption('2');

    const billingAccountSelect = new select(this.puppeteerPage);
    await billingAccountSelect.withLabel({text: EditPageFieldLabel.SELECT_BILLING});
    await billingAccountSelect.selectOption(billingAccount);

    // expand Disease purpose section
    await workspaceEditPage.expandResearchPurposeSection();
    console.log('exxpand resreach pupose');

    // Enter value in 'Disease-focused research'
    const diseaseName = workspaceEditPage.question1_diseaseFocusedResearch();
    await (await diseaseName.asLabel()).click(); // click on label to toggle checkbox
    await (await diseaseName.asTextbox()).type('diabetes');
    await (await diseaseName.asTextbox()).pressKeyboard('Tab', { delay: 100 });

    const educationPurpose = workspaceEditPage.question1_educationalPurpose();
    await (await educationPurpose.asCheckbox()).check();

    const forProfitPurpose = workspaceEditPage.question1_forProfitPurpose();
    console.log('for-profit purpose checkbox begins');
    await (await forProfitPurpose.asCheckbox()).check();

    const otherPurpose = workspaceEditPage.question1_otherPurpose();
    await (await otherPurpose.asCheckbox()).check(); // enables textarea
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
    await (await publicationInPeerReviewedJournal.asCheckbox()).check();

    // 4. The All of Us Research Program would like to understand how ....
    const increaseWellness = workspaceEditPage.increaseWellnessResilience();
    await (await increaseWellness.asCheckbox()).check();

    // 5. Population of interest: use default values
    // Leave alone default values

    // 6. Request for Review of Research Purpose Description
    // Leave alone default values

    // CREATE WORKSPACE button
    const createButton = await workspaceEditPage.getCreateWorkspaceButton();
    await createButton.focus(); // bring into viewport
    await createButton.click();
    await this.puppeteerPage.waitFor(2000);
    await (new DataPage(this.puppeteerPage)).waitUntilPageReady();
    await new Link(this.puppeteerPage).withXpath(`//a[.='${workspaceName}' and @href]`, {visible: true})
  }


}
