import { ElementHandle, Page } from 'puppeteer';
import { FieldSelector } from 'app/page/cohort-build-page';
import { waitForNumericalString, waitForText, waitWhileLoading } from 'utils/waits-utils';
import CohortSearchPage from 'app/page/cohort-search-page';
import CriteriaSearchPage, { FilterSign, PhysicalMeasurementsCriteria } from 'app/page/criteria-search-page';
import TieredMenu from 'app/component/tiered-menu';
import { LinkText, MenuOption } from 'app/text-labels';
import { snowmanIconXpath } from 'app/component/snowman-menu';
import Modal from 'app/modal/modal';

export default class CohortParticipantsGroup {
  private rootXpath: string;

  constructor(private readonly page: Page) {}

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
    await this.page.waitForXPath(iconXpath, { visible: true }).then((icon) => icon.click());
  }

  /**
   * Build Cohort Criteria page: Group snowman menu
   * @param {GroupMenuOption} option
   */
  async selectGroupSnowmanMenu(option: MenuOption): Promise<void> {
    const menu = new TieredMenu(this.page);
    await menu.waitUntilVisible();
    return menu.select(option);
  }

  /**
   * Update Group name.
   * @param {string} newGroupName
   * @return {boolean} Returns TRUE if rename was successful.
   */
  async editGroupName(newGroupName: string): Promise<void> {
    await this.clickSnowmanIcon();
    await this.selectGroupSnowmanMenu(MenuOption.EditGroupName);
    const modal = new Modal(this.page);
    const textbox = await modal.waitForTextbox('New Name:');
    await textbox.type(newGroupName);
    await modal.clickButton(LinkText.Rename, { waitForClose: true });
  }

  /**
   * Delete Group.
   * @return Returns array of criterias in this group.
   */
  async deleteGroup(): Promise<ElementHandle[]> {
    await this.clickSnowmanIcon();
    await this.selectGroupSnowmanMenu(MenuOption.DeleteGroup);
    try {
      await waitForText(this.page, 'This group has been deleted');
    } catch (err) {
      // Sometimes message fails to show up. Ignore error.
    }
    await waitWhileLoading(this.page);
    return this.getGroupCriteriasList();
  }

  async includePhysicalMeasurement(criteriaName: PhysicalMeasurementsCriteria, value: number): Promise<string> {
    await this.clickCriteriaMenuItems([MenuOption.PhysicalMeasurements]);
    const searchPage = new CriteriaSearchPage(this.page);
    await searchPage.waitForLoad();
    return searchPage.filterPhysicalMeasurementValue(criteriaName, FilterSign.GreaterThanOrEqualTo, value);
  }

  async includeDemographicsDeceased(): Promise<string> {
    await this.clickCriteriaMenuItems([MenuOption.Demographics, MenuOption.Deceased]);
    return this.getGroupCount();
  }

  async includeConditions(): Promise<CriteriaSearchPage> {
    await this.clickCriteriaMenuItems([MenuOption.Conditions]);
    const searchPage = new CriteriaSearchPage(this.page);
    await searchPage.waitForLoad();
    return searchPage;
  }

  async includeDrugs(): Promise<CriteriaSearchPage> {
    await this.clickCriteriaMenuItems([MenuOption.Drugs]);
    const searchPage = new CriteriaSearchPage(this.page);
    await searchPage.waitForLoad();
    return searchPage;
  }

  async includeEthnicity(): Promise<CohortSearchPage> {
    await this.clickCriteriaMenuItems([MenuOption.Demographics, MenuOption.Ethnicity]);
    const searchPage = new CohortSearchPage(this.page);
    await searchPage.waitForLoad();
    return searchPage;
  }

  async getGroupCount(): Promise<string> {
    return waitForNumericalString(this.page, this.getGroupCountXpath(), 60000);
  }

  async includeAge(minAge: number, maxAge: number): Promise<string> {
    await this.clickCriteriaMenuItems([MenuOption.Demographics, MenuOption.Age]);
    const searchPage = new CohortSearchPage(this.page);
    await searchPage.waitForLoad();
    const results = await searchPage.addAge(minAge, maxAge);
    await waitWhileLoading(this.page);
    return results;
  }

  async includeVisits(): Promise<CriteriaSearchPage> {
    await this.clickCriteriaMenuItems([MenuOption.Visits]);
    const searchPage = new CriteriaSearchPage(this.page);
    await searchPage.waitForLoad();
    return searchPage;
  }

  private async clickCriteriaMenuItems(menuItemLinks: MenuOption[]): Promise<void> {
    const menu = await this.openTieredMenu();
    await menu.select(menuItemLinks);
  }

  private async openTieredMenu(): Promise<TieredMenu> {
    const addCriteriaButton = await this.page.waitForXPath(this.getAddCriteriaButtonXpath(), { visible: true });
    await addCriteriaButton.click(); // Click dropdown trigger to open menu
    const tieredMenu = new TieredMenu(this.page);
    await tieredMenu.waitUntilVisible();
    return tieredMenu;
  }

  async getGroupCriteriasList(): Promise<ElementHandle[]> {
    const selector = `${this.rootXpath}//*[@data-test-id="item-list"]`;
    return this.page.$x(selector);
  }
}
