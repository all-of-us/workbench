import Home from '../../app/home';
import Workspaces from "../../app/workspace-page";
const Chrome = require('../../driver/ChromeDriver');

jest.setTimeout(60 * 1000);

const configs = require('../../config/config');

// unfinished
describe('Workspace creation tests:', () => {

  let page;

  beforeEach(async () => {
    page = await Chrome.setup();
  });

  afterEach(async () => {
    await Chrome.teardown();
  });

  test('Create new workspace with default values', async () => {
    const home = new Home(page);
    const link = await home.getCreateNewWorkspaceLink();
    await link.click();

    const workspace = new Workspaces(page);
    await workspace.createWorkspacePageReady();

    await page.waitForXPath('//label[contains(normalize-space(text()),"Use All of Us free credits")]');
    const workspaceName = `aoutest-${Math.floor(Math.random() * 1000)}-${Math.floor(Date.now() / 1000)}`;
    await (await workspace.inputTextWorkspaceName()).type(workspaceName);
    await page.keyboard.press('Tab', { delay: 100 });

    const diseaseName = workspace.element_diseaseName();
    await (await diseaseName.getLabel()).click();
    await (await diseaseName.textfield()).type('diabetes');

    const drugTherapeuticsDevelopment = workspace.element_question1_DrugTherapeuticsDevelopment();
    await page.evaluate(elem => elem.click(), (await drugTherapeuticsDevelopment.checkbox()).asElement() );

    await page.waitFor(30000);

  });

  test.skip('Create new workspace with explicit values', async () => {
    const home = new Home(page);
    const link = await home.getCreateNewWorkspaceLink();
    await link.click();

    const workspace = new Workspaces(page);
    const workspaceName = `aoutest-${Math.floor(Math.random() * 1000)}-${Math.floor(Date.now() / 1000)}`;
    await (await workspace.inputTextWorkspaceName()).type(workspaceName);
    await (await workspace.select_dataSet()).select("2");
    await page.waitFor(30000);

  });

});
