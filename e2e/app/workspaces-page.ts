import {Page} from 'puppeteer';
import {findButton} from './aou-elements/xpath-finder';
import {PageUrl} from './authenticated-page';
import WorkspaceEditPage from './workspace-edit-page';

const faker = require('faker/locale/en_US');

export const PAGE = {
  TITLE: 'View Workspace',
};

export const FIELD_LABEL = {
  CREATE_A_NEW_WORKSPACE: 'Create a New Workspace',
};

export const FIELD_FINDER = {
  CREATE_A_NEW_WORKSPACE_BUTTON: {
    textOption: {
      normalizeSpace: FIELD_LABEL.CREATE_A_NEW_WORKSPACE
    }
  }
};


export default class WorkspacesPage extends WorkspaceEditPage {

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
    await this.loadPageUrl(PageUrl.WORKSPACES);
    return this;
  }

  async getCreateNewWorkspaceButton() {
    return findButton(this.page, {normalizeSpace: FIELD_LABEL.CREATE_A_NEW_WORKSPACE}, {visible: true});
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
  async clickCreateNewWorkspace(): Promise<WorkspacesPage> {
    const link = await this.getCreateNewWorkspaceButton();
    await this.clickAndWait(link);
    const workspaceEdit = new WorkspaceEditPage(this.page);
    await workspaceEdit.waitForLoad();
    return this;
  }

   /**
    * Create a simple and basic new workspace end-to-end.
    */
  async createWorkspace(workspaceName: string, billingAccount: string) {

    await this.clickCreateNewWorkspace();
      // wait for Billing Account default selected value
    await this.waitForTextExists('Use All of Us free credits');

    await (await this.getWorkspaceNameTextbox()).type(workspaceName);
    await (await this.getWorkspaceNameTextbox()).pressKeyboard('Tab', {delay: 100});

      // select Synthetic Data Set 2
    await this.selectDataSet('2');

      // select Billing Account
    await this.selectBillingAccount(billingAccount);

      // expand Disease purpose section
    await this.expandResearchPurposeGroup(true);

      // Enter value in 'Disease-focused research'
    await this.fillOutDiseaseFocusedResearch();

      // check Educational Purpose checkbox
    const educationPurpose = this.question1_educationalPurpose();
    await (await educationPurpose.asCheckBox()).check();

      // check For-Profit Purpose checkbox
    const forProfitPurpose = this.question1_forProfitPurpose();
    await (await forProfitPurpose.asCheckBox()).check();

      // check Other-Purpose checkbox
    await this.fillOutOtherPurpose();

      // 2. Please provide a summary of your research purpose by responding to the questions below.
    const scientificQuestions = this.question2_scientificQuestionsIntendToStudy();
    await (await scientificQuestions.asTextArea()).type(faker.lorem.word());

    const scientificApproaches = this.question2_scientificApproaches();
    await (await scientificApproaches.asTextArea()).type(faker.lorem.word());

    const anticipatedFindings = this.question2_anticipatedFindings();
    await (await anticipatedFindings.asTextArea()).type(faker.lorem.word());

      // 3. The All of Us Research Program encourages researchers to disseminate ....
    const publicationInJournal = this.publicationInJournal();
    await (await publicationInJournal.asCheckBox()).check();

      // 4. The All of Us Research Program would like to understand how ....
    const increaseWellness = this.increaseWellnessResilience();
    await (await increaseWellness.asCheckBox()).check();

      // 5. Population of interest: use default values. Using default value

      // 6. Request for Review of Research Purpose Description. Using default value
    await this.requestForReviewRadiobutton(false);

      // click CREATE WORKSPACE button
    await this.clickCreateFinishButton();
  }

   /**
    * Type in new workspace name.
    * @return {string} new workspace name
    */
  async fillOutWorkspaceName(): Promise<string> {
    const newWorkspaceName = `aoutest-${Math.floor(Math.random() * 1000)}-${Math.floor(Date.now() / 1000)}`;
    await (await this.getWorkspaceNameTextbox()).type(newWorkspaceName);
    await (await this.getWorkspaceNameTextbox()).tabKey();
    return newWorkspaceName;
  }


}
