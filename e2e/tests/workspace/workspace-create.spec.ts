import Link from '../../app/aou-elements/link';
import DataPage from '../../app/data-page';
import WorkspacesPage from '../../app/workspaces-page';
import {signIn} from '../app';


describe.skip('Workspace creation tests:', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  afterEach(async () => {
    await jestPuppeteer.resetBrowser();
  });


  test('User can create a simple workspace with some default values', async () => {
    const workspaceName = `aoutest-${Math.floor(Math.random() * 1000)}-${Math.floor(Date.now() / 1000)}`;
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();
    await workspacesPage.createWorkspace(workspaceName, 'Use All of Us free credits',);
    // check Data page is loaded
    const dataPage = new DataPage(page);
    await dataPage.waitForLoad();
    // checking new workspace link is found
    expect(await new Link(page).withXpath(`//a[text()='${workspaceName}']`, {visible: true})).toBeTruthy();
  });


});
