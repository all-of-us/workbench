import {Page} from 'puppeteer';
import Home from '../../app/home';
import WorkspacePage from '../../app/workspace-page';
import AouElement from '../../driver/AouElement';

const Chrome = require('../../driver/ChromeDriver');
jest.setTimeout(60 * 1000);

describe('Edit Workspace page', () => {

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
    const nameInput = new AouElement(await workspace.inputTextWorkspaceName());
    expect(await nameInput.isVisible()).toBe(true);
    expect(await nameInput.isReadOnly()).toBe(false);

    // expect DataSet Select field exists
    const dataSetSelect = new AouElement(await workspace.select_dataSet());
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

    // expand Disease purpose section if needed
    const expandIcon = await workspace.getResearchPurposeExpandIcon();
    if (expandIcon !== undefined) {
      await (expandIcon[0]).click();
    }
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
    await page.evaluate(elem => elem.click(), await (await diseaseName.checkbox()).asElement() );
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

    const reasonTextarea = new AouElement(await workspace.question2ScientificReason());
    expect(await reasonTextarea.getProp('disabled')).toBe(false);
    expect(await reasonTextarea.getProp('value')).toEqual('');

    const approachesTextarea = new AouElement(await workspace.question2ScientificApproaches());
    expect(await approachesTextarea.getProp('disabled')).toBe(false);
    expect(await approachesTextarea.getProp('value')).toEqual('');

    const findingsTextarea = new AouElement(await workspace.question2AnticipatedFindings());
    expect(await findingsTextarea.getProp('disabled')).toBe(false);
    expect(await findingsTextarea.getProp('value')).toEqual('');

  }, 60 * 1000);

  test.skip('Create Workspace page: Question 3', async () => {
    const workspace = new WorkspacePage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    // TODO
  }, 60 * 1000);

  test.skip('Create Workspace page: Question 4', async () => {
    const workspace = new WorkspacePage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    // TODO
  }, 60 * 1000);

  test('Create Workspace page: Question 5 Population of interest', async () => {
    const workspace = new WorkspacePage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    expect(await (workspace.radioButtonNotCenterOnUnrepresentedPopulation()).isChecked()).toBe(true);
  }, 60 * 1000);

  test('Create Workspace page: Question 6 Request for Review of Research Purpose Description', async () => {
    const workspace = new WorkspacePage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    expect(!! (await (workspace.radioButtonRequestReviewYes()).get())).toBe(true);
    expect(!! (await (workspace.radioButtonRequestReviewNo()).get())).toBe(true);
    expect(
       await (workspace.radioButtonRequestReviewNo()).get()
       .then(elem => elem.getProperty('checked'))
       .then(elemhandle => elemhandle.jsonValue())
    ).toBe(true);
  }, 60 * 1000);

});
