import {Page} from 'puppeteer';
import {PageUrl} from 'app/page-identifiers';
import Link from 'app/element/link';
import AuthenticatedPage from 'app/page/authenticated-page';
import ClrIconLink from 'app/element/clr-icon-link';

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
        this.waitUntilTitleMatch(PAGE.TITLE),
        this.waitForTextExists(PAGE.HEADER),
        Link.forLabel(this.page, LABEL_ALIAS.SEE_ALL_WORKSPACES),
        this.waitUntilNoSpinner(),
      ]);
      return true;
    } catch (e) {
      return false;
    }
  }

  async getCreateNewWorkspaceLink(): Promise<ClrIconLink> {
    return ClrIconLink.forLabel(this.page, {text: LABEL_ALIAS.CREATE_NEW_WORKSPACE}, 'plus-circle');
  }

  /**
   * Load Home page and ensure page load is completed.
   */
  async load(): Promise<this> {
    await this.loadPageUrl(PageUrl.HOME);
    return this;
  }

  async getSeeAllWorkspacesLink(): Promise<Link> {
    return Link.forLabel(this.page, LABEL_ALIAS.SEE_ALL_WORKSPACES);
  }

}
