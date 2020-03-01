import WorkspacesPage from '../../app/WorkspacesPage';

const Chrome = require('../../driver/ChromeDriver');

jest.setTimeout(5 * 60 * 1000);

describe('Workspace create:', () => {

  let page;

  beforeEach(async () => {
    page = await Chrome.setup();
  });

  afterEach(async () => {
    await Chrome.teardown();
  });

  test('Create simple workspace with default values', async () => {
    const workspaceName = `aoutest-${Math.floor(Math.random() * 1000)}-${Math.floor(Date.now() / 1000)}`;
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.createWorkspace(workspaceName, 'Use All of Us free credits',);

  }, 2 * 60 * 1000);


});
