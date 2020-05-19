import {ElementHandle, Page} from 'puppeteer';
import Container from 'app/element/container';
import {buttonXpath} from 'app/element/xpath-defaults';

export enum ButtonLabel {
  Confirm = 'Confirm',
  KeepEditing = 'Keep Editing',
}


export default class Dialog extends Container {

  private dialogElement: ElementHandle;

  constructor(private readonly puppeteerPage: Page, selector: {xpath?: string, testId?: string}) {
    super(selector);
  }

  async getContent(): Promise<string> {
    await this.findDialog();
    const modalText = await (await this.dialogElement.getProperty('innerText')).jsonValue();
    console.debug('dialog: \n' + modalText);
    return modalText.toString();
  }

  async clickButton(buttonLabel: ButtonLabel): Promise<void> {
    const selector = buttonXpath({text: buttonLabel}, this);
    const button = await this.puppeteerPage.waitForXPath(selector, {visible: true});
    return button.click();
  }

  async waitUntilDialogIsClosed(): Promise<void> {
    await this.puppeteerPage.waitForXPath(this.xpath, {visible: false});
  }

  private async findDialog(): Promise<void> {
    if (this.dialogElement === undefined) {
      this.dialogElement = await this.puppeteerPage.waitForXPath(this.xpath, {visible: true});
    }
  }


}
