import {ElementHandle, JSHandle, Page} from 'puppeteer';
import SelectComponent from './aou-elements/select-component';
import Textbox from './aou-elements/textbox';
import WebComponent from './aou-elements/web-component';
import {findButton} from './aou-elements/xpath-finder';
import AuthenticatedPage from './authenticated-page';


export const PAGE = {
  TITLE: 'Create Workspace',
};

export const FIELD_LABEL = {
  SYNTHETIC_DATASET: 'Workspace Name',  // select Synthetic DataSet
  SELECT_BILLING: 'Select account',   // select billing account
  CREATE_WORKSPACE: 'Create Workspace',  // button CREATE WORKSPACE
  CANCEL: `Cancel`,  // button CANCEL
  NEW_WORKSPACE_NAME: 'Create a new Workspace',  // Workspace name input textbox
  RESEARCH_PURPOSE: 'Research purpose',
  EDUCATION_PURPOSE: 'Educational Purpose',
  FOR_PROFIT_PURPOSE: 'For-Profit Purpose',
  OTHER_PURPOSE: 'Other Purpose',
  DISEASE_FOCUSED_RESEARCH: 'Disease-focused research',
  POPULATION_HEALTH: 'Population Health/Public Health Research',
  METHODS_DEVELOPMENT: 'Methods development/validation study',
  DRUG_THERAPEUTIC_DEVELOPMENT: 'Drug/Therapeutics Development Research',
  RESEARCH_CONTROL: 'Research Control',
  GENETIC_RESEARCH: 'Genetic Research',
  SOCIAL_BEHAVIORAL_RESEARCH: 'Social/Behavioral Research',
  ETHICAL_LEGAL_SOCIAL_IMPLICATIONS: 'Ethical, Legal, and Social Implications (ELSI) Research',
  INTENT_TO_STUDY: 'What are the specific scientific question(s) you intend to study',
  SCIENTIFIC_APPROACHES: 'What are the scientific approaches you plan to use for your study',
  ANTICIPATED_FINDINGS: 'What are the anticipated findings from the study',
  PUBLICATION_IN_JOURNALS: 'Publication in peer-reviewed scientific journals',
  INCREASE_WELLNESS: 'This research project seeks to increase wellness and resilience',
  NO_CONCERNS_ABOUT_STIGMATIZATION: 'No, I have no concerns at this time about potential stigmatization'
};

