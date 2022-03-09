import AuthenticatedPage from './authenticated-page';
import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';

const PageTitle = 'Data Access Requirements';

export default class DataAccessRequirementsPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitForDocumentTitle(this.page, PageTitle);
    await waitWhileLoading(this.page);
    return true;
  }
}
