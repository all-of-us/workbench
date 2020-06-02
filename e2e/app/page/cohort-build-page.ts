import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle, waitForNumericalString} from 'utils/waits-utils';
import {makeRandomName} from 'utils/str-utils';
import Dialog from 'app/component/dialog';
import Button from 'app/element/button';
import {ElementType} from 'app/xpath-options';
import ClrIconLink from 'app/element/clr-icon-link';
import AuthenticatedPage from './authenticated-page';
import CohortParticipantsGroup from './cohort-participants-group';
const faker = require('faker/locale/en_US');

const PageTitle = 'Build Cohort Criteria';

export enum FieldSelector {
   TotalCount = '//*[contains(normalize-space(text()), "Total Count")]/parent::*//span',
   GroupCount = '//*[contains(normalize-space(text()), "Group Count")]/parent::*//span',
}

export default class CohortBuildPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (e) {
      console.log(`CohortBuildPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  async saveCohortAs(cohortName?: string, description?: string): Promise<string> {
    const createCohortButton = await Button.findByName(this.page, {name: 'Create Cohort'});
    await createCohortButton.waitUntilEnabled();
    await createCohortButton.click();

    if (cohortName === undefined) {
      cohortName = makeRandomName();
    }
    if (description === undefined) {
      description = faker.lorem.words(10);
    }

    const dialog = new Dialog(this.page);
    const nameTextbox = await dialog.waitForTextbox('COHORT NAME');
    await nameTextbox.type(cohortName);

    const descriptionTextarea = await dialog.waitForTextarea('DESCRIPTION');
    await descriptionTextarea.type(description);

    const saveButton = await Button.findByName(this.page, {name: 'Save'});
    await saveButton.waitUntilEnabled();
    await saveButton.click();
    await dialog.waitUntilDialogIsClosed();
    await waitWhileLoading(this.page);

    return cohortName;
  }

  async deleteCohort(): Promise<string> {
    await this.getDeleteButton().then(b => b.click());
    return this.deleteConfirmationDialog();
  }

  /**
   * Click DELETE COHORT button in Cohort Delete Confirmation dialog.
   * @return {string} Dialog textContent.
   */
  async deleteConfirmationDialog(): Promise<string> {
    const dialog = new Dialog(this.page);
    const contentText = await dialog.getContent();
    const deleteButton = await Button.findByName(this.page, {type: ElementType.Button, normalizeSpace: 'Delete Cohort'}, dialog);
    await Promise.all([
      deleteButton.click(),
      dialog.waitUntilDialogIsClosed(),
    ]);
    await waitWhileLoading(this.page);
    return contentText;
  }

  /**
   * Click DISCARD CHANGES button in Confirmation dialog.
   * @return {string} Dialog textContent.
   */
  async discardChangesConfirmationDialog(): Promise<string> {
    const dialog = new Dialog(this.page);
    const contentText = await dialog.getContent();
    const deleteButton = await Button.findByName(this.page, {type: ElementType.Button, normalizeSpace: 'Discard Changes'}, dialog);
    await Promise.all([
      deleteButton.click(),
      dialog.waitUntilDialogIsClosed(),
      this.page.waitForNavigation({waitUntil: ['domcontentloaded', 'networkidle0'], timeout: 60000}),
    ]);
    await waitWhileLoading(this.page);
    return contentText;
  }

  /**
   * Find the Cohort Total Count.
   * @return {string} Total Count string.
   */
  async getTotalCount(): Promise<string> {
    return waitForNumericalString(this.page, FieldSelector.TotalCount);
  }

  /**
   * Find DELETE (trash icon) button in Cohort Build page.
   * @return {ClrIconLink}
   */
  async getDeleteButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {iconShape: 'trash'});
  }

  /**
   * Find EXPORT button in Cohort Build page.
   */
  async getExportButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {iconShape: 'export'});
  }

  /**
   * Find COPY button in Cohort Build page.
   */
  async getCopyButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {iconShape: 'copy'});
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
