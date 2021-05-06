import { Page } from 'puppeteer';
import TieredMenu from 'app/component/tiered-menu';
import Button from 'app/element/button';
import ClrIconLink from 'app/element/clr-icon-link';
import { ElementType } from 'app/xpath-options';
import { makeRandomName } from 'utils/str-utils';
import { waitForDocumentTitle, waitForNumericalString, waitForText, waitWhileLoading } from 'utils/waits-utils';
import { buildXPath } from 'app/xpath-builders';
import { LinkText, MenuOption } from 'app/text-labels';
import Modal from 'app/modal/modal';
import AuthenticatedPage from './authenticated-page';
import CohortParticipantsGroup from './cohort-participants-group';

const faker = require('faker/locale/en_US');
const PageTitle = 'Build Cohort Criteria';

export enum FieldSelector {
  TotalCount = '//*[contains(normalize-space(text()), "Total Count")]/parent::*//span',
  GroupCount = '//*[contains(normalize-space(text()), "Group Count")]/parent::*//span'
}

export default class CohortBuildPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle), waitWhileLoading(this.page)]);
    return true;
  }

  /**
   * Save Cohort changes.
   */
  async saveChanges(): Promise<void> {
    const createCohortButton = Button.findByName(this.page, { normalizeSpace: LinkText.SaveCohort });
    await createCohortButton.waitUntilEnabled();
    await createCohortButton.click(); // Click dropdown trigger to open menu
    const menu = new TieredMenu(this.page);
    await menu.select(MenuOption.Save);
  }

  async saveCohortAs(cohortName?: string, description?: string): Promise<string> {
    const createCohortButton = this.getCreateCohortButton();
    await createCohortButton.waitUntilEnabled();
    await createCohortButton.click();

    if (cohortName === undefined) {
      cohortName = makeRandomName();
    }
    if (description === undefined) {
      description = faker.lorem.words(10);
    }

    const modal = new Modal(this.page);
    await modal.waitForLoad();
    const nameTextbox = modal.waitForTextbox('COHORT NAME');
    await nameTextbox.type(cohortName);

    const descriptionTextarea = modal.waitForTextarea('DESCRIPTION');
    await descriptionTextarea.type(description);

    await modal.clickButton(LinkText.Save, { waitForClose: true, timeout: 2 * 60 * 1000 });
    await waitWhileLoading(this.page);

    await waitForText(this.page, 'Cohort Saved Successfully');
    console.log(`Created Cohort: "${cohortName}"`);
    return cohortName;
  }

  async deleteCohort(): Promise<string[]> {
    await this.getDeleteButton().click();
    return this.deleteConfirmationDialog();
  }

  /**
   * Click DELETE COHORT button in Cohort Delete Confirmation dialog.
   * @return {string} Dialog textContent.
   */
  async deleteConfirmationDialog(): Promise<string[]> {
    const modal = new Modal(this.page);
    await modal.waitForLoad();
    const contentText = await modal.getTextContent();
    await modal.clickButton(LinkText.DeleteCohort, { waitForClose: true });
    await waitWhileLoading(this.page);
    console.log(`Delete Confirmation modal:\n${contentText}`);
    return contentText;
  }

  /**
   * Click DISCARD CHANGES button in Confirmation dialog.
   * @return {string} Dialog textContent.
   */
  async discardChangesConfirmationDialog(): Promise<string[]> {
    const modal = new Modal(this.page);
    await modal.waitForLoad();
    const contentText = await modal.getTextContent();
    await modal.clickButton(LinkText.DiscardChanges, { waitForNav: true, waitForClose: true });
    await waitWhileLoading(this.page);
    return contentText;
  }

  /**
   * Find the Cohort Total Count.
   * This function also can be used to wait until participants calculation has completed.
   * @return {string} Total Count.
   */
  async getTotalCount(timeout = 60000): Promise<string> {
    const highCharts = '//*[@class="highcharts-container "]//*[contains(@class, "highcharts-bar-series")]/*';
    const [count] = await Promise.all([
      waitForNumericalString(this.page, FieldSelector.TotalCount, timeout),
      this.page.waitForXPath(highCharts, { timeout, visible: true })
    ]);
    return count;
  }

  getSaveCohortButton(): Button {
    const xpath = buildXPath({ type: ElementType.Button, normalizeSpace: LinkText.SaveCohort });
    return new Button(this.page, xpath);
  }

  getCreateCohortButton(): Button {
    const xpath = buildXPath({ type: ElementType.Button, normalizeSpace: LinkText.CreateCohort });
    return new Button(this.page, xpath);
  }

  /**
   * Find DELETE (trash icon) button in Cohort Build page.
   * @return {ClrIconLink}
   */
  getDeleteButton(): ClrIconLink {
    return ClrIconLink.findByName(this.page, { iconShape: 'trash' });
  }

  /**
   * Find EXPORT button in Cohort Build page.
   */
  getExportButton(): ClrIconLink {
    return ClrIconLink.findByName(this.page, { iconShape: 'export' });
  }

  /**
   * Find COPY button in Cohort Build page.
   */
  getCopyButton(): ClrIconLink {
    return ClrIconLink.findByName(this.page, { iconShape: 'copy' });
  }

  /**
   * Include Participants Group.
   * @param groupName
   */
  findIncludeParticipantsGroup(groupName: string): CohortParticipantsGroup {
    const group = new CohortParticipantsGroup(this.page);
    group.setXpath(`//*[@id="list-include-groups"][.//*[normalize-space()="${groupName}"]]`);
    return group;
  }

  findExcludeParticipantsGroup(groupName: string): CohortParticipantsGroup {
    const group = new CohortParticipantsGroup(this.page);
    group.setXpath(
      `//*[@id="list-exclude-groups"]/*[@data-test-id="excludes-search-group"][.//*[normalize-space()="${groupName}"]]`
    );
    return group;
  }

  findIncludeParticipantsEmptyGroup(): CohortParticipantsGroup {
    const group = new CohortParticipantsGroup(this.page);
    group.setXpath(
      '//*[@id="list-include-groups"]' +
        '/*[./*[not(@data-test-id="includes-search-group") and normalize-space()="Add Criteria"]]'
    );
    return group;
  }

  // Find Include Participants Group: Group 1 is index 1, Group 2 is index 2, etc.
  findIncludeParticipantsGroupByIndex(index = 1): CohortParticipantsGroup {
    const group = new CohortParticipantsGroup(this.page);
    group.setXpath(`//*[@id="list-include-groups"]//div[./*[@data-test-id="item-list"]][${index}]`);
    return group;
  }
}
// data-test-id="includes-search-group"
