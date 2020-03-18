import {ElementHandle, Page} from 'puppeteer';
import Link from './aou-elements/link';
import {findIcon} from './aou-elements/xpath-finder';
import AuthenticatedPage, {PageUrl} from './authenticated-page';

export const PAGE = {
  TITLE: 'Homepage',
  HEADER: 'Workspaces',
};

export const FIELD_LABEL = {
  SEE_ALL_WORKSPACES: 'See all Workspaces',
  CREATE_NEW_WORKSPACE: 'Workspaces',
};


export default class HomePage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await this.waitUntilTitleMatch(PAGE.TITLE);
      await this.waitForTextExists(PAGE.HEADER);
      await Link.forLabel(this.page, FIELD_LABEL.SEE_ALL_WORKSPACES);
      return true;
    } catch (e) {
      return false;
    }
  }

  async getCreateNewWorkspaceLink(): Promise<ElementHandle> {
    return findIcon(this.page, FIELD_LABEL.CREATE_NEW_WORKSPACE, 'plus-circle');
  }

  async load(): Promise<this> {
    const onPageUrl = this.page.url();
    const homeUrl = PageUrl.HOME.toString();
    if (onPageUrl !== homeUrl) {
      // only load Home URL if current page is not the Home page
      await this.gotoUrl(homeUrl);
      await this.waitForLoad();
    }
    return this;
  }

}
