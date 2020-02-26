import {Page} from 'puppeteer';
import WebElement from '../../app/elements/web-element';
import Home from '../../app/home';
import WorkspaceEditPage from '../../app/workspace-edit';

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
  test('Home page: Click Create-New-Workspace link', async () => {
    const home = new Home(page);
    const link = await home.getCreateNewWorkspaceLink();
    expect(await link.boxModel() != null).toBe(true);
    await link.click();

    const workspace = new WorkspaceEditPage(page);
    await workspace.waitUntilPageReady();

    // expect Workspace-Name Input text field exists and is NOT readOnly
    const nameInput = new WebElement(await workspace.getWorkspaceNameTextbox());
    expect(await nameInput.isVisible()).toBe(true);
    expect(await nameInput.isReadOnly()).toBe(false);

    // expect DataSet Select field exists
    const dataSetSelect = new WebElement(await workspace.getDataSetSelectOption());
    expect(await dataSetSelect.isVisible()).toBe(true);
  }, 60 * 1000);

  // Click CreateNewWorkspace link in My Workpsaces page => Open Create Workspace page
  test('My Workspaces page: Click link Create-New-Workspace', async () => {
    const workspace = new WorkspaceEditPage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();
    await workspace.waitUntilPageReady();
  }, 60 * 1000);

  // Checking all fields out-of-box
  test('Create Workspace page: Question 1', async () => {
    const workspace = new WorkspaceEditPage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();
    await workspace.waitUntilPageReady();

    // expand Disease purpose section if needed
    const expandIcon = await workspace.getResearchPurposeExpandIcon();
    if (expandIcon !== undefined) {
      await (expandIcon[0]).click();
    }
    // Disease-focused research checkbox
    const diseaseName = workspace.question1_diseaseFocusedResearch();
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
    expect(await (await workspace.question1_populationHealth().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1_methodsDevelopmentValidationStudy().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1_drugTherapeuticsDevelopmentResearch().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1_forProfitPurpose().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1_researchControl().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1_educationalPurpose().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1_geneticResearch().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1_socialBehavioralResearch().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1_otherPurpose().checkbox()).isVisible()).toBe(true);
    expect(await (await workspace.question1_otherPurpose().textarea()).isVisible()).toBe(true);
  }, 60 * 1000);

  test('Create Workspace page: Question 2', async () => {
    const workspace = new WorkspaceEditPage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    const reasonTextarea = new WebElement(await workspace.question2_scientificQuestionsIntendToStudy());
    expect(await reasonTextarea.getProp('disabled')).toBe(false);
    expect(await reasonTextarea.getProp('value')).toEqual('');

    const approachesTextarea = new WebElement(await workspace.question2_scientificApproaches());
    expect(await approachesTextarea.getProp('disabled')).toBe(false);
    expect(await approachesTextarea.getProp('value')).toEqual('');

    const findingsTextarea = new WebElement(await workspace.question2_anticipatedFindings());
    expect(await findingsTextarea.getProp('disabled')).toBe(false);
    expect(await findingsTextarea.getProp('value')).toEqual('');

  }, 60 * 1000);

  test.skip('Create Workspace page: Question 3', async () => {
    const workspace = new WorkspaceEditPage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    // TODO
  }, 60 * 1000);

  test.skip('Create Workspace page: Question 4', async () => {
    const workspace = new WorkspaceEditPage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    // TODO
  }, 60 * 1000);

  test('Create Workspace page: Question 5 Population of interest', async () => {
    const workspace = new WorkspaceEditPage(page);
    await workspace.goURL();
    await workspace.click_button_CreateNewWorkspace();

    expect(await (workspace.radioButtonNotCenterOnUnrepresentedPopulation()).isChecked()).toBe(true);
  }, 60 * 1000);

  test('Create Workspace page: Question 6 Request for Review of Research Purpose Description', async () => {
    const workspace = new WorkspaceEditPage(page);
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
