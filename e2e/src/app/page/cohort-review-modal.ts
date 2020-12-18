import {Page} from 'puppeteer';
import Modal from 'src/app/component/modal';
import {waitForText, waitWhileLoading} from 'utils/waits-utils';
import InputNumber from 'src/app/element/input-number';

const title = 'Create Review Set';

export default class CohortReviewModal extends Modal {

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    const titleXpath = `${this.getXpath()}/div[normalize-space()="${title}"]`;
    await Promise.all([
      waitForText(this.page, titleXpath),
      waitWhileLoading(this.page),
    ]);
    return true;
  }

  async fillInNumberOfPartcipants(numOfparticipants: number): Promise<void> {
    const input = await InputNumber.findByName(this.page, {name: 'NUMBER OF PARTICIPANTS'}, this);
    await input.type(numOfparticipants.toString());
  }

}
