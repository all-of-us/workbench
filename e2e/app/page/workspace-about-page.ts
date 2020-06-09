import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForAttributeEquality, waitForDocumentTitle} from 'utils/waits-utils';
import AuthenticatedPage from './authenticated-page';
import {LabelAlias} from './data-page';

export const PageTitle = 'View Workspace Details';
export const AboutTabSelector = `//*[@id="workspace-top-nav-bar"]/*[@aria-selected="true" and @role="button" and text()="${LabelAlias.About}"]`

export default class WorkspaceAboutPage extends AuthenticatedPage{

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle, 60000),
        waitWhileLoading(this.page),
        this.page.waitForXPath(AboutTabSelector, {timeout: 60000}),
      ]);
      return true;
    } catch (err) {
      console.log(`WorkspaceAboutPage isLoaded() encountered ${err}`);
      return false;
    }
  }

  async isOpen(): Promise<boolean> {
    return waitForAttributeEquality(page, {xpath: AboutTabSelector}, 'aria-selected', 'true');
  }

}
