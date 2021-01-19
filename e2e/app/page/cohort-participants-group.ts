import {ElementHandle, Page} from 'puppeteer';
import {FieldSelector} from 'app/page/cohort-build-page';
import Modal from 'app/component/modal';
import {waitForNumericalString, waitForText, waitWhileLoading} from 'utils/waits-utils';
import CohortSearchPage from 'app/page/cohort-search-page';
import CriteriaSearchPage, {FilterSign, PhysicalMeasurementsCriteria} from 'app/page/criteria-search-page';
import TieredMenu from 'app/component/tiered-menu';
import {LinkText, Option} from 'app/text-labels';
import {snowmanIconXpath} from 'app/component/snowman-menu';
import Button from 'app/element/button';
import HelpSidebar from 'app/component/help-sidebar';

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

  async clickSnowmanIcon(): Promise<void> {
    const iconXpath = `${this.rootXpath}${snowmanIconXpath}`;
    await this.page.waitForXPath(iconXpath, {visible: true})
       .then(icon => icon.click());
  }

  /**
   * Build Cohort Criteria page: Group snowman menu
   * @param {GroupMenuOption} option
   */
  async selectGroupSnowmanMenu(option: Option): Promise<void> {
    const menu = new TieredMenu(this.page);
    return menu.select(option);
  }

  /**
   * Update Group name.
   * @param {string} newGroupName
   * @return {boolean} Returns TRUE if rename was successful.
   */
  async editGroupName(newGroupName: string): Promise<void> {
    await this.clickSnowmanIcon();
    await this.selectGroupSnowmanMenu(Option.EditGroupName);
    const modal = new Modal(this.page);
    const textbox = await modal.waitForTextbox('New Name:');
    await textbox.type(newGroupName);
    await modal.clickButton(LinkText.Rename, {waitForClose: true});
  }

  /**
   * Delete Group.
   * @return Returns array of criterias in this group.
   */
  async deleteGroup(): Promise<ElementHandle[]> {
    await this.clickSnowmanIcon();
    await this.selectGroupSnowmanMenu(Option.DeleteGroup);
    try {
      await waitForText(this.page, 'This group has been deleted');
    } catch (err) {
      // Sometimes message fails to show up. Ignore error.
    }
    await waitWhileLoading(this.page);
    return this.getGroupCriteriasList();
  }

  async includePhysicalMeasurement(criteriaName: PhysicalMeasurementsCriteria, value: number): Promise<string> {
    await this.clickCriteriaMenuItems([Option.PhysicalMeasurements]);
    const searchPage = new CriteriaSearchPage(this.page);
    await searchPage.waitForLoad();
    return searchPage.filterPhysicalMeasurementValue(criteriaName, FilterSign.GreaterThanOrEqualTo, value);
  }

  async includeDemographicsDeceased(): Promise<string> {
    await this.clickCriteriaMenuItems([Option.Demographics, Option.Deceased]);
    return this.getGroupCount();
  }

  async includeConditions(): Promise<CriteriaSearchPage> {
    await this.clickCriteriaMenuItems([Option.Conditions]);
    const searchPage = new CriteriaSearchPage(this.page);
    await searchPage.waitForLoad();
    return searchPage;
  }

  async includeDrugs(): Promise<CriteriaSearchPage> {
    await this.clickCriteriaMenuItems([Option.Drugs]);
    const searchPage = new CriteriaSearchPage(this.page);
    await searchPage.waitForLoad();
    return searchPage;
  }

  async includeEthnicity(): Promise<CohortSearchPage> {
    await this.clickCriteriaMenuItems([Option.Demographics, Option.Ethnicity]);
    const searchPage = new CohortSearchPage(this.page);
    await searchPage.waitForLoad();
    return searchPage;
  }

  async getGroupCount(): Promise<string> {
    return waitForNumericalString(this.page, this.getGroupCountXpath(), 60000);
  }

  async includeAge(minAge: number, maxAge: number): Promise<string> {
    await this.clickCriteriaMenuItems([Option.Demographics, Option.Age]);
    const searchPage = new CohortSearchPage(this.page);
    await searchPage.waitForLoad();
    const results = await searchPage.addAge(minAge, maxAge);
    await waitWhileLoading(this.page);
    return results;
  }

  async includeVisits(): Promise<CriteriaSearchPage> {
    await this.clickCriteriaMenuItems([Option.Visits]);
    const searchPage = new CriteriaSearchPage(this.page);
    await searchPage.waitForLoad();
    return searchPage;
  }

  private async clickCriteriaMenuItems(menuItemLinks: Option[]): Promise<void> {
    const menu = await this.openTieredMenu();
    await menu.select(menuItemLinks);
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

  async viewAndSaveCriteria(): Promise<void> {
    const finishAndReviewButton = await Button.findByName(this.page, {name: LinkText.FinishAndReview});
    await finishAndReviewButton.waitUntilEnabled();
    await finishAndReviewButton.click();

    // Click Save Criteria button in sidebar
    const helpSidebar = new HelpSidebar(this.page);
    await helpSidebar.clickSaveCriteriaButton();
  }

}
