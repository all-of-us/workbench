import {ElementHandle, Page} from 'puppeteer';
import Link from './aou-elements/link';
import {findIcon} from './aou-elements/xpath-finder';
import AuthenticatedPage from './authenticated-page';

const configs = require('../resources/workbench-config.js');


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
    await this.waitUntilTitleMatch(PAGE.TITLE);
    await this.waitForTextExists(PAGE.HEADER);
    await new Link(this.page).withLabel(FIELD_LABEL.SEE_ALL_WORKSPACES);
    return true;
  }

  async waitForReady(): Promise<this> {
    super.waitForReady();
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  /**
   * navigate to Home page URL
   */
  async goToUrl(): Promise<void> {
    const pageUrl = configs.uiBaseUrl;
    await this.page.goto(pageUrl, {waitUntil: ['domcontentloaded','networkidle0']});
    await this.waitForReady();
  }

  async getCreateNewWorkspaceLink(): Promise<ElementHandle> {
    return findIcon(this.page, FIELD_LABEL.CREATE_NEW_WORKSPACE, 'plus-circle');
  }

}
