import {Page} from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import {waitForText} from 'utils/waits-utils';
import {waitWhileLoading} from 'utils/test-utils';

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
        waitForText(this.page, PAGE.HEADER),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (e) {
      console.log(`FeaturedWorkspacesPage isLoaded() encountered ${e}`);
      return false;
    }
  }

}
