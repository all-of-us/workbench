import { Page } from 'puppeteer';
import { PageUrl } from 'app/text-labels';
import Link from 'app/element/link';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle, waitWhileLoading, waitWhileSpinnerDisplayed } from 'utils/waits-utils';
import WorkspacesPage from './workspaces-page';
import { logger } from 'libs/logger';

export const PageTitle = 'Homepage';

export const LabelAlias = {
  ArchivedWorkspacesPrefix: 'You have '
};

export default class HomePage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    const waitFor = async (timeout: number): Promise<void> => {
      // Wait for non-blank page before checking for spinner
      await waitForDocumentTitle(this.page, PageTitle);
      await waitWhileLoading(this.page);

      // Homepage is ready when the active workspaces panel and a key panel header are visible.
      await this.page.waitForXPath('//*[@data-test-id="active-workspaces-panel"]', { timeout, visible: true });
      await this.getSeeAllWorkspacesLink().waitUntilEnabled();
      await Promise.race([
        this.page.waitForXPath('//h2[normalize-space()="Announcements"]', { timeout, visible: true }),
        this.page.waitForXPath('//h2[normalize-space()="Resources"]', { timeout, visible: true }),
        this.page.waitForXPath('//h2[normalize-space()="Event Calendar"]', { timeout, visible: true })
      ]);

      // A secondary spinner can appear while additional homepage requests complete.
      await waitWhileSpinnerDisplayed(this.page);
    };

    try {
      await waitFor(60 * 1000);
    } catch (err) {
      logger.error('RETRY loading Home page in 5 seconds after failure.');
      logger.error(err);
      await this.page.waitForTimeout(5000);
      await waitFor(30 * 1000);
    }
    return true;
  }

  async exists(): Promise<boolean> {
    return (await this.page.title()).includes(PageTitle);
  }

  /**
   * Load Home page and ensure page load is completed.
   */
  async load(): Promise<this> {
    await this.loadPage({ url: PageUrl.Home });
    return this;
  }

  getSeeAllWorkspacesLink(): Link {
    return Link.findByName(this.page, { startsWith: LabelAlias.ArchivedWorkspacesPrefix });
  }

  async goToAllWorkspacesPage(): Promise<WorkspacesPage> {
    await this.getSeeAllWorkspacesLink().clickAndWait();
    const workspacesPage = new WorkspacesPage(this.page);
    await workspacesPage.waitForLoad();
    return workspacesPage;
  }
}
