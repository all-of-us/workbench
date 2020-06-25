import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import Button from '../element/button';
import AuthenticatedPage from './authenticated-page';

const PageTitle = 'Participant Detail';

export default class CohortParticipantDetailPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (e) {
      console.log(`CohortParticipantDetailPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  async getBackToReviewSetButton(): Promise<Button> {
    return Button.findByName(this.page, {name: 'Back to review set'});
  }

}
