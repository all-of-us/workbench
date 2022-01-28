import { Page } from 'puppeteer';
import { PageUrl } from 'app/text-labels';
import Link from 'app/element/link';
import AuthenticatedPage from 'app/page/authenticated-page';
import ClrIconLink from 'app/element/clr-icon-link';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import WorkspacesPage from './workspaces-page';
import { getAttrValue } from 'utils/element-utils';
import { logger } from 'libs/logger';

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
    const waitFor = async (timeout: number): Promise<void> => {
      await waitForDocumentTitle(this.page, PageTitle);
      await waitWhileLoading(this.page);
      
      const seeAllWorkspace = this.getSeeAllWorkspacesLink();
      await seeAllWorkspace.waitUntilEnabled();

      // Find either a workspace card or "Create your first workspace" msg.
      const firstWorkspaceMsgXpath = '//h2[.="Create your first workspace"]';
      const workspaceCardXpath =
        '//*[@data-test-id="workspace-card"][.//*[@data-test-id="workspace-card-name" and normalize-space(text())]]';
      const foundElement = await Promise.race([
        this.page.waitForXPath(firstWorkspaceMsgXpath, {
          timeout,
          visible: true
        }),
        this.page.waitForXPath(workspaceCardXpath, { timeout, visible: true })
      ]);

      await getAttrValue(this.page, foundElement, 'data-test-id').then((id) => {
        if (id === null) {
          logger.info('Home page is empty without workspace card');
        }
      });
      await waitWhileLoading(this.page);
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
