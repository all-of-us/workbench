import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import InputNumber from 'app/element/input-number';
import Modal from './modal';

const title = 'Create Review Set';

export default class CohortReviewModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, title, { container: this });
    return true;
  }

  async fillInNumberOfParticipants(numOfparticipants: number): Promise<void> {
    const input = InputNumber.findByName(this.page, { name: 'NUMBER OF PARTICIPANTS' }, this);
    await input.type(numOfparticipants.toString());
  }
}
