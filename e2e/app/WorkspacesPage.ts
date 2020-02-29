import {waitUntilTitleMatch} from '../driver/waitFuncs';
import {findButton} from './aou-elements/xpath-finder';
import AuthenticatedPage from './mixin-pages/AuthenticatedPage';
import WorkspaceEditPage from './WorkspaceEditPage';

const configs = require('../resources/workbench-config.js');

export default class WorkspacesPage extends AuthenticatedPage {

  public selectors = {
    pageTitle: 'View Workspaces',
      // button/link "Create a New Workspace"
    createNewWorkspaceButton: 'Create a New Workspace',
  };

   /**
    * navigate to My Workspaces URL
    */
  public async navigateToURL(): Promise<void> {
    const pageUrl = configs.uiBaseUrl + configs.workspacesUrlPath;
    await this.puppeteerPage.goto(pageUrl, {waitUntil: ['domcontentloaded','networkidle0']});
    await this.waitForSpinner();
  }

  public async isLoaded(): Promise<boolean> {
    await waitUntilTitleMatch(this.puppeteerPage, this.selectors.pageTitle);
    await this.puppeteerPage.waitForXPath('//h3[normalize-space(text())="Workspaces"]', {visible: true});
    await this.waitForSpinner();
    return true;
  }

  public async getCreateNewWorkspaceLink() {
    return findButton(this.puppeteerPage, this.selectors.createNewWorkspaceButton);
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
