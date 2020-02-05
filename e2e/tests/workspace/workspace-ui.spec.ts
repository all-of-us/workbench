import Workspaces from '../../app/Workspaces';
import Home from '../../pages/home';
import { getProperty } from '../../services/element-handler';
const Chrome = require('../../browser/ChromeBrowser');

jest.setTimeout(60 * 1000);

describe('Workspace-editing page', () => {

  let page;

  beforeEach(async () => {
    page = await Chrome.setup();
  });

  afterEach(async () => {
    await Chrome.teardown();
  });

  test('Can Create new workspace from Home page', async () => {
    const home = new Home(page);
    const link = await home.getCreateNewWorkspaceLink();
    expect(link).toBeTruthy();
    await link.click();

    // check page title
    const actPageTitle = await page.title();
    expect(actPageTitle).toMatch('Create Workspace');

    const workspace = new Workspaces(page);
    // check Workspace-Name textfield
    expect(await workspace.input_workspace_name()).toBeTruthy();
    // check dataset select options
    expect(await workspace.select_dataSet()).toBeTruthy();
  }, 60 * 1000);


  test('Create new workspace page', async () => {
    const workspaces = new Workspaces(page);
    await workspaces.goURL();
    await workspaces.click_button_CreateNewWorkspace();

    // check page title
    const actPageTitle = await page.title();
    expect(actPageTitle).toMatch('Create Workspace');

    const workspace = new Workspaces(page);

    // webelement Disease-focused research: out-of-box
    const elementDiseaseName = await workspace.element_diseaseName();
    const diseaseNameCheckbox = await elementDiseaseName.checkbox();
    expect(diseaseNameCheckbox).toBeTruthy();
    expect(await getProperty(page, diseaseNameCheckbox, 'checked')).toBeFalsy();
    expect(await getProperty(page, diseaseNameCheckbox, 'disabled')).toBeFalsy();

    const diseaseNameTextInput = await elementDiseaseName.textfield();
    expect(diseaseNameTextInput).toBeTruthy();
    expect(await getProperty(page, diseaseNameTextInput, 'disabled')).toBeTruthy();

    // await (diseaseNameCheckbox).click(); // click checkbox to change
    await page.evaluate(elem => elem.click(), diseaseNameCheckbox);
    // TODO wait async for checked and disabled checking or test will fail
    await page.waitFor(1000);
    expect(await getProperty(page, await elementDiseaseName.checkbox(), 'checked')).toBeTruthy();
    expect(await getProperty(page, await elementDiseaseName.checkbox(), 'disabled')).toBeFalsy();


    // check Workspace Name textfield
    expect(await workspace.input_workspace_name()).toBeTruthy();

    // check dataset select option
    expect(await workspace.select_dataSet()).toBeTruthy();

    // check all fields in Question #1. What is the primary purpose of your project?
    expect(await workspace.element_question1_populationHealth().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_MethodsDevelopment().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_DrugTherapeuticsDevelopment().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_ResearchControl().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_EducationalPurpose().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_GeneticResearch().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_SocialBehavioralResearch()).toBeTruthy();
    expect(await workspace.element_question1_OtherPurpose().checkbox()).toBeTruthy();
    expect(await workspace.element_question1_OtherPurpose().textarea()).toBeTruthy();
    expect(await workspace.textarea_question2()).toBeTruthy();
    expect(await workspace.textarea_question3()).toBeTruthy();
    expect(await workspace.textarea_question4()).toBeTruthy();

    // Question #5 out-of-box behavior
    expect(await workspace.radiobutton_question5_notFocusingSpecificPopulation.get()).toBeTruthy();
    expect(await workspace.radiobutton_question5_notFocusingSpecificPopulation.isChecked()).toBeTruthy();

    // Question: Request a review ... out-of-box behavior
    expect(await workspace.radiobutton_request_review_yes()).toBeTruthy();
    expect(await workspace.radiobutton_request_review_no()).toBeTruthy();
    expect(
       await workspace.radiobutton_request_review_no()
        .then(elem => elem.getProperty('checked'))
        .then(elemhandle => elemhandle.jsonValue())
    ).toBeTruthy();

  }, 60 * 1000);



});
