import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';

const title = 'Start planning for migration';

export default class PlanForMigrationModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, title, { container: this });
    return true;
  }

  getGotItButton(): Button {
    return Button.findByName(this.page, { name: LinkText.GotIt });
  }
}
