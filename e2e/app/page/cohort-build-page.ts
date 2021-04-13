import { Page } from 'puppeteer';
import TieredMenu from 'app/component/tiered-menu';
import Button from 'app/element/button';
import ClrIconLink from 'app/element/clr-icon-link';
import { ElementType } from 'app/xpath-options';
import { makeRandomName } from 'utils/str-utils';
import { waitForDocumentTitle, waitForNumericalString, waitWhileLoading } from 'utils/waits-utils';
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
    const createCohortButton = await Button.findByName(this.page, { normalizeSpace: LinkText.SaveCohort });
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
    const nameTextbox = await modal.waitForTextbox('COHORT NAME');
    await nameTextbox.type(cohortName);

    const descriptionTextarea = await modal.waitForTextarea('DESCRIPTION');
    await descriptionTextarea.type(description);

    await modal.clickButton(LinkText.Save, { waitForClose: true, timeout: 2 * 60 * 1000 });
    await waitWhileLoading(this.page);

    return cohortName;
  }

  async deleteCohort(): Promise<string[]> {
    await this.getDeleteButton().then((b) => b.click());
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
  async getTotalCount(): Promise<string> {
    return waitForNumericalString(this.page, FieldSelector.TotalCount, 60000);
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
  async getDeleteButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, { iconShape: 'trash' });
  }

  /**
   * Find EXPORT button in Cohort Build page.
   */
  async getExportButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, { iconShape: 'export' });
  }

  /**
   * Find COPY button in Cohort Build page.
   */
  async getCopyButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, { iconShape: 'copy' });
  }

  /**
   * Include Participants Group.
   * @param groupName
   */
  findIncludeParticipantsGroup(groupName: string): CohortParticipantsGroup {
    const group = new CohortParticipantsGroup(this.page);
    group.setXpath(`//*[@id="list-include-groups"]//*[normalize-space()="${groupName}"]`);
    return group;
  }

  findExcludeParticipantsGroup(groupName: string): CohortParticipantsGroup {
    const group = new CohortParticipantsGroup(this.page);
    group.setXpath(`//*[@id="list-exclude-groups"]//*[normalize-space()="${groupName}"]`);
    return group;
  }
}
