import {Page} from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';

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
        this.waitForTextExists(PAGE.TITLE),
        this.waitUntilNoSpinner(),
      ]);
      return true;
    } catch (e) {
      return false;
    }
  }


}
