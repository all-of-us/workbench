import { Page } from 'puppeteer';
import Button from 'app/element/button';
import { Language, LinkText, PageUrl } from 'app/text-labels';
import WorkspaceEditPage, { FIELD as EDIT_FIELD } from 'app/page/workspace-edit-page';
import RadioButton from 'app/element/radiobutton';
import { findOrCreateWorkspace } from 'utils/test-utils';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import ReactSelect from 'app/element/react-select';
import WorkspaceDataPage from './workspace-data-page';
import WorkspaceAnalysisPage from './workspace-analysis-page';
import { config } from 'resources/workbench-config';
import { UseFreeCredits } from './workspace-base';
import OldCdrVersionModal from 'app/modal/old-cdr-version-modal';
import AuthenticatedPage from './authenticated-page';
import { logger } from 'libs/logger';
import WorkspaceAboutPage from './workspace-about-page';
import WorkspaceReviewResearchPurposeModal from 'app/modal/workspace-review-research-purpose-modal';
import WorkspaceCard from 'app/component/workspace-card';
const faker = require('faker/locale/en_US');

export const PageTitle = 'View Workspace';

export const FieldSelector = {
  CreateNewWorkspaceButton: {
    textOption: {
      normalizeSpace: LinkText.CreateNewWorkspace
    }
  }
};

