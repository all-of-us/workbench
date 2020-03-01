import {waitUntilTitleMatch} from '../driver/waitFuncs';
import {findButton} from './aou-elements/xpath-finder';
import AuthenticatedPage from './page-mixin/AuthenticatedPage';
import WorkspaceEditPage from './WorkspaceEditPage';

const configs = require('../resources/workbench-config.js');

export const FIELD_LABEL = {
  TITLE: 'View Workspaces',
  CREATE_NEW_WORKSPACE: 'Create a New Workspace',
};

export default class WorkspacesPage extends AuthenticatedPage {
   /**
    * navigate to My Workspaces URL
    */
  public async navigateToURL(): Promise<void> {
    const pageUrl = configs.uiBaseUrl + configs.workspacesUrlPath;
    await this.puppeteerPage.goto(pageUrl, {waitUntil: ['domcontentloaded','networkidle0']});
    await this.waitForSpinner();
  }

  public async isLoaded(): Promise<boolean> {
    await waitUntilTitleMatch(this.puppeteerPage, FIELD_LABEL.TITLE);
    await this.puppeteerPage.waitForXPath('//h3[normalize-space(text())="Workspaces"]', {visible: true});
    await this.waitForSpinner();
    return true;
  }

  public async waitForReady(): Promise<WorkspacesPage> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  public async getCreateNewWorkspaceLink() {
    return findButton(this.puppeteerPage, {text: FIELD_LABEL.CREATE_NEW_WORKSPACE});
  }



   // tests helpers: combined a number of steps in one function

   /**
    * Perform following steps:
    *
    * 1: go to My Workspaces page
    * 2: click Create New Workspace link
    * 3: wait until Edit page is loaded and ready
    * 4: return
    */
  public async createNewWorkspace(): Promise<WorkspaceEditPage> {
    await this.navigateToURL();
    const link = await this.getCreateNewWorkspaceLink();
    await Promise.all([
      this.puppeteerPage.waitForNavigation( { waitUntil: 'networkidle0' } ),
      link.click()
    ]);
    const workspaceEdit = new WorkspaceEditPage(this.puppeteerPage);
    await workspaceEdit.waitUntilReady();
    return workspaceEdit;
  }

}
