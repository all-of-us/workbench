import { Page } from 'puppeteer';
import { waitForPropertyExists, waitForText, waitWhileLoading } from 'utils/waits-utils';
import RadioButton from 'app/element/radiobutton';
import { Language, LinkText } from 'app/text-labels';
import Button from 'app/element/button';
import Textbox from 'app/element/textbox';
import { waitUntilChanged } from 'utils/element-utils';
import Textarea from 'app/element/textarea';
import Modal from './modal';

const modalTitle = 'New Notebook';

export default class NewNotebookModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalTitle, { xpath: this.getXpath() });
    return true;
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
    await this.name().type(notebookName);
    const radio = language === Language.Python ? this.getPythonRadioButton() : this.getRRadioButton();
    await radio.select();
    return this.clickButton(LinkText.CreateNotebook, { waitForClose: true, waitForNav: true });
  }

  name(): Textbox {
    return Textbox.findByName(this.page, { name: 'Name:' }, this);
  }

  getPythonRadioButton(): RadioButton {
    const selector = '//*[@role="dialog"]//label[contains(normalize-space(),"Python 3")]//input[@type="radio"]';
    return new RadioButton(this.page, selector);
  }

  getRRadioButton(): RadioButton {
    const selector = '//*[@role="dialog"]//label[contains(normalize-space(),"R")]//input[@type="radio"]';
    return new RadioButton(this.page, selector);
  }

  createNotebookButton(): Button {
    return Button.findByName(this.page, { name: LinkText.CreateNotebook }, this);
  }

  /**
   * Click 'See Code Preview' button. Returns code contents.
   */
  async previewCode(): Promise<string> {
    const previewButton = Button.findByName(this.page, { name: LinkText.SeeCodePreview }, this);
    const element = await previewButton.asElementHandle();
    await previewButton.click();
    await waitUntilChanged(this.page, element);
    await waitWhileLoading(this.page);

    // Find Preview Code
    const selector = `${this.getXpath()}//textarea[@data-test-id="code-text-box"]`;
    const previewTextArea = new Textarea(this.page, selector);
    // Has 'disabled' property
    await waitForPropertyExists(this.page, selector, 'disabled');
    return previewTextArea.getTextContent();
  }
}
