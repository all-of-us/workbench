import {ElementHandle, Page} from 'puppeteer';

export enum ButtonLabel {
  Confirm = 'Confirm',
  KeepEditing = 'Keep Editing',
}

const SELECTOR = {
  dialogRoot: '.ReactModal__Content[role="dialog"]',
  button: '[role="button"]'
}

export default class Dialog {
  private dialogElement: ElementHandle;

  constructor(private readonly page: Page) {

  }

  async getContent(): Promise<string> {
    await this.findDialog();
    const modalText = await this.page.evaluate((selector) => {
      const modalElement = document.querySelector(selector);
      return modalElement.innerText;
    }, SELECTOR.dialogRoot);
    console.log('dialog text = ' + modalText);
    return modalText;
  }

  async clickButton(buttonLabel: ButtonLabel): Promise<void> {
    const selector = this.getButtonSelector();
    await this.page.waitForSelector(selector, {visible: true});
    const buttons = await this.page.$$(selector);
    for (const button of buttons) {
      const propValue = await button.getProperty('textContent');
      if (await propValue.jsonValue() === buttonLabel) {
        return await button.click();
      }
    }
    throw new Error(`Failed to find button with label ${buttonLabel}`);
  }

  async waitUntilDialogIsClosed() {
    await this.page.waitForSelector(SELECTOR.dialogRoot, {visible: false});
  }

  private async findDialog() {
    if (this.dialogElement === undefined) {
      this.dialogElement = await this.page.waitForSelector(SELECTOR.dialogRoot, {visible: true});
    }
  }

  private getButtonSelector() {
    return `${SELECTOR.dialogRoot} ${SELECTOR.button}`;
  }

}