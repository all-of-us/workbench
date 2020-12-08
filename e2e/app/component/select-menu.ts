import Container from 'app/container';
import BaseElement from 'app/element/base-element';
import {buildXPath} from 'app/xpath-builders';
import {ElementType, XPathOptions} from 'app/xpath-options';
import {Page} from 'puppeteer';
import {getPropValue} from 'utils/element-utils';

export default class SelectMenu extends Container {

  static async findByName(page: Page, xOpt: XPathOptions = {}, container?: Container): Promise<SelectMenu> {
    xOpt.type = ElementType.Dropdown;
    const menuXpath = buildXPath(xOpt, container);
    const selectMenu = new SelectMenu(page, menuXpath);
    await page.waitForXPath(menuXpath, {visible: true});
    return selectMenu;
  }

  constructor(page: Page, xpath: string) {
    super(page, xpath);
  }

  /**
   * Select an option in a Select element.
   * @param {string} textValue Partial or full string to click in Select.
   * @param {number} maxAttempts Default try count is 2.
   */
  async clickMenuItem(textValue: string, maxAttempts: number = 2): Promise<void> {

    const clickText = async () => {
      await this.open(2);
      const selector = this.xpath + `//li[contains(normalize-space(text()), "${textValue}")]`;
      const selectValue = await this.page.waitForXPath(selector, {visible: true});
      const baseElement = BaseElement.asBaseElement(this.page, selectValue);
      const textContent = await baseElement.getTextContent(); // get full text
      await selectValue.click();
      // need to make sure dropdown is disappeared, so it cannot interfere with clicking on elements below.
      await this.waitUntilDropdownClosed();
      return textContent;
    };

    const clickAndCheck = async () => {
      maxAttempts--;
      const shouldSelectedValue = await clickText();
      // compare to displayed text in Select textbox
      const displayedValue = await this.getSelectedValue();
      if (shouldSelectedValue === displayedValue) {
        return shouldSelectedValue; // success
      }
      if (maxAttempts <= 0) {
        return null;
      }
      return await this.page.waitForTimeout(2000).then(clickAndCheck); // two seconds pause and retry
    };

    await clickAndCheck();
  }

  /**
   * Returns selected value in Select.
   */
  async getSelectedValue(): Promise<string> {
    const selector = this.xpath + '/label';
    const displayedValue = await this.page.waitForXPath(selector, { visible: true });
    return getPropValue<string>(displayedValue, 'innerText');
  }

  /**
   * Open Select dropdown.
   * @param {number} maxAttempts Default is 1.
   */
  private async open(maxAttempts: number = 1): Promise<void> {
    const click = async () => {
      await this.toggle();
      const opened = await this.isOpen();
      if (opened) {
        return;
      }
      if (maxAttempts <= 0) {
        return;
      }
      maxAttempts--;
      await this.page.waitForTimeout(1000).then(click); // one second pause before try again
    };
    return click();
  }

  private async toggle(): Promise<void> {
    const selector = this.xpath + '/*[@class="p-dropdown-trigger"]';
    const dropdownTrigger = await this.page.waitForXPath(selector, { visible: true });
    await dropdownTrigger.hover();
    await dropdownTrigger.click();
    await dropdownTrigger.dispose();
  }

  private async isOpen(): Promise<boolean> {
    const selector = this.xpath + '/*[contains(concat(" ", normalize-space(@class), " "), " p-dropdown-panel ")]';
    try {
      const panel = await this.page.waitForXPath(selector);
      const classNameString = await getPropValue<string>(panel, 'className');
      const splits = classNameString.toString().split(' ');
      await panel.dispose();
      return splits.includes('p-input-overlay-visible');
    } catch (err) {
      return false;
    }
  }

  private async waitUntilDropdownClosed() {
    const xpath = this.xpath + '/*[contains(concat(" ", normalize-space(@class), " "), " p-input-overlay-visible ")]';
    await this.page.waitForXPath(xpath, {hidden: true}).catch((err) => {
      console.error('Select dropdown is not closed.');
      throw err;
    })
  }

}
