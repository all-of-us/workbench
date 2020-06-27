import {ElementHandle, Page} from 'puppeteer';
import {FieldSelector} from 'app/page/cohort-build-page';
import Dialog from 'app/component/dialog';
import EllipsisMenu from 'app/component/ellipsis-menu';
import {waitForNumericalString, waitForText} from 'utils/waits-utils';
import CohortCriteriaModal, {FilterSign, PhysicalMeasurementsCriteria} from 'app/page/cohort-criteria-modal';
import TieredMenu from 'app/component/tiered-menu';
import {waitWhileLoading} from 'utils/test-utils';
import {LinkText} from 'app/text-labels';

export enum GroupAction {
   EditGroupName  = 'Edit group name',
   SuppressGroupFromTotalCount = 'Suppress group from total count',
   DeleteGroup = 'Delete group',
}

export default class CohortParticipantsGroup {

  private rootXpath: string;

  constructor(private readonly page: Page) {

  }

  setXpath(xpath: string): void {
    this.rootXpath = xpath;
  }

  async exists(): Promise<boolean> {
    return (await this.page.$x(this.rootXpath)).length > 0;
  }

  getAddCriteriaButtonXpath(): string {
    return `${this.rootXpath}/ancestor::node()[1]/*[normalize-space()="Add Criteria"]/button`;
  }

  getGroupCountXpath(): string {
    return `${this.rootXpath}/ancestor::node()[1]${FieldSelector.GroupCount}`;
  }

  getGroupEllipsisMenu(): EllipsisMenu {
    const ellipsisXpath = `${this.rootXpath}//clr-icon[@shape="ellipsis-vertical"]`;
    return new EllipsisMenu(this.page, ellipsisXpath);
  }

   /**
    * Update Group name.
    * @param {string} newGroupName
    * @return {boolean} Returns TRUE if rename was successful.
    */
  async editGroupName(newGroupName: string): Promise<void> {
    const menu = this.getGroupEllipsisMenu();
    await menu.clickParticipantsGroupAction(GroupAction.EditGroupName);
    const dialog = new Dialog(this.page);
    const textbox = await dialog.waitForTextbox('New Name:');
    await textbox.type(newGroupName);
    await dialog.waitForButton(LinkText.Rename).then(b => b.click());
    await dialog.waitUntilDialogIsClosed();
  }

  /**
   * Delete Group.
   * @return Returns array of criterias in this group.
   */
  async deleteGroup(): Promise<ElementHandle[]> {
    const menu = this.getGroupEllipsisMenu();
    await menu.clickParticipantsGroupAction(GroupAction.DeleteGroup);
    await waitForText(this.page, 'This group has been deleted');
    await waitWhileLoading(this.page);
    return this.getGroupCriteriasList();
  }

  async includePhysicalMeasurement(criteriaName: PhysicalMeasurementsCriteria, value: number): Promise<string> {
    await this.clickCriteriaMenuItems(['Physical Measurements']);
    const modal = new CohortCriteriaModal(this.page);
    await modal.waitUntilVisible();
    return modal.filterPhysicalMeasurementValue(criteriaName, FilterSign.GreaterThanOrEqualTo, value);
  }

  async includeDemographicsDeceased(): Promise<string> {
    await this.clickCriteriaMenuItems(['Demographics', 'Deceased']);
    return this.getGroupCount();
  }

  async includeConditions(): Promise<CohortCriteriaModal> {
    await this.clickCriteriaMenuItems(['Conditions']);
    const modal = new CohortCriteriaModal(this.page);
    await modal.waitUntilVisible();
    return modal;
  }

  async includeDrugs(): Promise<CohortCriteriaModal> {
    await this.clickCriteriaMenuItems(['Drugs']);
    const modal = new CohortCriteriaModal(this.page);
    await modal.waitUntilVisible();
    return modal;
  }

  async includeEthnicity(): Promise<CohortCriteriaModal> {
    await this.clickCriteriaMenuItems(['Demographics', 'Ethnicity']);
    const modal = new CohortCriteriaModal(this.page, '//*[@class="modal-container demographics"]');
    await modal.waitUntilVisible();
    return modal;
  }

  async getGroupCount(): Promise<string> {
    return waitForNumericalString(this.page, this.getGroupCountXpath());
  }

  async includeAge(minAge: number, maxAge: number): Promise<string> {
    await this.clickCriteriaMenuItems(['Demographics', 'Age']);
    const modal = new CohortCriteriaModal(this.page, '//*[@class="modal-container demographics age"]');
    await modal.waitUntilVisible();
    const results = await modal.addAge(minAge, maxAge);
    await waitWhileLoading(this.page);
    return results;
  }

  async includeVisits(): Promise<CohortCriteriaModal> {
    await this.clickCriteriaMenuItems(['Visits']);
    const modal = new CohortCriteriaModal(this.page);
    await modal.waitUntilVisible();
    return modal;
  }

  private async clickCriteriaMenuItems(menuItemLinks: string[]): Promise<void> {
    const menu = await this.openTieredMenu();
    return menu.clickMenuItem(menuItemLinks);
  }

  private async openTieredMenu(): Promise<TieredMenu> {
    const addCriteriaButton = await this.page.waitForXPath(this.getAddCriteriaButtonXpath(), {visible: true});
    await addCriteriaButton.click(); // Click dropdown trigger to open menu
    return new TieredMenu(this.page);
  }

  async getGroupCriteriasList(): Promise<ElementHandle[]> {
    const selector = `${this.rootXpath}//*[@data-test-id="item-list"]`;
    return this.page.$x(selector);
  }

}
