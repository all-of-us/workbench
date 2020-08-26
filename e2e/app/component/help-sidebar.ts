import {ElementHandle, Page} from 'puppeteer';

import SelectMenu from 'app/component/select-menu';
import Container from 'app/container';
import Button from 'app/element/button';
import {FilterSign} from 'app/page/cohort-search-page';
import {LinkText} from 'app/text-labels';
import {buildXPath} from 'app/xpath-builders';
import {ElementType} from 'app/xpath-options';
import {waitForNumericalString} from 'utils/waits-utils';
import {waitWhileLoading} from '../../utils/test-utils';

const defaultXpath = '//*[@id="help-sidebar"]';
enum SectionSelectors {
  AttributesForm = '//*[@id="attributes-form"]',
  ModifiersForm = '//*[@id="modifiers-form"]',
  SelectionList = '//*[@id="selection-list"]',
}

export default class HelpSidebar extends Container {

  constructor(page: Page, xpath: string = defaultXpath) {
    super(page, xpath);
  }

  async getPhysicalMeasurementParticipantResult(filterSign: FilterSign, filterValue: number): Promise<string> {
    await this.waitUntilSectionVisible(SectionSelectors.AttributesForm);

    const selectMenu = await SelectMenu.findByName(this.page, {ancestorLevel: 2}, this);
    await selectMenu.clickMenuItem(filterSign);

    const numberField = await this.page.waitForXPath(`${this.xpath}//input[@type="number"]`, {visible: true});
    await numberField.type(String(filterValue));

    await this.clickSidebarButton(LinkText.Calculate);
    const participantResult = await this.waitForParticipantResult();

    // Find criteria in Selected Criteria Content Box.
    const removeSelectedCriteriaIconSelector = buildXPath({type: ElementType.Icon, iconShape: 'times-circle'}, this);
    // Before add criteria, first check for nothing in Selected Criteria Content Box.
    await this.page.waitForXPath(removeSelectedCriteriaIconSelector,{hidden: true});

    await this.clickSidebarButton(LinkText.AddThis);
    // After add criteria, should flip to selection list section
    await this.waitUntilSectionVisible(SectionSelectors.SelectionList);
    // Look for X (remove) icon for indication that add succeeded.
    await this.page.waitForXPath(removeSelectedCriteriaIconSelector,{visible: true});

    // dialog close after click FINISH button.
    await this.clickFinishButton();

    return participantResult;
  }

  async clickSidebarButton(buttonLabel: LinkText): Promise<void> {
    const button = await Button.findByName(this.page, {normalizeSpace: buttonLabel}, this);
    await button.click();
    await waitWhileLoading(this.page);
  }

  async clickFinishButton(): Promise<void> {
    await this.clickSidebarButton(LinkText.Finish);
    await this.waitUntilSectionHidden(SectionSelectors.SelectionList);
  }

  async waitForParticipantResult(): Promise<string> {
    const selector = `${this.xpath}//*[./*[contains(text(), "Results")]]/div[contains(text(), "Number")]`;
    return waitForNumericalString(this.page, selector);
  }

  waitUntilSectionVisible(xpath: string): Promise<ElementHandle> {
    return this.page.waitForXPath(xpath, {visible: true});
  }

  waitUntilSectionHidden(xpath: string): Promise<ElementHandle> {
    return this.page.waitForXPath(xpath, {hidden: true});
  }
}
