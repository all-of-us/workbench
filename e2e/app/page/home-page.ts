import {Page} from 'puppeteer';
import {PageUrl} from 'app/text-labels';
import Link from 'app/element/link';
import AuthenticatedPage from 'app/page/authenticated-page';
import ClrIconLink from 'app/element/clr-icon-link';
import {waitForDocumentTitle} from 'utils/waits-utils';
import {waitWhileLoading} from 'utils/test-utils';

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
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
        this.getSeeAllWorkspacesLink().then( (element) => element.asElementHandle()),
        // Look for either a workspace card or welcome texts.
        Promise.race([
          this.page.waitForXPath('//*[@data-test-id="workspace-card"]', {visible: true}),
          this.page.waitForXPath('//text()[contains(., "Create your first workspace")]', {visible: true}),
        ]),
      ]);
      return true;
    } catch (err) {
      console.log(`HomePage isLoaded() encountered ${err}`);
      return false;
    }
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
