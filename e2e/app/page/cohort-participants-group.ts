import { ElementHandle, Page } from 'puppeteer';
import CohortBuildPage, { FieldSelector } from 'app/page/cohort-build-page';
import { waitForNumericalString, waitForText, waitWhileLoading } from 'utils/waits-utils';
import CohortSearchPage from 'app/page/cohort-search-page';
import CriteriaSearchPage, { FilterSign, PhysicalMeasurementsCriteria, Visits } from 'app/page/criteria-search-page';
import TieredMenu from 'app/component/tiered-menu';
import { LinkText, MenuOption } from 'app/text-labels';
import { snowmanIconXpath } from 'app/component/snowman-menu';
import Modal from 'app/modal/modal';
import InputSwitch from 'app/element/input-switch';
import Button from 'app/element/button';
import ClrIconLink from 'app/element/clr-icon-link';

export default class CohortParticipantsGroup {
  private rootXpath: string;

  constructor(private readonly page: Page) {}

  setXpath(xpath: string): void {
    this.rootXpath = xpath;
  }

  async exists(): Promise<boolean> {
    return (await this.page.$x(this.rootXpath)).length > 0;
  }

  getAddCriteriaButton(): Button {
    const xpath = `${this.rootXpath}//*[normalize-space()="Add Criteria"]/button`;
    return new Button(this.page, xpath);
  }

  getAnyMentionOfButton(): Button {
    const xpath = `${this.rootXpath}//*[normalize-space()="Any mention of"]/button`;
    return new Button(this.page, xpath);
  }

  getDuringSameEncounterAsButton(): Button {
    const xpath = `${this.rootXpath}//*[normalize-space()="During same encounter as"]/button`;
    return new Button(this.page, xpath);
  }

  getGroupCountXpath(): string {
    return `${this.rootXpath}${FieldSelector.GroupCount}`;
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
    const textbox = modal.waitForTextbox('New Name:');
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
    return this.getGroupCriteriaList();
  }

  async includePhysicalMeasurement(
    criteriaName: PhysicalMeasurementsCriteria,
    opts?: { filterSign?: FilterSign; filterValue?: number }
  ): Promise<string> {
    await this.clickCriteriaMenuItems([MenuOption.PhysicalMeasurements]);
    const searchPage = new CriteriaSearchPage(this.page);
    await searchPage.waitForLoad();
    return searchPage.addPhysicalMeasurementsCriteria([criteriaName], opts);
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
    return waitForNumericalString(this.page, this.getGroupCountXpath(), 120000);
  }

  async includeAge(minAge: number, maxAge: number): Promise<string> {
    await this.clickCriteriaMenuItems([MenuOption.Demographics, MenuOption.Age]);
    const searchPage = new CohortSearchPage(this.page);
    await searchPage.waitForLoad();
    const results = await searchPage.addAge(minAge, maxAge);
    await waitWhileLoading(this.page);
    return results;
  }

  /*

  async addVisits(visits: Visits[]): Promise<void> {
    for (const visit of visits) {
      const icon = ClrIconLink.findByName(this.page, { startsWith: visit, iconShape: 'plus-circle', ancestorLevel: 1 });
      await icon.click();
      await this.finishReviewAndSaveCriteria();
    }
  }

   */
  async includeVisits(visits: Visits[]): Promise<string> {
    await this.clickCriteriaMenuItems([MenuOption.Visits]);
    const searchPage = new CriteriaSearchPage(this.page);
    await searchPage.waitForLoad();

    for (const visit of visits) {
      const icon = ClrIconLink.findByName(this.page, { startsWith: visit, iconShape: 'plus-circle', ancestorLevel: 1 });
      await icon.click();
      await searchPage.finishReviewAndSaveCriteria();
    }

    // Wait for Cohort Build page to load.
    await waitWhileLoading(this.page);
    const cohortBuildPage = new CohortBuildPage(this.page);
    await cohortBuildPage.waitForLoad();
    return cohortBuildPage.getTotalCount();
  }

  private async clickCriteriaMenuItems(menuItemLinks: MenuOption[]): Promise<void> {
    const menu = await this.openAddCriteriaTieredMenu();
    await menu.select(menuItemLinks);
  }

  async openAddCriteriaTieredMenu(): Promise<TieredMenu> {
    const addCriteriaButton = await this.getAddCriteriaButton();
    await addCriteriaButton.waitUntilEnabled();
    await addCriteriaButton.focus();
    await addCriteriaButton.click(); // Click dropdown trigger to open menu
    const tieredMenu = new TieredMenu(this.page);
    await tieredMenu.waitUntilVisible();
    return tieredMenu;
  }

  async openAnyMentionOfTieredMenu(): Promise<TieredMenu> {
    const anyMentionOfButton = await this.getAnyMentionOfButton();
    await anyMentionOfButton.waitUntilEnabled();
    await anyMentionOfButton.focus();
    await anyMentionOfButton.click(); // Click dropdown trigger to open menu
    const tieredMenu = new TieredMenu(this.page);
    await tieredMenu.waitUntilVisible();
    return tieredMenu;
  }

  async openDuringSameEncounterAsTieredMenu(): Promise<TieredMenu> {
    const duringSameEncounterButton = await this.getDuringSameEncounterAsButton();
    await duringSameEncounterButton.waitUntilEnabled();
    await duringSameEncounterButton.focus();
    await duringSameEncounterButton.click(); // Click dropdown trigger to open menu
    const tieredMenu = new TieredMenu(this.page);
    await tieredMenu.waitUntilVisible();
    return tieredMenu;
  }

  async getGroupCriteriaList(): Promise<ElementHandle[]> {
    const selector = `${this.rootXpath}//*[@data-test-id="item-list"]`;
    return this.page.$x(selector);
  }

  async clickTemporalSwitch(onoff: boolean): Promise<void> {
    // InputSwitch constructor takes xpath selector.
    const xpath = '//*[contains(concat(" ", normalize-space(@class), " ")," p-inputswitch ")  and @role="checkbox"]';
    const inputSwitch = new InputSwitch(this.page, xpath);
    onoff ? await inputSwitch.check() : await inputSwitch.unCheck();

    // waitForFunction takes css selector.
    const inputSwitchCss = '.p-inputswitch.p-component[role="checkbox"]';
    await this.page.waitForFunction(
      (css) => document.querySelectorAll(css),
      { polling: 'mutation', timeout: 30000 },
      inputSwitchCss
    );
    await this.page.waitForTimeout(1000);
  }
}
