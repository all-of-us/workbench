import {ElementHandle, Page} from 'puppeteer';
import SelectMenu from 'app/component/select-menu';
import Button from 'app/element/button';
import {FilterSign} from 'app/page/criteria-search-page';
import {LinkText, SideBarLink} from 'app/text-labels';
import {buildXPath} from 'app/xpath-builders';
import {ElementType} from 'app/xpath-options';
import {waitForNumericalString, waitWhileLoading} from 'utils/waits-utils';
import BaseHelpSidebar from './base-help-sidebar';

const defaultXpath = '//*[@id="help-sidebar"]';
enum SectionSelectors {
  AttributesForm = '//*[@id="attributes-form"]',
  ModifiersForm = '//*[@id="modifiers-form"]',
  SelectionList = '//*[@id="selection-list"]',
}

export default class HelpSidebar extends BaseHelpSidebar {

  constructor(page: Page, xpath: string = defaultXpath) {
    super(page);
    super.setXpath(`${super.getXpath()}${xpath}`);
  }

  async open(): Promise<void> {
    const isOpen = await this.isVisible();
    if (isOpen) return;
    await this.clickIcon(SideBarLink.HelpTips);
    await this.waitUntilVisible();
    console.log(`Opened "${await this.getTitle()}" Help Tips sidebar`);
  }

  async getPhysicalMeasurementParticipantResult(filterSign: FilterSign, filterValue: number): Promise<string> {
    await this.waitUntilSectionVisible(SectionSelectors.AttributesForm);

    const selectMenu = await SelectMenu.findByName(this.page, {ancestorLevel: 0}, this);
    await selectMenu.select(filterSign);

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

    // Sidebar close after click Save Criteria button.
    await this.clickSaveCriteriaButton();

    return participantResult;
  }

  async addAgeModifier(filterSign: FilterSign, filterValue: number): Promise<string> {
    await this.clickSidebarButton(LinkText.ApplyModifiers);
    await this.waitUntilSectionVisible(SectionSelectors.ModifiersForm);

    const selectMenu = await SelectMenu.findByName(this.page, {name: 'Age At Event', ancestorLevel: 1}, this);
    await selectMenu.select(filterSign);
    const numberField = await this.page.waitForXPath(`${this.xpath}//input[@type="number"]`, {visible: true});
    // Issue with Puppeteer type() function: typing value in this textbox doesn't always trigger change event. workaround is needed.
    // Error: "Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation."
    await numberField.focus();
    await numberField.click();
    await this.page.keyboard.type(String(filterValue));
    await numberField.press('Tab', { delay: 200 });

    let participantResult;
    await this.clickSidebarButton(LinkText.Calculate);
    try {
      participantResult = await this.waitForParticipantResult();
    } catch (e) {
      // Retry one more time.
      await this.clickSidebarButton(LinkText.Calculate);
      participantResult = await this.waitForParticipantResult();
    }
    await this.clickSidebarButton(LinkText.ApplyModifiers);
    await this.waitUntilSectionVisible(SectionSelectors.SelectionList);
    console.debug(`Age Modifier: ${filterSign} ${filterValue}  => number of participants: ${participantResult}`);
    return participantResult;
  }

  async clickSidebarButton(buttonLabel: LinkText): Promise<void> {
    await this.findSidebarButton(buttonLabel).then(butn => butn.click());
    await waitWhileLoading(this.page);
  }

  async findSidebarButton(buttonLabel: LinkText): Promise<Button> {
    const button = await Button.findByName(this.page, {normalizeSpace: buttonLabel}, this);
    await button.waitUntilEnabled();
    return button;
  }

  async clickSaveCriteriaButton(): Promise<void> {
    await this.clickSidebarButton(LinkText.SaveCriteria);
    await this.waitUntilSectionHidden(SectionSelectors.SelectionList);
    await this.page.waitForXPath(this.xpath, {visible: false});
  }

  async clickSaveConceptSetButton(): Promise<void> {
    await this.clickSidebarButton(LinkText.SaveConceptSet);
    await this.waitUntilSectionHidden(SectionSelectors.SelectionList);
    await this.page.waitForXPath(this.xpath, {visible: false});
  }

  async waitForParticipantResult(): Promise<string> {
    const selector = `${this.xpath}//*[./*[contains(text(), "Number of Participants")]]`;
    return waitForNumericalString(this.page, selector);
  }

  waitUntilSectionVisible(xpath: string): Promise<ElementHandle> {
    return this.page.waitForXPath(xpath, {visible: true});
  }

  waitUntilSectionHidden(xpath: string): Promise<ElementHandle> {
    return this.page.waitForXPath(xpath, {hidden: true, visible: false});
  }

}