export default class WorkspacesPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitForDocumentTitle(this.page, PageTitle);

    await waitWhileLoading(this.page, 120000).catch(async () => {
      logger.warn('Retry loading Workspaces page');
      await this.page.reload({ waitUntil: ['networkidle0', 'load'] });
      await waitWhileLoading(this.page);
    });

    await Promise.all([
      this.page.waitForXPath('//a[text()="Workspaces"]', { visible: true }),
      this.page.waitForXPath('//h3[normalize-space(text())="Workspaces"]', { visible: true }) // Texts above Filter By Select
    ]);
    return true;
  }

  /**
   * Load 'Your Workspaces' page and ensure page load is completed.
   */
  async load(): Promise<this> {
    await this.loadPageUrl(PageUrl.Workspaces);
    await waitWhileLoading(this.page);
    return this;
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
    const link = Button.findByName(this.page, FieldSelector.CreateNewWorkspaceButton.textOption);
    await link.clickAndWait();
    const workspaceEdit = new WorkspaceEditPage(this.page);
    await workspaceEdit.waitForLoad();
    return workspaceEdit;
  }

  /**
   * Create a simple and basic new workspace end-to-end.
   */
  async createWorkspace(
    workspaceName: string,
    cdrVersionName: string = config.DEFAULT_CDR_VERSION_NAME,
    billingAccount: string = UseFreeCredits,
    reviewRequest = false
  ): Promise<string[]> {
    const editPage = await this.fillOutRequiredCreationFields(workspaceName, billingAccount, reviewRequest);

    // select the chosen CDR Version
    await editPage.selectCdrVersion(cdrVersionName);

    // if the CDR Version is not the default, consent to the necessary restrictions
    if (cdrVersionName !== config.DEFAULT_CDR_VERSION_NAME) {
      const modal = new OldCdrVersionModal(this.page);
      await modal.waitForLoad();
      await modal.consentToOldCdrRestrictions();
    }

    // click CREATE WORKSPACE button
    const createButton = editPage.getCreateWorkspaceButton();
    await createButton.waitUntilEnabled();
    const modalContent = await editPage.clickCreateFinishButton(createButton);
    logger.info(`Created workspace "${workspaceName}" with CDR Version "${cdrVersionName}"`);
    return modalContent;
  }

  async fillOutRequiredCreationFields(
    workspaceName: string,
    billingAccount: string = UseFreeCredits,
    reviewRequest = false
  ): Promise<WorkspaceEditPage> {
    const editPage = await this.clickCreateNewWorkspace();
    // wait for Billing Account default selected value to appear
    const selectBilling = editPage.getBillingAccountSelect();
    await selectBilling.waitForSelectedValue(UseFreeCredits, 60000);

    await editPage.getWorkspaceNameTextbox().type(workspaceName);
    await editPage.getWorkspaceNameTextbox().pressTab();

    // select Billing Account
    await editPage.selectBillingAccount(billingAccount);

    // 1. What is the primary purpose of your project?
    // check Educational Purpose checkbox
    const educationPurpose = editPage.question1_educationalPurpose();
    await educationPurpose.asCheckBox().check();

    // 2. Please provide a summary of your research purpose by responding to the questions below.
    const scientificQuestions = editPage.question2_scientificQuestionsIntendToStudy();
    await scientificQuestions.asTextArea().paste(faker.lorem.paragraph());

    const scientificApproaches = editPage.question2_scientificApproaches();
    await scientificApproaches.asTextArea().paste(faker.lorem.paragraph());

    const anticipatedFindings = editPage.question2_anticipatedFindings();
    await anticipatedFindings.asTextArea().paste(faker.lorem.paragraph());

    // 3. The All of Us Research Program encourages researchers to disseminate ....
    const publicationInJournal = editPage.publicationInJournal();
    await publicationInJournal.asCheckBox().check();

    // 4. The All of Us Research Program would like to understand how ....
    const increaseWellness = editPage.increaseWellnessResilience();
    await increaseWellness.asCheckBox().check();

    // 5. Population of interest: use default values. Using default value
    const noRadiobutton = RadioButton.findByName(this.page, EDIT_FIELD.POPULATION_OF_INTEREST.noRadiobutton.textOption);
    await noRadiobutton.select();

    // 6. Request for Review of Research Purpose Description. Using default value
    await editPage.requestForReviewRadiobutton(reviewRequest);

    return editPage;
  }

  /**
   * Create a new notebook in a selected Workspace.
   * @param {string} workspaceName Workspace name.
   * @param {string} notebookName Notebook name
   * @param {Language} lang Notebook language.
   */
  async createNotebook(opts: {
    workspaceName: string;
    notebookName: string;
    lang?: Language;
  }): Promise<WorkspaceAnalysisPage> {
    const { workspaceName, notebookName, lang } = opts;
    await findOrCreateWorkspace(this.page, { workspaceName });

    const dataPage = new WorkspaceDataPage(this.page);
    const notebookPage = await dataPage.createNotebook(notebookName, lang); // Python 3 is the default

    // Do not run any code. Simply returns to the Workspace Analysis tab.
    return notebookPage.goAnalysisPage();
  }

  async filterByAccessLevel(level: string): Promise<string> {
    const selectMenu = new ReactSelect(this.page, { name: 'Filter by' });
    await selectMenu.selectOption(level);
    await waitWhileLoading(this.page);
    return selectMenu.getSelectedOption();
  }

  async openAboutPage(workspaceCard: WorkspaceCard): Promise<WorkspaceAboutPage> {
    await workspaceCard.clickWorkspaceName(false);
    const aboutPage = new WorkspaceAboutPage(this.page);
    // Older workspace requires Review Research Purpose
    const reviewPurposeModal = new WorkspaceReviewResearchPurposeModal(this.page);
    // Wait maximum 3 seconds to find Review Purpose modal.
    const modalVisible = await reviewPurposeModal.isVisible(3000);
    if (modalVisible) {
      await reviewPurposeModal.clickReviewNowButton();
      await aboutPage.waitForLoad();
      // Click "Looks Good" link
      await this.page
        .waitForXPath('//a[text()="Looks Good"]', { visible: true, timeout: 2000 })
        .then((link) => link.click())
        .catch(() => {
          // READER doesn't have permission to review research purpose. "Looks Good" link is not available to click.
          // Ignore timeout error thrown by waitForXpath
        });
    } else {
      await new WorkspaceDataPage(this.page).openAboutPage();
    }
    await aboutPage.waitForLoad();
    return aboutPage;
  }
}
