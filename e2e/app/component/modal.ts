import Container from 'app/container';
import {ElementHandle, Page} from 'puppeteer';
import Button from 'app/element/button';
import Textbox from 'app/element/textbox';
import Textarea from 'app/element/textarea';
import Checkbox from 'app/element/checkbox';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils';
import {LinkText} from 'app/text-labels';
import {getPropValue} from 'utils/element-utils';
import * as fp from 'lodash/fp';
import {waitWhileLoading} from 'utils/waits-utils';

const Selector = {
  defaultDialog: '//*[@role="dialog"]',
}

export default class Modal extends Container {

  constructor(page: Page, xpath: string = Selector.defaultDialog) {
    super(page, xpath);
  }

  async waitForLoad(): Promise<this> {
    try {
      await this.waitUntilVisible();
      await waitWhileLoading(this.page);
    } catch (err) {
      await savePageToFile(this.page);
      await takeScreenshot(this.page);
      throw err;
    }
    return this;
  }

  async getTextContent(): Promise<string[]> {
    // xpath that excludes button labels and spans
    const selector = '//*[@role="dialog"]//div[normalize-space(text()) and not(@role="button")]';
    await this.waitUntilVisible();
    await this.page.waitForXPath(selector, {visible: true});
    const elements: ElementHandle[] = await this.page.$x(selector);
    return fp.flow(
       fp.map( async (elem: ElementHandle) => (await getPropValue<string>(elem, 'textContent')).trim()),
       contents => Promise.all(contents)
    )(elements);
  }

  /**
   * Click a button.
   * @param {string} buttonLabel The button text label.
   * @param waitOptions Wait for navigation or/and modal close after click button.
   */
  async clickButton(buttonLabel: LinkText, waitOptions: {waitForNav?: boolean, waitForClose?: boolean} = {}): Promise<void> {
    const { waitForNav = false, waitForClose = false } = waitOptions;
    const button = await this.waitForButton(buttonLabel);
    await button.waitUntilEnabled();
    await Promise.all( fp.flow(
       fp.filter<{shouldWait: boolean, waitFn: () => Promise<void>}>('shouldWait'),
       fp.map(item => item.waitFn()),
       fp.concat([button.click()])
    )([
      {shouldWait: waitForNav, waitFn: () => this.page.waitForNavigation({waitUntil: ['load', 'networkidle0']})},
      {shouldWait: waitForClose, waitFn: () => this.waitUntilClose()}
    ]));
  }

  async waitForButton(buttonLabel: LinkText): Promise<Button> {
    return Button.findByName(this.page, {normalizeSpace: buttonLabel}, this);
  }

  async waitForTextbox(textboxName: string): Promise<Textbox> {
    return Textbox.findByName(this.page, {name: textboxName}, this);
  }

  async waitForTextarea(textareaName: string): Promise<Textarea> {
    return Textarea.findByName(this.page, {name: textareaName}, this);
  }

  async waitForCheckbox(checkboxName: string): Promise<Checkbox> {
    return Checkbox.findByName(this.page, {name: checkboxName}, this);
  }

  async waitUntilClose(): Promise<void> {
    await this.page.waitForXPath(this.xpath, {hidden: true, timeout: 60000});
  }

  /**
   * Returns true if Dialog exists on page.
   */
  async exists(): Promise<boolean> {
    return (await this.page.$x(this.xpath)).length > 0;
  }

}