export default class WorkspaceEditPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await this.waitUntilTitleMatch(PAGE.TITLE);
      await this.getWorkspaceNameTextbox();
      await this.getDataSetSelectOption();
      await new SelectComponent(this.page, FIELD_LABEL.SELECT_BILLING).getSelectedValue();
      await this.getCreateWorkspaceButton();
      return true;
    } catch (e) {
      return false;
    }
  }

  async getCreateWorkspaceButton(): Promise<ElementHandle> {
    return findButton(this.page, {text: FIELD_LABEL.CREATE_WORKSPACE}, {visible: true});
  }

  async getCancelButton(): Promise<ElementHandle> {
    return findButton(this.page, {text: FIELD_LABEL.CANCEL}, {visible: true});
  }

  async getWorkspaceNameTextbox(): Promise<Textbox> {
    const textbox = new Textbox(this.page);
    await textbox.withLabel({text: FIELD_LABEL.NEW_WORKSPACE_NAME, ancestorNodeLevel: 2});
    return textbox;
  }

  getDataSetSelectOption(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.SYNTHETIC_DATASET});
  }

  question1_researchPurpose(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.RESEARCH_PURPOSE, ancestorNodeLevel: 2});
  }

  question1_educationalPurpose(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.EDUCATION_PURPOSE, ancestorNodeLevel: 2});
  }

  question1_forProfitPurpose(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.FOR_PROFIT_PURPOSE, ancestorNodeLevel: 2});
  }

  question1_otherPurpose(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.OTHER_PURPOSE, ancestorNodeLevel: 2});
  }

  question1_diseaseFocusedResearch(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.DISEASE_FOCUSED_RESEARCH, ancestorNodeLevel: 2});
  }

  question1_populationHealth(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.POPULATION_HEALTH, ancestorNodeLevel: 2});
  }

  question1_methodsDevelopmentValidationStudy(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.METHODS_DEVELOPMENT, ancestorNodeLevel: 2});
  }

  question1_drugTherapeuticsDevelopmentResearch(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.DRUG_THERAPEUTIC_DEVELOPMENT, ancestorNodeLevel: 2});
  }

  question1_researchControl(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.RESEARCH_CONTROL, ancestorNodeLevel: 2});
  }

  question1_geneticResearch(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.GENETIC_RESEARCH, ancestorNodeLevel: 2});
  }

  question1_socialBehavioralResearch(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.SOCIAL_BEHAVIORAL_RESEARCH, ancestorNodeLevel: 2});
  }

  question1_ethicalLegalSocialImplicationsResearch(): WebComponent {
    return new WebComponent(this.page, {text: FIELD_LABEL.ETHICAL_LEGAL_SOCIAL_IMPLICATIONS, ancestorNodeLevel: 2});
  }

  question2_scientificQuestionsIntendToStudy(): WebComponent {
    return new WebComponent(this.page, {textContains: FIELD_LABEL.INTENT_TO_STUDY, ancestorNodeLevel: 3});
  }

  question2_scientificApproaches(): WebComponent {
    return new WebComponent(this.page, {textContains: FIELD_LABEL.SCIENTIFIC_APPROACHES, ancestorNodeLevel: 3});
  }

  question2_anticipatedFindings(): WebComponent {
    return new WebComponent(this.page, {textContains: FIELD_LABEL.ANTICIPATED_FINDINGS, ancestorNodeLevel: 3});
  }

  // Question 3. one of many checkboxes
  publicationInJournal(): WebComponent {
    return new WebComponent(this.page, {textContains: FIELD_LABEL.PUBLICATION_IN_JOURNALS, ancestorNodeLevel: 2});
  }

  // Question 4. one of many checkboxes
  increaseWellnessResilience(): WebComponent {
    return new WebComponent(this.page, {textContains: FIELD_LABEL.INCREASE_WELLNESS, ancestorNodeLevel: 1});
  }

  noConcernAboutStigmatization(): WebComponent {
    return new WebComponent(this.page, {textContains: FIELD_LABEL.NO_CONCERNS_ABOUT_STIGMATIZATION});
  }


  async expandResearchPurposeSection() {
    // expand Disease purpose section if needed
    const researchPurpose = this.question1_researchPurpose();
    const researchPurposeCheckbox = await researchPurpose.asCheckBox();
    if (!await researchPurposeCheckbox.isChecked()) {
      await researchPurposeCheckbox.check();
      await this.page.waitFor(1000);
    }
  }

  /**
   * Find and Click "Create a New Workspace" button.
   */
  async click_button_CreateNewWorkspace(): Promise<void> {
    const buttonSelectr = '//*[@role="button" and normalize-space(.)="Create a New Workspace"]';
    const button = await this.page.waitForXPath(buttonSelectr, { visible: true });
    await button.click();
  }


  /**
   * Find all visible Workspace names.
   */
  async getAllWorkspaceNames(): Promise<any[]> {
    return await this.page.evaluate(() => {
      return Array.from(document.querySelectorAll(`*[data-test-id="workspace-card-name"]`)).map(a =>a.textContent)
    })
  }

  /**
   * Find workspace access level.
   * @param workspaceName
   */
  async getWorkspaceAccessLevel(workspaceName: string) : Promise<JSHandle<string>> {
    const element = await this.page.waitForXPath(this.accessLevel(workspaceName), {visible: true});
    return await element.getProperty('innerText');
  }

  /**
   * Find element with specified workspace name on the page.
   * @param {string} workspaceName
   */
  async getWorkspaceLink(workspaceName: string) : Promise<ElementHandle> {
    return await this.page.waitForXPath(this.workspaceLink(workspaceName));
  }

  private workspaceLink(workspaceName: string) {
    return `//*[@role='button'][./*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]`
  }

  private accessLevel(workspaceName: string) {
    return `//*[.//*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]/*[@data-test-id='workspace-access-level']`;
  }

}
