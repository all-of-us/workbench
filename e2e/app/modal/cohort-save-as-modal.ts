import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import { LinkText } from 'app/text-labels';

const title = 'Save Cohort as';

export default class CohortSaveAsModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, title, { container: this });
    return true;
  }

  async typeCohortName(name: string): Promise<void> {
    const nameTextbox = this.waitForTextbox('COHORT NAME');
    await nameTextbox.type(name);
  }

  async typeDescription(description: string): Promise<void> {
    const descriptionTextarea = this.waitForTextarea('DESCRIPTION');
    await descriptionTextarea.paste(description);
  }

  async clickSaveButton(): Promise<void> {
    await this.clickButton(LinkText.Save, { waitForClose: true, timeout: 2 * 60 * 1000 });
    await waitWhileLoading(this.page);
  }
}
