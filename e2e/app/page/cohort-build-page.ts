import { Page } from 'puppeteer';
import TieredMenu from 'app/component/tiered-menu';
import Button from 'app/element/button';
import ClrIconLink from 'app/element/clr-icon-link';
import { ElementType } from 'app/xpath-options';
import { makeRandomName, numericalStringToNumber } from 'utils/str-utils';
import { waitForDocumentTitle, waitForNumericalString, waitForText, waitWhileLoading } from 'utils/waits-utils';
import { buildXPath } from 'app/xpath-builders';
import { LinkText, MenuOption } from 'app/text-labels';
import Modal from 'app/modal/modal';
import AuthenticatedPage from './authenticated-page';
import CohortParticipantsGroup from './cohort-participants-group';
import CohortSaveAsModal from 'app/modal/cohort-save-as-modal';
import { getPropValue } from 'utils/element-utils';
import WarningDiscardChangesModal from 'app/modal/warning-discard-changes-modal';
import WorkspaceDataPage from './workspace-data-page';
import faker from 'faker';

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
    await Promise.all([
      waitForDocumentTitle(this.page, PageTitle),
      waitWhileLoading(this.page),
      this.findIncludeParticipantsGroup('Group 1').getAddCriteriaButton().asElementHandle()
    ]);
    return true;
  }

  async getCohortName(): Promise<string> {
    const h3Xpath = '//h3[./ancestor::node()[2]/*[@id="list-include-groups"]]';
    const h3 = await this.page.waitForXPath(h3Xpath, { visible: true });
    return getPropValue<string>(h3, 'textContent');
  }

  /**
   * Save Cohort changes.
   */
  async saveChanges(menuOption: MenuOption = MenuOption.Save): Promise<string> {
    let cohortName = await this.getCohortName();

    const createCohortButton = Button.findByName(this.page, { normalizeSpace: LinkText.SaveCohort });
    await createCohortButton.waitUntilEnabled();
    await createCohortButton.click(); // Click dropdown trigger to open menu
    const menu = new TieredMenu(this.page);
    await menu.select(menuOption);

    switch (menuOption) {
      case MenuOption.SaveAs:
        cohortName = makeRandomName();
        const modal = new CohortSaveAsModal(this.page);
        await modal.waitForLoad();
        await modal.typeCohortName(cohortName);
        await modal.clickSaveButton();
        break;
      default:
        break;
    }
    await waitForText(this.page, 'Cohort Saved Successfully');
    return cohortName;
  }

  async createCohort(cohortName?: string, description?: string): Promise<string> {
    const createCohortButton = this.getCreateCohortButton();
    await createCohortButton.waitUntilEnabled();
    await createCohortButton.click();

    if (cohortName === undefined) {
      cohortName = makeRandomName();
    }
    if (description === undefined) {
      description = faker.lorem.words(10);
    }

    const modal = new CohortSaveAsModal(this.page);
    await modal.waitForLoad();
    await modal.typeCohortName(cohortName);
    await modal.typeDescription(description);
    await modal.clickSaveButton();

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
    await new WorkspaceDataPage(this.page).waitForLoad();
    return contentText;
  }

  /**
   * Click DISCARD CHANGES button in Confirmation dialog.
   * @return {string} Dialog textContent.
   */
  async discardChangesConfirmationDialog(): Promise<string[]> {
    const modal = new WarningDiscardChangesModal(this.page);
    await modal.waitForLoad();
    const contentText = await modal.getTextContent();
    await modal.clickDiscardChangesButton();
    return contentText;
  }

  /**
   * Find the Cohort Total Count.
   * This function also can be used to wait until participants calculation has completed.
   * @return {string} Total Count.
   */
  async getTotalCount(timeout = 120000): Promise<number> {
    const chartsCss = '.highcharts-container .highcharts-bar-series rect';
    const count = await waitForNumericalString(this.page, FieldSelector.TotalCount, timeout);
    await this.page.waitForFunction(
      (css) => document.querySelectorAll(css),
      { polling: 'mutation', timeout },
      chartsCss
    );
    return numericalStringToNumber(count);
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
    group.setXpath(`//*[@id="list-include-groups"]//div[./*[normalize-space()="${groupName}"]]`);
    return group;
  }

  findExcludeParticipantsGroup(groupName: string): CohortParticipantsGroup {
    const group = new CohortParticipantsGroup(this.page);
    group.setXpath(`//*[@id="list-exclude-groups"][.//*[normalize-space()="${groupName}"]]`);
    return group;
  }

  findIncludeParticipantsEmptyGroup(): CohortParticipantsGroup {
    const group = new CohortParticipantsGroup(this.page);
    group.setXpath(
      '//*[@id="list-include-groups"]' +
        '/*[./*[not(@data-test-id="includes-search-group")][./button[normalize-space()="Add Criteria"]]]'
    );
    return group;
  }

  // Find Include Participants Group: Group 1 is index 1, Group 2 is index 2, etc.
  findIncludeParticipantsGroupByIndex(index = 1): CohortParticipantsGroup {
    const group = new CohortParticipantsGroup(this.page);
    group.setXpath(`//*[@id="list-include-groups"]//div[./*[@data-test-id="item-list"]][${index}]`);
    return group;
  }

  async findGenderSelectMenu(): Promise<TieredMenu> {
    const xpath = '//*[./div[text()="Results by"]]//button[text()][./clr-icon][1]';
    const button = new Button(this.page, xpath);
    await button.click();
    return new TieredMenu(this.page);
  }

  async findAgeSelectMenu(): Promise<TieredMenu> {
    const xpath = '//*[./div[text()="Results by"]]//button[text()][./clr-icon][2]';
    const button = await this.page.waitForXPath(xpath, { visible: true });
    await button.click();
    return new TieredMenu(this.page);
  }

  findRefreshButton(): Button {
    return new Button(this.page, '//*[./div[text()="Results by"]]//button[.="REFRESH"]');
  }
}
