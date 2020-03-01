import AuthenticatedPage from './page-mixin/AuthenticatedPage';

const selectors = {
  pageTitle: 'Workspace Library',
  header: '//*[normalize-space(text())="Profile"]', // Researcher Workbench Workspace Library
};

export default class FeaturedWorkspacesPage extends AuthenticatedPage {

  public async isLoaded(): Promise<boolean> {
    await super.isLoaded(selectors.pageTitle);
    // change to waitfortext
    await this.puppeteerPage.waitForXPath(selectors.header, {visible: true});
    return true;
  }

  public async waitForReady(): Promise<FeaturedWorkspacesPage> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }
  

}
