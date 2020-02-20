import {ElementHandle, Page} from 'puppeteer';
import Home from '../../app/home';
import WorkspacePage from '../../app/workspace-page';
require('../../driver/puppeteerExtension');

const Chrome = require('../../driver/ChromeDriver');
jest.setTimeout(60 * 1000);

describe.skip('Edit Workspace page', () => {

  let page: Page;

  beforeEach(async () => {
    page = await Chrome.setup();
  });

  afterEach(async () => {
    await Chrome.teardown();
  });

  // Click CreateNewWorkspace link in Home page => Open Create Workspace page
  test('Home page: Click link Create-New-Workspace', async () => {
    const home = new Home(page);
    const link = await home.getCreateNewWorkspaceLink();
    expect(await link.boxModel() != null).toBe(true);
    await link.click();

    const workspace = new WorkspacePage(page);
    await workspace.waitUntilPageReady();

    // expect Workspace-Name Input text field exists and is NOT readOnly
    const nameInput = (await workspace.inputTextWorkspaceName()).asAouElement();
    expect(await nameInput.isVisible()).toBe(true);
    expect(await nameInput.isReadOnly()).toBe(false);

    // expect DataSet Select field exists
    const dataSetSelect = (await workspace.select_dataSet()).asAouElement();
    expect(await dataSetSelect.isVisible()).toBe(true);
  }, 60 * 1000);

  // Click CreateNewWorkspace link in My Workpsaces page => Open Create Workspace page
  test('My Workspaces page: Click link Create-New-Workspace', async () => {
    const workspace = new WorkspacePage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();
    await workspace.waitUntilPageReady();
  }, 60 * 1000);

  // Checking all fields out-of-box
  test('Create Workspace page: Question 1', async () => {
    const workspace = new WorkspacePage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();
    await workspace.waitUntilPageReady();

    // Disease-focused research checkbox
    const diseaseName = workspace.diseaseName();
    let cbox = (await diseaseName.checkbox());
    expect(await cbox.isVisible()).toBe(true);
    expect(await cbox.getProp('checked')).toBe(false);
    expect(await cbox.getProp('disabled')).toBe(false);
    const txtField = await diseaseName.textfield();
    expect(await txtField.isVisible()).toBe(true);
    expect(await txtField.getProp('disabled')).toBe(true);

    // Set the checkbox checked
    await page.evaluate(elem => elem.click(), await (await diseaseName.checkbox()).asElementHandle() );
    // TODO wait async for checked and disabled checking or test will fail
    await page.waitFor(1000);
    cbox = await diseaseName.checkbox();
    expect(await cbox.getProp('checked')).toBe(true);
    expect(await txtField.getProp('disabled')).toBe(false);

    // check all other fields in Question #1. What is the primary purpose of your project?
    expect(await (await workspace.question1PopulationHealth().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1MethodsDevelopment().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1DrugTherapeuticsDevelopment().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1ForProfit().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1ResearchControl().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1EducationalPurpose().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1GeneticResearch().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1SocialBehavioralResearch().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1OtherPurpose().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1OtherPurpose().textarea()).isVisible()).toBe(true);
  }, 60 * 1000);

  test('Create Workspace page: Question 2', async () => {
    const workspace = new WorkspacePage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    const tarea = (await workspace.question2ReasonForChoosing()).asAouElement();
    expect(await tarea.getProp('disabled')).toBe(false);
    expect(await tarea.getProp('value')).toEqual('');
  }, 60 * 1000);

  test('Create Workspace page: Question 3', async () => {
    const workspace = new WorkspacePage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    const tarea = (await workspace.question3ScienficQuestionsToStudy()).asAouElement();
    expect(await tarea.getProp('disabled')).toBe(false);
    expect(await tarea.getProp('value')).toEqual('');
  }, 60 * 1000);

  test('Create Workspace page: Question 4', async () => {
    const workspace = new WorkspacePage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    const tarea = (await workspace.question4AnticipatedFindingsFromStudy()).asAouElement();
    expect(await tarea.getProp('disabled')).toBe(false);
    expect(await tarea.getProp('value')).toEqual('');
  }, 60 * 1000);

  test('Create Workspace page: Question 5', async () => {
    const workspace = new WorkspacePage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    expect(await workspace.radiobuttonQuestion5NotFocusingSpecificPopulation.isChecked()).toBe(true);
  }, 60 * 1000);

  test('Create Workspace page: Question on Request Review', async () => {
    const workspace = new WorkspacePage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    expect(!! (await workspace.radioButtonRequestReviewYes())).toBe(true);
    expect(!! (await workspace.radioButtonRequestReviewNo())).toBe(true);
    expect(
       await workspace.radioButtonRequestReviewNo()
       .then(elem => elem.getProperty('checked'))
       .then(elemhandle => elemhandle.jsonValue())
    ).toBe(true);
  }, 60 * 1000);

});
