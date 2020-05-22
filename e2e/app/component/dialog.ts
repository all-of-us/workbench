import Container from 'app/container';
import {xPathOptionToXpath} from 'app/element/xpath-defaults';
import {ElementHandle, Page} from 'puppeteer';
import {ElementType} from '../xpath-options';

export enum ButtonLabel {
  Confirm = 'Confirm',
  KeepEditing = 'Keep Editing',
  Cancel = 'Cancel',
  Calculate = 'Calculate',
  AddThis = 'ADD THIS',
  Finish = 'Finish',
}

const Selector = {
  defaultDialog: '//*[@role="dialog"]',
}

export default class Dialog extends Container {

  private dialogElement: ElementHandle;

  constructor(page: Page, xpath: string = Selector.defaultDialog) {
    super(page, xpath);
  }

  async getContent(): Promise<string> {
    await this.findDialog();
    const modalText = await (await this.dialogElement.getProperty('innerText')).jsonValue();
    console.debug('dialog: \n' + modalText);
    return modalText.toString();
  }

  async clickButton(buttonLabel: ButtonLabel): Promise<void> {
    const button = await this.waitForButton(buttonLabel);
    return button.click();
  }

  async waitForButton(buttonLabel: ButtonLabel): Promise<ElementHandle> {
    const selector = xPathOptionToXpath({name: buttonLabel, type: ElementType.Button}, this);
    return this.page.waitForXPath(selector, {visible: true});
  }

  async waitUntilDialogIsClosed(): Promise<void> {
    await this.page.waitForXPath(this.xpath, {visible: false});
  }

  async findDialog(): Promise<ElementHandle> {
    if (this.dialogElement === undefined) {
      this.dialogElement = await this.page.waitForXPath(this.xpath, {visible: true});
    }
    return this.dialogElement;
  }

  async waitUntilVisible(): Promise<void> {
    await this.page.waitForXPath(this.xpath, {visible: true});
  }

  /**
   * Returns true if Dialog exists on page.
   */
  async exists(): Promise<boolean> {
    return (await this.page.$x(this.xpath)).length > 0;
  }

}
