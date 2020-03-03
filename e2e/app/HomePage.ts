import {ElementHandle, Page} from 'puppeteer';
import {waitUntilFindTexts} from '../driver/waitFuncs';
import Link from './aou-elements/Link';
import {findIcon} from './aou-elements/xpath-finder';
import AuthenticatedPage from './page-mixin/AuthenticatedPage';
import {SideNav} from './page-mixin/SideNav';

const configs = require('../resources/workbench-config.js');

export const FIELD_LABEL = {
  TITLE: 'Homepage',
  HEADER: 'Workspaces',
  SEE_ALL_WORKSPACES: 'See all Workspaces',
  CREATE_NEW_WORKSPACE: 'Workspaces',
};


class Home extends AuthenticatedPage {

  public page: Page;
  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  public async isLoaded(): Promise<boolean> {
    await super.isLoaded(FIELD_LABEL.TITLE);
    await new Link(this.puppeteerPage).withLabel(FIELD_LABEL.SEE_ALL_WORKSPACES);
    await waitUntilFindTexts(this.puppeteerPage, FIELD_LABEL.HEADER);
    return true;
  }

  public async waitForReady(): Promise<Home> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  /**
   * navigate to Home page URL
   */
  public async goToURL(): Promise<void> {
    const pageUrl = configs.uiBaseUrl;
    await this.puppeteerPage.goto(pageUrl, {waitUntil: ['domcontentloaded','networkidle0']});
    await this.waitForReady();
  }

  public async getCreateNewWorkspaceLink(): Promise<ElementHandle> {
    return findIcon(this.puppeteerPage, FIELD_LABEL.CREATE_NEW_WORKSPACE, 'plus-circle');
  }

}

export const HomePage = SideNav(Home);

