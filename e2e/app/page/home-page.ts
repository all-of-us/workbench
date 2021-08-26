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
    await waitWhileLoading(this.page);
    // Look for "See All Workspaces" link.
    await this.getSeeAllWorkspacesLink().asElementHandle();
    // Look for either a workspace card or the "Create your first workspace" msg.
    const createWorkspaceXpath = '//text()[contains(., "Create your first workspace")]';
    const workspaceCardXpath = '//*[@data-test-id="workspace-card"]';
    await Promise.race([
      this.page.waitForXPath(workspaceCardXpath, { visible: true }),
      this.page.waitForXPath(createWorkspaceXpath, { visible: true })
    ]);
    // Look for either the recent-resources table or the getting-started msg.
    await Promise.race([
      this.page.waitForXPath('//*[@data-test-id="recent-resources-table"]', { visible: true, timeout: 2000 }),
      this.page.waitForXPath('//*[@data-test-id="getting-started"]', { visible: true, timeout: 2000 })
    ]).catch();
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
