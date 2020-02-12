import {ElementHandle, Page} from 'puppeteer';
import Home from '../../app/home';
import Workspaces from '../../app/workspace-page';
import AouElement from '../../driver/AouElement'
require('../../driver/puppeteer-element-extension');

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
  test.skip('Home page: Click link Create New Workspace', async () => {
    const home = new Home(page);
    const link = await home.getCreateNewWorkspaceLink();
    expect(link).toBeTruthy();
    await link.click();

    // check page title
    const actPageTitle = await page.title();
    expect(actPageTitle).toMatch('Create Workspace');

    const workspace = new Workspaces(page);
    // check Workspace-Name Input text field
    const nameInput = await workspace.inputTextWorkspaceName();
    // expect field exists and is NOT readOnly
    const elem = new AouElement(nameInput);
    expect(await elem.isVisible()).toBe(true);
    expect(await elem.getProperty('readOnly')).toBe(false);

    // check DataSet Select field
    const dataSetSelect = (await workspace.select_dataSet()).asAouElement();
    // expect field exists
    expect(await dataSetSelect.isVisible()).toBe(true);

  }, 60 * 1000);

  // Click CreateNewWorkspace link in My Workpsaces page => Open Create Workspace page
  test.skip('My Workspaces page: Click link Create New Workspace', async () => {
    const workspaces = new Workspaces(page);
    await workspaces.goURL();
    await workspaces.click_button_CreateNewWorkspace();

  }, 60 * 1000);

  // Checking all fields out-of-box
  test('Create Workspace page: Question 1', async () => {
    const workspaces = new Workspaces(page);
    await workspaces.goURL();
    await workspaces.click_button_CreateNewWorkspace();

    const workspace = new Workspaces(page);

    // Disease-focused research checkbox
    const diseaseName = workspace.element_diseaseName();
    let cbox = (await diseaseName.checkbox());
    expect(await cbox.isVisible()).toBe(true);
    expect(await cbox.getProperty('checked')).toBe(false);
    expect(await cbox.getProperty('disabled')).toBe(false);
    const txtField = (await diseaseName.textfield());
    expect(await txtField.isVisible()).toBe(true);
    expect(await txtField.getProperty('disabled')).toBe(true);

    // Set the checkbox checked
    await page.evaluate(elem => elem.click(), await (await diseaseName.checkbox()).asElement() );
    // TODO wait async for checked and disabled checking or test will fail
    await page.waitFor(1000);
    cbox = (await diseaseName.checkbox());
    expect(await cbox.getProperty('checked')).toBe(true);
    expect(await txtField.getProperty('disabled')).toBe(false);
    await page.waitFor(30000);
    // check all fields in Question #1. What is the primary purpose of your project?
    expect(await workspace.element_question1_populationHealth().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_MethodsDevelopment().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_DrugTherapeuticsDevelopment().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_forProfitPurpose().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_ResearchControl().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_EducationalPurpose().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_GeneticResearch().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_SocialBehavioralResearch()).toBeTruthy();
    expect(await workspace.element_question1_OtherPurpose().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_OtherPurpose().textarea()).toBeTruthy();

  }, 60 * 1000);

  test.skip('Create Workspace page: Question 2', async () => {
    const workspaces = new Workspaces(page);
    await workspaces.goURL();
    await workspaces.click_button_CreateNewWorkspace();

    const workspace = new Workspaces(page);
    const tarea = (await workspace.inputTextAreaProvideReason()).asAouElement();
    expect(await tarea.getProperty('disabled')).toBe(false);
    expect(await tarea.getProperty('value')).toEqual('');

  }, 60 * 1000);

  test.skip('Create Workspace page: Question 3', async () => {
    const workspaces = new Workspaces(page);
    await workspaces.goURL();
    await workspaces.click_button_CreateNewWorkspace();

    const workspace = new Workspaces(page);

    const tarea = (await workspace.inpuTextAreaWhatScienficQuestions()).asAouElement();
    expect(await tarea.getProperty('disabled')).toBe(false);
    expect(await tarea.getProperty('value')).toEqual('');

  }, 60 * 1000);

  test.skip('Create Workspace page: Question 4', async () => {
    const workspaces = new Workspaces(page);
    await workspaces.goURL();
    await workspaces.click_button_CreateNewWorkspace();

    const workspace = new Workspaces(page);
    const tarea = (await workspace.inputTextAreaWhatAnticipatedFindings()).asAouElement();
    expect(await tarea.getProperty('disabled')).toBe(false);
    expect(await tarea.getProperty('value')).toEqual('');

  }, 60 * 1000);

  test.skip('Create Workspace page: Question 5', async () => {
    const workspaces = new Workspaces(page);
    await workspaces.goURL();
    await workspaces.click_button_CreateNewWorkspace();

    const workspace = new Workspaces(page);
    expect(await workspace.radiobuttonQuestion5NotFocusingSpecificPopulation.get()).toBeTruthy();
    expect(await workspace.radiobuttonQuestion5NotFocusingSpecificPopulation.isChecked()).toBeTruthy();

  }, 60 * 1000);

  test.skip('Create Workspace page: Question on Request Review', async () => {
    const workspaces = new Workspaces(page);
    await workspaces.goURL();
    await workspaces.click_button_CreateNewWorkspace();

    const workspace = new Workspaces(page);
    expect(await workspace.radioButtonRequestReviewYes()).toBeTruthy();
    expect(await workspace.radioButtonRequestReviewNo()).toBeTruthy();
    expect(
       await workspace.radioButtonRequestReviewNo()
       .then(elem => elem.getProperty('checked'))
       .then(elemhandle => elemhandle.jsonValue())
    ).toBeTruthy();

  }, 60 * 1000);

});
