import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle, waitForNumericalString} from 'utils/waits-utils';
import {makeRandomName} from 'utils/str-utils';
import CreateCriteriaModal, {FilterSign, PhysicalMeasurementsCriteria} from 'app/component/create-criteria-modal';
import Dialog from 'app/component/dialog';
import TieredMenu from 'app/component/tiered-menu';
import Button from 'app/element/button';
import {ElementType} from 'app/xpath-options';
import ClrIconLink from '../element/clr-icon-link';
import AuthenticatedPage from './authenticated-page';
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

  async includePhysicalMeasurement(groupNum: number, criteriaName: PhysicalMeasurementsCriteria, value: number): Promise<string> {
    await this.selectMenuCriteria(['Physical Measurements'], groupNum);
    const modal = new CreateCriteriaModal(this.page);
    await modal.waitUntilVisible();
    return modal.filterPhysicalMeasurementValue(criteriaName, FilterSign.GreaterThanOrEqualTo, value);
  }

  async includeDemographicsDeceased(groupNum: number): Promise<string> {
    await this.selectMenuCriteria(['Demographics', 'Deceased'], groupNum);
    return waitForNumericalString(this.page, this.getIncludedGroupCountXpath(groupNum));
  }

  async selectMenuCriteria(menuItemLinks: string[], groupNum: number = 1) {
    const menu = await this.openTieredMenu(groupNum);
    await menu.clickMenuItem(menuItemLinks);
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
    const trashIcon = await ClrIconLink.findByName(this.page, {iconShape: 'trash'});
    await trashIcon.click();
    return this.deleteCohortConfirmationDialog();
  }

  /**
   * Confirm delete cohort.
   */
  async deleteCohortConfirmationDialog(): Promise<string> {
    const dialog = new Dialog(this.page);
    const contentText = await dialog.getContent();
    const deleteButton = await Button.findByName(this.page, {type: ElementType.Button, normalizeSpace: 'Delete Cohort'}, dialog);
    await Promise.all([
      deleteButton.click(),
      dialog.waitUntilDialogIsClosed(),
      this.page.waitForNavigation({waitUntil: ['domcontentloaded', 'networkidle0'], timeout: 60000}),
    ]);
    await waitWhileLoading(this.page);
    return contentText;
  }

  async getTotalCount(): Promise<string> {
    return waitForNumericalString(this.page, FieldSelector.TotalCount);
  }

  getAddCriteriaButtonXpath(groupNum: number): string {
    return `${this.getIncludeGroupRootXpath(groupNum)}/ancestor::node()[1]/*[normalize-space()="Add Criteria"]/button`;
  }

  getIncludedGroupCountXpath(groupNum: number): string {
    return `${this.getIncludeGroupRootXpath(groupNum)}/ancestor::node()[1]${FieldSelector.GroupCount}`;
  }

  getIncludeGroupRootXpath(groupNum: number): string {
    return `//*[@id="list-include-groups"]//*[normalize-space()="Group ${groupNum}"]`;
  }

  private async openTieredMenu(groupNum: number): Promise<TieredMenu> {
    const addCriteriaButton = await this.page.waitForXPath(this.getAddCriteriaButtonXpath(groupNum), {visible: true});
    await addCriteriaButton.click(); // Click dropdown trigger to open menu
    return new TieredMenu(this.page);
  }

}
