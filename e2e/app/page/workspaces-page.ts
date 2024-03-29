import { Page } from 'puppeteer';
import Button from 'app/element/button';
import { AccessTierDisplayNames, LinkText, PageUrl, Tabs } from 'app/text-labels';
import WorkspaceEditPage, { FIELD as EDIT_FIELD } from 'app/page/workspace-edit-page';
import RadioButton from 'app/element/radiobutton';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import ReactSelect from 'app/element/react-select';
import WorkspaceDataPage from './workspace-data-page';
import { config } from 'resources/workbench-config';
import { UseFreeCredits } from './workspace-base';
import OldCdrVersionModal from 'app/modal/old-cdr-version-modal';
import AuthenticatedPage from './authenticated-page';
import { logger } from 'libs/logger';
import WorkspaceAboutPage from './workspace-about-page';
import WorkspaceReviewResearchPurposeModal from 'app/modal/workspace-review-research-purpose-modal';
import WorkspaceCard from 'app/component/card/workspace-card';
import { openTab } from 'utils/test-utils';

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
    await waitWhileLoading(this.page).catch(async () => {
      logger.warn('Retry loading Workspaces page');
      await this.page.reload({ waitUntil: ['networkidle0', 'load'] });
      await waitWhileLoading(this.page);
    });
    await this.page.waitForXPath('//a[text()="Workspaces"]', { visible: true });
    await this.page.waitForSelector('[data-test-id="workspace-card"], [role="button"]', {
      visible: true
    });
    return true;
  }

  /**
   * Load 'Your Workspaces' page and ensure page load is completed.
   */
  async load(): Promise<this> {
    const title = await this.page.title();
    if (!title.includes(PageTitle)) {
      await this.loadPage({ url: PageUrl.Workspaces });
    }
    await this.waitForLoad();
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
    await link.clickAndWait({ timeout: 3 * 60 * 1000 });
    const workspaceEdit = new WorkspaceEditPage(this.page);
    await workspaceEdit.waitForLoad();
    return workspaceEdit;
  }

  /**
   * Create a simple and basic new workspace end-to-end.
   */
  async createWorkspace(
    workspaceName: string,
    opts: {
      dataAccessTier?: string;
      cdrVersionName?: string;
      billingAccount?: string;
      reviewRequest?: boolean;
    } = {}
  ): Promise<string[]> {
    const {
      dataAccessTier = AccessTierDisplayNames.Registered,
      cdrVersionName = config.DEFAULT_CDR_VERSION_NAME,
      billingAccount = UseFreeCredits,
      reviewRequest = false
    } = opts;

    const createPage = await this.fillOutRequiredCreationFields(workspaceName, billingAccount, reviewRequest);

    const cdrVersionSelect = createPage.getCdrVersionSelect();
    expect(await cdrVersionSelect.getSelectedValue()).toBe(config.DEFAULT_CDR_VERSION_NAME);

    // select Data access tier
    await createPage.selectAccessTier(dataAccessTier);

    if (dataAccessTier === AccessTierDisplayNames.Controlled) {
      // observe that the CDR Version default has changed automatically
      expect(await cdrVersionSelect.getSelectedValue()).not.toBe(config.DEFAULT_CDR_VERSION_NAME);
    }

    // select the chosen CDR Version
    await createPage.selectCdrVersion(cdrVersionName);

    // if the CDR Version is not the default, consent to the necessary restrictions
    // cannot create a workspace with an old CDR Version without consenting to the restrictions.
    if (cdrVersionName === config.OLD_CDR_VERSION_NAME) {
      const modal = new OldCdrVersionModal(this.page);
      await modal.waitForLoad();
      await modal.consentToOldCdrRestrictions();
    }

    // click CREATE WORKSPACE button
    const createButton = createPage.getCreateWorkspaceButton();
    await createButton.waitUntilEnabled();
    const modalContent = await createPage.clickCreateFinishButton(createButton);

    await new WorkspaceDataPage(this.page).waitForLoad();
    logger.info(
      `Created workspace "${workspaceName}" with CDR Version "${cdrVersionName}"` +
        ` and Data Access Tier "${dataAccessTier}"`
    );
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
    await selectBilling.selectOptionByValuePrefix(billingAccount);

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

  async filterByAccessLevel(level: string): Promise<string> {
    const selectMenu = new ReactSelect(this.page, { name: 'Filter by' });
    await selectMenu.selectOption(level);
    await waitWhileLoading(this.page);
    return selectMenu.getSelectedOption();
  }

  async openAboutPage(workspaceCard: WorkspaceCard): Promise<WorkspaceAboutPage> {
    await workspaceCard.clickName();
    const aboutPage = new WorkspaceAboutPage(this.page);
    // Older workspace requires Review Research Purpose
    const reviewPurposeModal = new WorkspaceReviewResearchPurposeModal(this.page);
    // Wait maximum 2 seconds to find Review Purpose modal.
    const modalVisible = await reviewPurposeModal.isVisible(2000);
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
      await openTab(this.page, Tabs.About, aboutPage);
    }
    await aboutPage.waitForLoad();
    return aboutPage;
  }
}
