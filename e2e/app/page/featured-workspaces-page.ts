import {Page} from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';

export const PAGE = {
  TITLE: 'Workspace Library',
  HEADER: 'Researcher Workbench Workspace Library',
};

export default class FeaturedWorkspacesPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await this.waitForTextExists(PAGE.HEADER);
      return true;
    } catch (e) {
      return false;
    }
  }

}
