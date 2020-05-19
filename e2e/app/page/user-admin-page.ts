import {Page} from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForPageContainsText} from 'utils/wait-utils';

export const PAGE = {
  TITLE: 'User Admin Table',
};

export default class UserAdminPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForPageContainsText(this.puppeteerPage, PAGE.TITLE),
        waitWhileLoading(this.puppeteerPage)
      ]);
      return true;
    } catch (err) {
      console.log(`UserAdminPage isLoaded() encountered ${err}`);
      return false;
    }
  }


}
