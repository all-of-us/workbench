import {ElementHandle, Page} from 'puppeteer';
import Select from './aou-elements/select';
import SelectComponent from './aou-elements/select-component';
import Textbox from './aou-elements/textbox';
import WebComponent from './aou-elements/web-component';
import {findButton} from './aou-elements/xpath-finder';
import AuthenticatedPage from './authenticated-page';
const faker = require('faker/locale/en_US');

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
  NO_CONCERNS_ABOUT_STIGMATIZATION: 'No, I have no concerns at this time about potential stigmatization',
  YES_REQUEST_REVIEW: 'Yes, I would like to request a review of my research purpose',
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
    return await Textbox.forLabel(this.page, {text: FIELD_LABEL.NEW_WORKSPACE_NAME, ancestorNodeLevel: 2});
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

  /**
   * Select Data Set.
   * @param optionValue: 1 for selecting Data Set 1. 2 for Data Set 2.
   */
  async selectDataSet(optionValue: string) {
    const dataSetSelect = await Select.forLabel(this.page, {text: FIELD_LABEL.SYNTHETIC_DATASET});
    await dataSetSelect.selectOption(optionValue);
  }

  /**
   * Select Billing Account
   */
  async selectBillingAccount(account: string) {
    const billingAccountSelect = await Select.forLabel(this.page, {text: FIELD_LABEL.SELECT_BILLING});
    await billingAccountSelect.selectOption(account);
  }

  /**
   * Assumption: Checked checkbox means to expand the section, hidden questions will become visible.
   * @param yesOrNo: True means to check checkbox. False means to uncheck.
   */
  async expandResearchPurposeGroup(yesOrNo?: boolean) {
    if (yesOrNo === undefined) {
      yesOrNo = true;
    }
    // expand Disease purpose section if needed
    const researchPurpose = this.question1_researchPurpose();
    const researchPurposeCheckbox = await researchPurpose.asCheckBox();
    const is = await researchPurposeCheckbox.isChecked();
    if (yesOrNo !== is) {
      // click checkbox expands or collapses the section, reveal hidden questions contained inside.
      await researchPurposeCheckbox.check();
    }
  }

  /**
   *  Enter value in 'Disease-focused research' textbox
   * @param diseaseName
   */
  async fillOutDiseaseFocusedResearch(diseaseName?: string) {
    if (diseaseName === undefined) {
      diseaseName = 'diabetes';
    }
    const diseaseNameComponent = this.question1_diseaseFocusedResearch();
    await (await diseaseNameComponent.asCheckBox()).check();
    await (await diseaseNameComponent.asTextBox()).type(diseaseName);
    await (await diseaseNameComponent.asTextBox()).pressKeyboard('Tab', { delay: 10 });
  }

  /**
   * Enter value in Other Purpose textarea
   * @param value
   */
  async fillOutOtherPurpose(value?: string) {
    if (value === undefined) {
      value = faker.lorem.word();
    }
    // check Other-Purpose checkbox
    const otherPurpose = this.question1_otherPurpose();
    await (await otherPurpose.asCheckBox()).check(); // enables textarea
    await (await otherPurpose.asTextArea()).type(value);
  }

  /**
   * Question 6. Request for Review of Research Purpose Description
   * @param yesOrNo: True means select "Yes, Request Review" radiobutton. False means select "No, Request Review" radiobutton.
   */
  async requestForReviewRadiobutton(yesOrNo: boolean) {
    let radioComponent;
    if (yesOrNo) {
      radioComponent = new WebComponent(this.page, {textContains: FIELD_LABEL.YES_REQUEST_REVIEW});
    } else {
      radioComponent = new WebComponent(this.page, {textContains: FIELD_LABEL.NO_CONCERNS_ABOUT_STIGMATIZATION });
    }
    await (await radioComponent.asRadioButton()).select();
  }

  /**
   * Find and click the CREATE WORKSPACE (FINISH) button
   */
  async clickCreateFinishButton(): Promise<void> {
    const createButton = await this.getCreateWorkspaceButton();
    await createButton.focus(); // bring into viewport
    await Promise.all([
      createButton.click(),
      this.waitUntilNoSpinner(),
      this.page.waitForNavigation(),
    ]);
  }

}
