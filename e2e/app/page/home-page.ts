import {Page} from 'puppeteer';
import {PageUrl} from 'app/text-labels';
import Link from 'app/element/link';
import AuthenticatedPage from 'app/page/authenticated-page';
import ClrIconLink from 'app/element/clr-icon-link';
import {waitForDocumentTitle, waitWhileLoading} from 'utils/waits-utils';

export const PageTitle = 'Homepage';

export const LabelAlias = {
  SeeAllWorkspaces: 'See all workspaces',
  CreateNewWorkspace: 'Workspaces',
};


export default class HomePage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      waitForDocumentTitle(this.page, PageTitle),
      waitWhileLoading(this.page)
    ]);
    await this.getSeeAllWorkspacesLink().then( (element) => element.asElementHandle());
    // Look for either a workspace card or the "Create your first workspace" msg.
    await Promise.race([
         this.page.waitForXPath('//*[@data-test-id="workspace-card"]', {visible: true}),
         this.page.waitForXPath('//text()[contains(., "Create your first workspace")]', {visible: true}),
       ]);
    // Look for either the recent-resources table or the getting-started msg.
    await Promise.race([
         this.page.waitForXPath('//*[@data-test-id="recent-resources-table"]', {visible: true}),
         this.page.waitForXPath('//*[@data-test-id="getting-started"]', {visible: true}),
       ]);
    return true;
  }

  async getCreateNewWorkspaceLink(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: LabelAlias.CreateNewWorkspace, iconShape: 'plus-circle'});
  }

  /**
   * Load Home page and ensure page load is completed.
   */
  async load(): Promise<this> {
    await this.loadPageUrl(PageUrl.Home);
    return this;
  }

  async getSeeAllWorkspacesLink(): Promise<Link> {
    return Link.findByName(this.page, {name: LabelAlias.SeeAllWorkspaces});
  }
}
