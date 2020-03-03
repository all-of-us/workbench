import {Page} from 'puppeteer';
import {waitUntilFindTexts} from '../driver/waitFuncs';
import AuthenticatedPage from './page-mixin/AuthenticatedPage';
import {SideNav} from './page-mixin/SideNav';

export const FIELD_LABEL = {
  TITLE: 'Workspace Library',
  HEADER: 'Researcher Workbench Workspace Library',
};

export default class FeaturedWorkspaces extends AuthenticatedPage {

  public page: Page;
  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  public async isLoaded(): Promise<boolean> {
    await super.isLoaded(FIELD_LABEL.TITLE);
    await waitUntilFindTexts(this.puppeteerPage, FIELD_LABEL.HEADER);
    return true;
  }

  public async waitForReady(): Promise<FeaturedWorkspaces> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }
  

}

export const FeaturedWorkspacesPage = SideNav(FeaturedWorkspaces);
