import { Page } from 'puppeteer';
import { waitUntilChanged } from 'utils/element-utils';
import { makeRandomName } from 'utils/str-utils';
import { waitForPropertyExists, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import { LinkText } from 'app/text-labels';
import Modal from './modal';
import { logger } from 'libs/logger';

const modalTitleXpath =
  '//*[contains(normalize-space(),"Create Dataset") or contains(normalize-space(),"Update Dataset")]';

export default class DatasetCreateModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await this.page.waitForXPath(`${this.getXpath()}${modalTitleXpath}`, { visible: true });
    return true;
  }

  getNameTextbox(): Textbox {
    return this.waitForTextbox('DATASET NAME');
  }

  /**
   * Handle Create dialog
   */
  async createDataset(): Promise<string> {
    const newDatasetName = makeRandomName();

    const nameTextbox = this.getNameTextbox();
    await nameTextbox.clearTextInput();
    await nameTextbox.type(newDatasetName);

    await this.clickButton(LinkText.Save, { waitForClose: true, waitForNav: true });
    await waitWhileLoading(this.page);

    logger.info(`Created Dataset "${newDatasetName}"`);
    return newDatasetName;
  }

  /**
   * Click 'See Code Preview' button. Returns code contents.
   */
  async previewCode(): Promise<string> {
    // Click 'See Code Preview' button.
    const previewButton = Button.findByName(this.page, { name: LinkText.SeeCodePreview }, this);
    await previewButton.click();
    await waitUntilChanged(this.page, await previewButton.asElementHandle());

    // Find Preview Code
    const selector = `${this.getXpath()}//textarea[@data-test-id="code-text-box"]`;
    const previewTextArea = new Textarea(this.page, selector);
    // Has 'disabled' property
    await waitForPropertyExists(this.page, selector, 'disabled');
    return previewTextArea.getTextContent();
  }
}
