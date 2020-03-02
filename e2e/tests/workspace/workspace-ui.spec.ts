import {Page} from 'puppeteer';
import WebElement from "../../app/aou-elements/WebElement";
import WorkspaceResourceCard from "../../app/page-mixin/WorkspaceCard";
import WorkspacesPage from '../../app/WorkspacesPage';

const Chrome = require('../../driver/ChromeDriver');
jest.setTimeout(60 * 1000);

describe('Workspace', () => {

  let page: Page;

  beforeEach(async () => {
    page = await Chrome.setup();
  });

  afterEach(async () => {
    await Chrome.teardown();
  });

  test('Workspace cards have same UI size', async () => {
    const workspaceCards = new WorkspaceResourceCard(page);
    const cards = await workspaceCards.getAllCardsElements();
    let width;
    let height;
    for (const card of cards) {
      const cardElem = new WebElement(page, card);
      expect(await cardElem.isVisible()).toBe(true);
      const size = await cardElem.size();
      if (width === undefined) {
        width = size.width; // Initialize width and height with first card element's size, compare with rest cards
        height = size.height;
      } else {
        expect(size.height).toEqual(height);
        expect(size.width).toEqual(width);
      }
      await card.dispose();
    }
  });

  // Click CreateNewWorkspace link on My Workpsaces page => Open Create Workspace page
  test('Click Create New Workspace link on My Workspaces page', async () => {
    const workspaces = new WorkspacesPage(page);
    await workspaces.goToURL();
    const workspaceEdit = await workspaces.clickCreateNewWorkspace();
    const workspaceNameTextbox = await workspaceEdit.getWorkspaceNameTextbox();
    expect(await workspaceNameTextbox.isVisible()).toBe(true);
  });


/*
      // Checking all fields in Research Purpose section
  test('Checking Research Purpose questions on Create page', async () => {

    const workspaces = new Workspaces(page);
    const workspaceEdit = await workspaces.createNewWorkspace();

        // expand Disease purpose section if needed
    const researchPurpose = workspaceEdit.getResearchPurpose();
    const researchPurposeCheckbox = researchPurpose.asCheckbox();
    if (!await researchPurposeCheckbox.isChecked()) {
      await (researchPurposeCheckbox.check());
      await page.waitFor(1000);
    }

        // Disease-focused research checkbox
    const diseaseName = workspaceEdit.question1_diseaseFocusedResearch();
    const diseaseNameCheckbox = diseaseName.asCheckbox();
    const diseaseNameElement = await diseaseNameCheckbox.get();
    expect(await diseaseNameElement.isVisible()).toBe(true);
    expect(await diseaseNameElement.getProperty('checked')).toBe(false);
    expect(await diseaseNameElement.isDisabled()).toBe(false);

    const diseaseNameTextbox: Textbox = diseaseName.asTextbox();
    const diseaseNameTextboxElement = await diseaseNameTextbox.focus();
    expect(await diseaseNameTextboxElement.isVisible()).toBe(true);
    expect(await diseaseNameTextboxElement.isDisabled()).toBe(true);

        // Select the checkbox (checked)
    await diseaseNameCheckbox.check();
        // TODO wait async after change or test will fail
    await page.waitFor(1000);
    expect(await diseaseNameElement.getProperty('checked')).toBe(true);
    expect(await diseaseNameTextboxElement.isDisabled()).toBe(false);

        // check rest fields
    const populationCheckbox = workspaceEdit.question1_populationHealth().asCheckbox();
    expect(await (await populationCheckbox.get()).isVisible()).toBe(true);
    expect(await (await populationCheckbox.get()).isDisabled()).toBe(false);

    const methodsCheckbox = workspaceEdit.question1_methodsDevelopmentValidationStudy().asCheckbox();
    expect(await (await methodsCheckbox.get()).isVisible()).toBe(true);
    expect(await (await methodsCheckbox.get()).isDisabled()).toBe(false);

    const drugCheckbox = workspaceEdit.question1_drugTherapeuticsDevelopmentResearch().asCheckbox();
    expect(await (await drugCheckbox.get()).isVisible()).toBe(true);
    expect(await (await drugCheckbox.get()).isDisabled()).toBe(false);

    const forProfitCheckbox = workspaceEdit.question1_forProfitPurpose().asCheckbox();
    expect(await (await forProfitCheckbox.get()).isVisible()).toBe(true);
    expect(await (await forProfitCheckbox.get()).isDisabled()).toBe(false);

    const researchCheckbox = workspaceEdit.question1_researchControl().asCheckbox();
    expect(await (await researchCheckbox.get()).isVisible()).toBe(true);
    expect(await (await researchCheckbox.get()).isDisabled()).toBe(false);

    const educationCheckbox = workspaceEdit.question1_educationalPurpose().asCheckbox();
    expect(await (await educationCheckbox.get()).isVisible()).toBe(true);
    expect(await (await educationCheckbox.get()).isDisabled()).toBe(false);

    const geneticCheckbox = workspaceEdit.question1_geneticResearch().asCheckbox();
    expect(await (await geneticCheckbox.get()).isVisible()).toBe(true);
    expect(await (await geneticCheckbox.get()).isDisabled()).toBe(false);

    const socialCheckbox = workspaceEdit.question1_socialBehavioralResearch().asCheckbox();
    expect(await (await socialCheckbox.get()).isVisible()).toBe(true);
    expect(await (await socialCheckbox.get()).isDisabled()).toBe(false);

    const otherPurpose = workspaceEdit.question1_otherPurpose();
    const otherPurposeCheckbox = otherPurpose.asCheckbox();
    expect(await (await otherPurposeCheckbox.get()).isVisible()).toBe(true);
    expect(await (await otherPurposeCheckbox.get()).isDisabled()).toBe(false);
    const otherPurposeTextarea = otherPurpose.asTextArea();
    expect(await (await otherPurposeTextarea.focus()).isDisabled()).toBe(true);

  });


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



         */
});
