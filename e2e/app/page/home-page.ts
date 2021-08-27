import { Page } from 'puppeteer';
import { PageUrl } from 'app/text-labels';
import Link from 'app/element/link';
import AuthenticatedPage from 'app/page/authenticated-page';
import ClrIconLink from 'app/element/clr-icon-link';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import WorkspacesPage from './workspaces-page';

export const PageTitle = 'Homepage';

export const LabelAlias = {
  SeeAllWorkspaces: 'See all workspaces',
  CreateNewWorkspace: 'Workspaces'
};

export default class HomePage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitForDocumentTitle(this.page, PageTitle);

    // Find "See All Workspaces" link.
    await Promise.all([
      this.getSeeAllWorkspacesLink().asElementHandle({ timeout: 120000, visible: true }),
      waitWhileLoading(this.page, 120000)
    ]);

    // Find either a workspace card or "Create your first workspace" msg.
    await this.page
      .waitForXPath('//text()[contains(., "Create your first workspace")]', { visible: true, timeout: 1000 })
      .catch(async () => {
        await this.page.waitForXPath('//*[@data-test-id="workspace-card"]', { visible: true });
        try {
          // Look for either the recent-resources table or the getting-started msg.
          await Promise.race([
            this.page.waitForXPath('//*[@data-test-id="recent-resources-table"]', { visible: true, timeout: 1000 }),
            this.page.waitForXPath('//*[@data-test-id="getting-started"]', { visible: true, timeout: 1000 })
          ]);
        } catch (err) {
          // ignore error
        }
      });
    await waitWhileLoading(this.page);
    return true;
  }

  getCreateNewWorkspaceLink(): ClrIconLink {
    return ClrIconLink.findByName(this.page, { name: LabelAlias.CreateNewWorkspace, iconShape: 'plus-circle' });
  }

  /**
   * Load Home page and ensure page load is completed.
   */
  async load(): Promise<this> {
    await this.loadPageUrl(PageUrl.Home);
    return this;
  }

  getSeeAllWorkspacesLink(): Link {
    return Link.findByName(this.page, { name: LabelAlias.SeeAllWorkspaces });
  }

  async goToAllWorkspacesPage(): Promise<WorkspacesPage> {
    await this.getCreateNewWorkspaceLink().clickAndWait();
    const workspacesPage = new WorkspacesPage(this.page);
    await workspacesPage.waitForLoad();
    return workspacesPage;
  }
}
