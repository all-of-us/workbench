import {waitUntilFindTexts} from '../driver/waitFuncs';
import AuthenticatedPage from './page-mixin/AuthenticatedPage';

export const FIELD_LABEL = {
  TITLE: 'Workspace Library',
  HEADER: 'Researcher Workbench Workspace Library',
};

export default class FeaturedWorkspacesPage extends AuthenticatedPage {

  public async isLoaded(): Promise<boolean> {
    await super.isLoaded(FIELD_LABEL.TITLE);
    await waitUntilFindTexts(this.puppeteerPage, FIELD_LABEL.HEADER);
    return true;
  }

  public async waitForReady(): Promise<FeaturedWorkspacesPage> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }
  

}
