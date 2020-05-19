import {Page} from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForPageContainsText} from 'utils/wait-utils';

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
      await Promise.all([
        waitForPageContainsText(this.puppeteerPage, PAGE.HEADER),
        waitWhileLoading(this.puppeteerPage)
      ]);
      return true;
    } catch (err) {
      console.log(`FeaturedWorkspacesPage isLoaded() encountered ${err}`);
      return false;
    }
  }

}
