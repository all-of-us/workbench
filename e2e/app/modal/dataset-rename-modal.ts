import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import Textbox from 'app/element/textbox';
import Textarea from 'app/element/textarea';

const title = 'Enter new name for ';

export default class DatasetRenameModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, title, { xpath: this.getXpath() });
    return true;
  }

  async typeNewName(name: string): Promise<void> {
    const nameTextbox = new Textbox(this.page, `${this.getXpath()}//*[@id="new-name"]`);
    await nameTextbox.type(name);
  }

  async typeDescription(description: string): Promise<void> {
    const descriptionTextarea = Textarea.findByName(this.page, { dataTestId: 'descriptionLabel' });
    await descriptionTextarea.type(description);
  }
}
