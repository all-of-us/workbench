import AuthenticatedPage from './authenticatedpage';

const selectors = {
  pageTitle: 'Create Workspace',
  header: '//*[normalize-space(.)="Create a new Workspace"]',
  dataSetSelect: 'input[type=text][placeholder="Workspace Name"] + div[id] > select', // css
  workspaceName: 'input[type=text][placeholder="Workspace Name"]', // css

};

export default class WorkspaceEdit extends AuthenticatedPage {

  public async isLoaded() {
    await super.isLoaded(selectors.pageTitle);
    await this.puppeteerPage.waitForXPath(selectors.header, {visible: true});
  }

  public async waitForReady(): Promise<WorkspaceEdit> {
    await this.isLoaded();
    return this;
  }

}
