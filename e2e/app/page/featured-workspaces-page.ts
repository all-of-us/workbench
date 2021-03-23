import { Page } from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle, waitForText, waitWhileLoading } from 'utils/waits-utils';

export const PageHeader = 'Researcher Workbench Workspace Library';
export const PageTitle = 'Workspace Library';

export default class FeaturedWorkspacesPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      waitForDocumentTitle(this.page, PageTitle),
      waitForText(this.page, PageHeader),
      waitWhileLoading(this.page)
    ]);
    return true;
  }
}
