import AuthenticatedPage from './page-mixin/AuthenticatedPage';
const configs = require('../resources/workbench-config.js');

export const FIELD_LABEL = {
  TITLE: 'User Admin Table',
};

export default class UserAdminPage extends AuthenticatedPage {

  public async isLoaded(): Promise<boolean> {
    await super.isLoaded(FIELD_LABEL.TITLE);
    return true;
  }

  public async waitForReady(): Promise<UserAdminPage> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  /**
   * navigate to User Admin URL
   */
  public async goToURL(): Promise<void> {
    const pageUrl = configs.uiBaseUrl + configs.adminUrlPath;
    await this.puppeteerPage.goto(pageUrl, {waitUntil: ['domcontentloaded','networkidle0']});
    await this.waitForSpinner();
  }

}
