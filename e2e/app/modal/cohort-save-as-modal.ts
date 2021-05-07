import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import { LinkText } from 'app/text-labels';

const title = 'Save Cohort as';

export default class CohortSaveAsModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, title, { xpath: this.getXpath() });
    return true;
  }

  async typeCohortName(name: string): Promise<void> {
    const nameTextbox = this.waitForTextbox('COHORT NAME');
    await nameTextbox.type(name);
  }

  async typeDescription(description: string): Promise<void> {
    const descriptionTextarea = this.waitForTextarea('DESCRIPTION');
    await descriptionTextarea.type(description);
  }

  async clickSaveButton(): Promise<void> {
    await this.clickButton(LinkText.Save, { waitForNav: true, waitForClose: true, timeout: 2 * 60 * 1000 });
    await waitWhileLoading(this.page);
  }
}
