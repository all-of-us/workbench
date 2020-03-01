import DataPage from '../../app/DataPage';
import HomePage from '../../app/HomePage';
import WorkspaceEditPage from '../../app/WorkspaceEditPage';
import { getCursorValue } from '../../driver/element-util'

const Chrome = require('../../driver/ChromeDriver');
const faker = require('faker/locale/en_US');

jest.setTimeout(300000);

describe.skip('Workspace create:', () => {

  let page;

  beforeEach(async () => {
    page = await Chrome.setup();
  });

  afterEach(async () => {
    await Chrome.teardown();
  });

  test('Create new workspace with default values', async () => {

    const home = new HomePage(page);
    const link = await home.getCreateNewWorkspaceLink();
    await link.click();

    const workspace = new WorkspaceEditPage(page);
    await workspace.waitUntilReady();

    // wait for auto-selected value in Select billing account
    await page.waitForXPath('//label[contains(normalize-space(text()),"Use All of Us free credits")]', {visible: true});

    // CREATE WORKSPACE button should be disabled
    const createButton = await workspace.getCreateWorkspaceButton();
    let cursor = await getCursorValue(page, createButton);
    expect(cursor).toEqual('not-allowed');

    const workspaceName = `aoutest-${Math.floor(Math.random() * 1000)}-${Math.floor(Date.now() / 1000)}`;
    await (await workspace.getWorkspaceNameTextbox()).type(workspaceName);
    await (await workspace.getWorkspaceNameTextbox()).press('Tab', { delay: 100 }); // tab out

    // CREATE WORKSPACE button should be disabled
    cursor = await getCursorValue(page, createButton);
    expect(cursor).toEqual('not-allowed');

    // Enter value in 'Disease-focused research'
    const diseaseName = workspace.question1_diseaseFocusedResearch();
    await (await diseaseName.label()).click(); // click on text to set toggle checkbox
    await (await diseaseName.textfield()).type('diabetes');
    await (await diseaseName.textfield()).press('Tab');

    // CREATE WORKSPACE button should be disabled
    cursor = await getCursorValue(page, createButton);
    expect(cursor).toEqual('not-allowed');

    const drugTherapeuticsDevelopment = workspace.question1_drugTherapeuticsDevelopmentResearch();
    await (await drugTherapeuticsDevelopment.label()).click();

    // CREATE WORKSPACE button should be disabled
    cursor = await getCursorValue(page, createButton);
    expect(cursor).toEqual('not-allowed');

    const words = faker.lorem.word();
    const q2 = await workspace.question2_scientificQuestionsIntendToStudy();
    await q2.type(words);

    // CREATE WORKSPACE button should be disabled
    cursor = await getCursorValue(page, createButton);
    expect(cursor).toEqual('not-allowed');

    const q3 = await workspace.question3ScienficQuestionsToStudy();
    await q3.type(words);

    // CREATE WORKSPACE button should be disabled
    cursor = await getCursorValue(page, createButton);
    expect(cursor).toEqual('not-allowed');

    const q4 = await workspace.question4AnticipatedFindingsFromStudy();
    await q4.type(words);

    // Leave all other questions/fields unchanged

    // CREATE WORKSPACE button is enabled ONLY after all required fields are set
    await createButton.focus(); // bring into viewport
    await createButton.hover();

    cursor = await getCursorValue(page, createButton);
    expect(cursor).toEqual('pointer');

    await createButton.click();
    await (new DataPage(page)).waitUntilPageReady();
  }, 2 * 60 * 1000);

  // unfinished
  test.skip('Create new workspace with explicit values', async () => {
    const home = new HomePage(page);
    const link = await home.getCreateNewWorkspaceLink();
    await link.click();

    const workspace = new WorkspaceEditPage(page);
    const workspaceName = `aoutest-${Math.floor(Math.random() * 1000)}-${Math.floor(Date.now() / 1000)}`;
    await (await workspace.getWorkspaceNameTextbox()).type(workspaceName);
    await (await workspace.getDataSetSelectOption()).select('2');
    await page.waitFor(30000);

  }, 2 * 60 * 1000);

});
