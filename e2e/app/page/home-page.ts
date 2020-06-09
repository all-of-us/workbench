import {Page} from 'puppeteer';
import {PageUrl} from 'app/page-identifiers';
import Link from 'app/element/link';
import AuthenticatedPage from 'app/page/authenticated-page';
import ClrIconLink from 'app/element/clr-icon-link';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';

export const PAGE = {
  TITLE: 'Homepage',
  HEADER: 'Workspaces',
};

export const LABEL_ALIAS = {
  SEE_ALL_WORKSPACES: 'See all workspaces',
  CREATE_NEW_WORKSPACE: 'Workspaces',
};


export default class HomePage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PAGE.TITLE, 60000),
        waitWhileLoading(this.page, 60000),
      ]);
      return true;
    } catch (err) {
      console.log(`HomePage isLoaded() encountered ${err}`);
      return false;
    }
  }

  async getCreateNewWorkspaceLink(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: LABEL_ALIAS.CREATE_NEW_WORKSPACE, iconShape: 'plus-circle'});
  }

  /**
   * Load Home page and ensure page load is completed.
   */
  async load(): Promise<this> {
    await this.loadPageUrl(PageUrl.HOME);
    return this;
  }

  async getSeeAllWorkspacesLink(): Promise<Link> {
    return Link.findByName(this.page, {name: LABEL_ALIAS.SEE_ALL_WORKSPACES});
  }

}
