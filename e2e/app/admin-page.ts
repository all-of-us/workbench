import {Page} from 'puppeteer';
import AuthenticatedPage from './authenticated-page';

export const PAGE = {
  TITLE: 'User Admin Table',
};

export default class AdminPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await this.waitForTextExists(PAGE.TITLE);
      return true;
    } catch (e) {
      return false;
    }
  }


}
