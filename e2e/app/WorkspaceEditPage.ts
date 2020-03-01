import {ElementHandle, JSHandle} from 'puppeteer';
import RadioButton from './aou-elements/radiobutton';
import SelectComponent from './aou-elements/SelectComponent';
import WebComponent from './aou-elements/WebComponent';
import {findButton, findTextBox} from './aou-elements/xpath-finder';
import AuthenticatedPage from './page-mixin/AuthenticatedPage';
require('../driver/waitFuncs');


export const FIELD_LABEL = {
  TITLE: 'Create Workspaces',
  // select - Synthetic DataSet
  syntheticDataSet: 'Workspace Name',
  // button CREATE WORKSPACE
  createWorkspaceButton: 'Create Workspace',
  // button CANCEL
  cancelButton: `Cancel`,
  // input textbox - Workspace Name
  workspaceName: 'Create a new Workspace',
};

export default class WorkspaceEditPage extends AuthenticatedPage {

  public async getCreateWorkspaceButton(): Promise<ElementHandle> {
    return findButton(this.puppeteerPage, {text: FIELD_LABEL.createWorkspaceButton});
  }

  public async getCancelButton(): Promise<ElementHandle> {
    return findButton(this.puppeteerPage, {text: FIELD_LABEL.cancelButton});
  }

  public async getWorkspaceNameTextbox(): Promise<ElementHandle> {
    return findTextBox(this.puppeteerPage, {text: FIELD_LABEL.workspaceName});
  }

  public getDataSetSelectOption(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: FIELD_LABEL.syntheticDataSet});
  }

  public getResearchPurpose(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'Research purpose'});
  }

  public question1_researchPurpose(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'Research purpose'});
  }

  public question1_educationalPurpose(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'Educational Purpose'});
  }

  public question1_forProfitPurpose(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'For-Profit Purpose'});
  }

  public question1_otherPurpose(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'Other Purpose'});
  }

  public question1_diseaseFocusedResearch(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'Disease-focused research'});
  }

  public question1_populationHealth(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'Population Health/Public Health Research'});
  }

  public question1_methodsDevelopmentValidationStudy(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'Methods development/validation study'});
  }

  public question1_drugTherapeuticsDevelopmentResearch(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'Drug/Therapeutics Development Research'});
  }

  public question1_researchControl(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'Research Control'});
  }

  public question1_geneticResearch(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'Genetic Research'});
  }

  public question1_socialBehavioralResearch(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'Social/Behavioral Research'});
  }

  public question1_ethicalLegalSocialImplicationsResearch(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'Ethical, Legal, and Social Implications (ELSI) Research'});
  }

  public question2_scientificQuestionsIntendToStudy(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'What are the specific scientific question(s) you intend to study'});
  }

  public question2_scientificApproaches(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'What are the scientific approaches you plan to use for your study'});
  }

  public question2_anticipatedFindings(): WebComponent {
    return new WebComponent(this.puppeteerPage, {text: 'What are the anticipated findings from the study'});
  }


  public radioButtonRequestReviewYes(): RadioButton {
    return new RadioButton(this.puppeteerPage); // 'Yes, I would like to request a review')
  }

  public radioButtonRequestReviewNo(): RadioButton {
    return new RadioButton(this.puppeteerPage); // 'No, I have no concerns at this time'
  }

  public radioButtonNotCenterOnUnrepresentedPopulation(): RadioButton {
    return new RadioButton(this.puppeteerPage); // 'No, my study will not center on underrepresented populations.')
  }

  public async waitForReady(): Promise<WorkspaceEditPage> {
    await super.isLoaded(FIELD_LABEL.TITLE);
    return this;
  }

  /**
   * Find and Click "Create a New Workspace" button.
   */
  public async click_button_CreateNewWorkspace(): Promise<void> {

    const buttonSelectr = '//*[@role="button" and normalize-space(.)="Create a New Workspace"]';
    const button = await this.puppeteerPage.waitForXPath(buttonSelectr, { visible: true });
    await button.click();
  }


  /**
   * Find all visible Workspace names.
   */
  public async getAllWorkspaceNames(): Promise<any[]> {
    return await this.puppeteerPage.evaluate(() => {
      return Array.from(document.querySelectorAll(`*[data-test-id="workspace-card-name"]`)).map(a =>a.textContent)
    })
  }

  /**
   * Find workspace access level.
   * @param workspaceName
   */
  public async getWorkspaceAccessLevel(workspaceName: string) : Promise<JSHandle<string>> {
    const element = await this.puppeteerPage.waitForXPath(this.accessLevel(workspaceName), {visible: true});
    return await element.getProperty('innerText');
  }

  /**
   * Find element with specified workspace name on the page.
   * @param {string} workspaceName
   */
  public async getWorkspaceLink(workspaceName: string) : Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(this.workspaceLink(workspaceName));
  }

  public async waitUntilReady() {
    await this.isLoaded();
    await this.waitForSpinner();
  }

  public async isLoaded(): Promise<boolean> {
    await super.isLoaded(FIELD_LABEL.TITLE);
    await this.getWorkspaceNameTextbox();
    await this.getDataSetSelectOption();
    await new SelectComponent(this.puppeteerPage, 'Select account').getSelectedValue();
    await this.getCreateWorkspaceButton();
    return true;
  }

  private workspaceLink(workspaceName: string) {
    return `//*[@role='button'][./*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]`
  }

  private accessLevel(workspaceName: string) {
    return `//*[.//*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]/*[@data-test-id='workspace-access-level']`;
  }

}
