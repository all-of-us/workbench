import {Page} from 'puppeteer';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils';
import {waitForText} from 'utils/waits-utils';
import RadioButton from 'app/element/radiobutton';
import {Language, LinkText} from 'app/text-labels';
import Button from 'app/element/button';
import Textbox from 'app/element/textbox';
import Modal from './modal';

const modalTitle = 'New Notebook';

export default class NewNotebookModal extends Modal {

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async waitForLoad(): Promise<this> {
    try {
      await waitForText(this.page, modalTitle, {xpath: this.getXpath()});
      await this.name();
      await this.python3Radiobutton();
      await this.RRadiobutton();
    } catch (e) {
      await savePageToFile(this.page);
      await takeScreenshot(this.page);
      console.error(`"New Notebook" modal waitForLoad() encountered ${e}`);
      throw e;
    }
    return this;
  }

  /**
   * Fill out New Notebook modal:
   * - Type notebook name.
   * - Select notebook language.
   * - Click Create Notebook button.
   * @param {string} notebookName Notebook name.
   * @param {Language} language Notebook language.
   */
  async fillInModal(notebookName: string, language: Language): Promise<void> {
    await this.name().then( (textbox) => textbox.type(notebookName));
    if (language === Language.Python) {
      await this.python3Radiobutton().then( (radio) => radio.select());
    } else {
      await this.RRadiobutton().then( (radio) => radio.select());
    }
    return this.clickButton(LinkText.CreateNotebook, {waitForClose: true, waitForNav: true});
  }

  async name(): Promise<Textbox> {
    return Textbox.findByName(this.page, {name: 'Name:'});
  }

  async python3Radiobutton(): Promise<RadioButton> {
    const selector = '//*[@role="dialog"]//label[contains(normalize-space(),"Python 3")]//input[@type="radio"]';
    return new RadioButton(this.page, selector);
  }

  async RRadiobutton(): Promise<RadioButton> {
    const selector = '//*[@role="dialog"]//label[contains(normalize-space(),"R")]//input[@type="radio"]';
    return new RadioButton(this.page, selector);
  }

  async createNotebookButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LinkText.CreateNotebook});
  }

}
