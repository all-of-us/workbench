import Container from './container';

const SELECTOR = {
  dialogRoot: '.ReactModal__Content[role="dialog"]',
  button: '[role="button"]'
}

export enum ButtonLabel {
  Confirm = 'Confirm',
  KeepEditing = 'Keep Editing',
  Cancel = 'Cancel',
}


export default class Dialog extends Container {

  constructor(page, selector?) {
    selector = selector || {xpath: '.ReactModal__Content[role="dialog"]'};
    super(page, selector);
  }

  async getContent(): Promise<string> {
    await this.findElement();
    const modalText = await this.page.evaluate((selector) => {
      const modalElement = document.querySelector(selector);
      return modalElement.innerText;
    }, SELECTOR.dialogRoot);
    console.log('dialog: ' + modalText);
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

  private getButtonSelector() {
    return `${SELECTOR.dialogRoot} ${SELECTOR.button}`;
  }

}