import {Page} from 'puppeteer';
import AuthenticatedPage from './authenticated-page';

export const PAGE = {
  TITLE: 'Workspace Library',
  HEADER: 'Researcher Workbench Workspace Library',
};

export default class FeaturedWorkspacesPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await super.isLoaded();
    await this.waitForTextExists(PAGE.HEADER);
    return true;
  }

  async waitForReady(): Promise<this> {
    await super.waitForReady();
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

}
