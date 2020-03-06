import {Page} from 'puppeteer';
import AuthenticatedPage from './authenticated-page';
const configs = require('../resources/workbench-config.js');


export const PAGE = {
  TITLE: 'User Admin Table',
};

export default class AdminPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await this.waitForTextExists(PAGE.TITLE);
    return true;
  }

  async waitForReady(): Promise<this> {
    await super.waitForReady();
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  /**
   * navigate to User Admin URL
   */
  async goToUrl(): Promise<void> {
    const pageUrl = configs.uiBaseUrl + configs.adminUrlPath;
    await this.page.goto(pageUrl, {waitUntil: ['domcontentloaded','networkidle0']});
    await this.waitForSpinner();
  }

}
