import {Page} from 'puppeteer';
import Link from '../../app/aou-elements/link';
import DataPage from '../../app/data-page';
import WorkspacesPage from '../../app/workspaces-page';

const Chrome = require('../../driver/chrome-driver');

// set timeout globally per suite, not per test.
jest.setTimeout(2 * 60 * 1000);

describe('Workspace creation tests:', () => {
  let page: Page;

  beforeEach(async () => {
    page = await Chrome.setup();
  });

  afterEach(async () => {
    await Chrome.teardown();
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
    expect(await new Link(page).withXpath(`//a[.='${workspaceName}' and @href]`, {visible: true})).toBeTruthy();
  });


});
