import Container from 'app/container';
import {ElementHandle, Page} from 'puppeteer';
import Button from 'app/element/button';
import Textbox from 'app/element/textbox';
import Textarea from 'app/element/textarea';
import Checkbox from 'app/element/checkbox';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils';
import {LinkText} from 'app/text-labels';
import * as fp from 'lodash/fp';

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
    } catch (e) {
      await savePageToFile(this.page);
      await takeScreenshot(this.page);
      const title = await this.page.title();
      throw new Error(`"${title}" modal waitForLoad() encountered ${e}`);
    }
    return this;
  }

  async getContent(): Promise<string> {
    // xpath that excludes button labels and spans
    // '//*[@role="dialog"]//div[normalize-space(text()) and not(@role="button")]'
    const modal = await this.waitUntilVisible();
    const modalText = await (await modal.getProperty('innerText')).jsonValue();
    console.debug('Modal: \n' + modalText);
    return modalText.toString();
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
    await button.focus();
    await Promise.all( fp.flow(
       fp.filter<{shouldWait: boolean, waitFn: () => Promise<void>}>('shouldWait'),
       fp.map(item => item.waitFn()),
       fp.concat([button.click()])
    )([
      {shouldWait: waitForNav, waitFn: () => this.page.waitForNavigation({waitUntil: ['domcontentloaded', 'networkidle0']})},
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
    await this.page.waitForXPath(this.xpath, {hidden: true});
  }

  async waitUntilVisible(): Promise<ElementHandle> {
    return this.page.waitForXPath(this.xpath, {visible: true});
  }

  /**
   * Returns true if Dialog exists on page.
   */
  async exists(): Promise<boolean> {
    return (await this.page.$x(this.xpath)).length > 0;
  }

}
