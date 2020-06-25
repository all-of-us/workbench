import {Page} from 'puppeteer';
import Dialog from 'app/component/dialog';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForText} from 'utils/waits-utils';
import InputNumber from 'app/element/input-number';

const title = 'Create Review Set';

export default class CohortReviewModal extends Dialog {

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    const titleXpath = `${this.getXpath()}/div[normalize-space()="${title}"]`;
    try {
      await Promise.all([
        waitForText(this.page, titleXpath),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (e) {
      console.log(`CohortReviewModal isLoaded() encountered ${e}`);
      return false;
    }
  }

  async fillInNumberOfPartcipants(numOfparticipants: number): Promise<void> {
    const input = await InputNumber.findByName(this.page, {name: 'NUMBER OF PARTICIPANTS'}, this);
    await input.type(numOfparticipants.toString());
  }

}
