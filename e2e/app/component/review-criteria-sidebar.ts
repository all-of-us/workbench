import { ElementHandle, Page } from 'puppeteer';
import SelectMenu from 'app/component/select-menu';
import { LinkText } from 'app/text-labels';
import { buildXPath } from 'app/xpath-builders';
import { ElementType } from 'app/xpath-options';
import { waitForNumericalString, waitWhileLoading } from 'utils/waits-utils';
import BaseHelpSidebar, { Selectors } from './base-help-sidebar';
import { logger } from 'libs/logger';
import { FilterSign } from 'app/page/cohort-participants-group';
import { getPropValue } from 'utils/element-utils';

enum SectionSelectors {
  AttributesForm = '//*[@id="attributes-form"]',
  ModifiersForm = '//*[@id="modifiers-form"]',
  SelectionList = '//*[@id="selection-list"]'
}

export default class ReviewCriteriaSidebar extends BaseHelpSidebar {
  participantResultXpath = `${this.xpath}//*[./*[contains(text(), "Number of Participants")]]`;

  constructor(page: Page) {
    super(page);
  }

  // Not implemented because it's not triggered to open by sidebar tab.
  // eslint-disable-next-line @typescript-eslint/require-await
  async open(): Promise<void> {
    throw new Error('Do not use. Method not to be implemented.');
  }

  async waitUntilVisible(): Promise<void> {
    await super.waitUntilVisible();
    const title = await this.getTitle();
    await this.waitUntilSectionVisible(SectionSelectors.SelectionList);
    logger.info(`"${title}" sidebar is opened`);
  }

  async getCriteriaCount(): Promise<number> {
    const xpath = `${Selectors.rootXpath}//*[@data-test-id="criteria-count"]`;
    const element = await this.page.waitForXPath(xpath, { visible: true });
    return getPropValue<number>(element, 'textContent');
  }

  async getPhysicalMeasurementParticipantResult(filterSign: FilterSign, filterValue: number): Promise<string> {
    await this.waitUntilSectionVisible(SectionSelectors.AttributesForm);

    const selectMenu = SelectMenu.findByName(this.page, { ancestorLevel: 0 }, this);
    await selectMenu.select(filterSign);

    const numberField = await this.page.waitForXPath(`${this.xpath}//input[@type="number"]`, { visible: true });
    await numberField.type(String(filterValue));

    await this.clickButton(LinkText.Calculate);
    const participantResult = await waitForNumericalString(this.page, this.participantResultXpath);

    // Find criteria in Selected Criteria Content Box.
    const removeSelectedCriteriaIconSelector = buildXPath({ type: ElementType.Icon, iconShape: 'times-circle' }, this);
    // Before add criteria, first check for nothing in Selected Criteria Content Box.
    await this.page.waitForXPath(removeSelectedCriteriaIconSelector, { hidden: true });

    await this.clickButton(LinkText.AddThis);
    // After add criteria, should flip to selection list section
    await this.waitUntilSectionVisible(SectionSelectors.SelectionList);
    // Look for X (remove) icon for indication that add succeeded.
    await this.page.waitForXPath(removeSelectedCriteriaIconSelector, { visible: true });

    // Sidebar close after click Save Criteria button.
    await this.clickSaveCriteriaButton();

    return participantResult;
  }

  async addAgeModifier(filterSign: FilterSign, filterValue: number): Promise<string> {
    await this.clickButton(LinkText.ApplyModifiers);
    await this.waitUntilSectionVisible(SectionSelectors.ModifiersForm);

    const selectMenu = SelectMenu.findByName(this.page, { name: 'Age At Event', ancestorLevel: 1 }, this);
    await selectMenu.select(filterSign);
    const numberField = await this.page.waitForXPath(`${this.xpath}//input[@type="number"]`, { visible: true });
    // Issue with Puppeteer type() function: typing value in this textbox doesn't always trigger change event. workaround is needed.
    // Error: "Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation."
    await numberField.focus();
    await numberField.click();
    await this.page.keyboard.type(String(filterValue));
    await numberField.press('Tab', { delay: 200 });

    let participantResult: string;
    await this.clickButton(LinkText.Calculate);
    try {
      participantResult = await waitForNumericalString(this.page, this.participantResultXpath);
    } catch (e) {
      // Retry one more time.
      await this.clickButton(LinkText.Calculate);
      participantResult = await waitForNumericalString(this.page, this.participantResultXpath);
    }
    await this.clickButton(LinkText.ApplyModifiers);
    await this.waitUntilSectionVisible(SectionSelectors.SelectionList);
    return participantResult;
  }

  async clickSaveCriteriaButton(): Promise<void> {
    await this.clickButton(LinkText.SaveCriteria, { waitForClose: true });
    await waitWhileLoading(this.page);
  }

  async waitUntilSectionVisible(xpath: string): Promise<ElementHandle> {
    return this.page.waitForXPath(xpath, { visible: true });
  }

  async removeSelectedCriteria(criteriaName: string): Promise<void> {
    const removeSelectedCriteriaIcon = buildXPath(
      {
        type: ElementType.Icon,
        iconShape: 'times-circle',
        containsText: criteriaName,
        ancestorLevel: 2
      },
      this
    );
    const removeIcon = await this.page.waitForXPath(removeSelectedCriteriaIcon, { visible: true });
    await removeIcon.click();
    await this.page.waitForXPath(removeSelectedCriteriaIcon, { hidden: true });
  }
}
