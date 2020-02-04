import {ElementHandle, JSHandle} from 'puppeteer-core';
import authenticatedpage from '../pages/authenticatedpage';
import Button from '../pages/elements/button';
import Checkbox from '../pages/elements/checkbox';
import Input from '../pages/elements/input';
import Radio from '../pages/elements/radio';
import radio from '../pages/elements/radio';
import {waitUntilNetworkIdle} from '../services/page-wait';
import ProjectPurposeQuestion from './workspace-element';

const configs = require('../config/config');
const selectors = {
  pageTitleRegex: '/(Create|View) Workspaces?/i',
   // select - Synthetic DataSet
  dataSet: 'input[type=text][placeholder="Workspace Name"] + div[id] > select', // css
   // input - Workspace Name
  workspaceName: '//*[normalize-space(@placeholder)="Workspace Name"][@type="text"]',
   // input - required when Disease-focused research is checked
  nameOfDisease: '//*[normalize-space(@placeholder)="Name of Disease"][@type="text"]',
   // textarea - Other Purpose: required when Other Purpose is checked
  otherPurpose: '//*[normalize-space(text())=\'Other Purpose\']/following-sibling::*/textarea',
   // question #2 textarea (required): Provide the reason for choosing All of Us data for your investigation
   // xpath with full texts
  question2: '//*[starts-with(normalize-space(.), \'2. Provide the reason for choosing All of Us data for your investigation\')]/following-sibling::*/textarea',
   // question #3 textarea (required)
  question3: '//*[starts-with(normalize-space(.), \'3. What are the specific scientific question(s) you intend to study\')]/following-sibling::*/textarea',
   // question #4 textarea (required)
  question4: '//*[starts-with(normalize-space(.), \'4. What are your anticipated findings from this study\')]/following-sibling::*/textarea',
   // request review Yes or No radiobutton
  radio_request_review_yes: '//*[label[contains(normalize-space(.),\'Would you like to request a review of your research purpose\')]]/following-sibling::*/input[@type=\'radio\'][following-sibling::label[1]/text()=\'Yes\']',
  radio_request_review_no: '//*[label[contains(normalize-space(.),\'Would you like to request a review of your research purpose\')]]/following-sibling::*/input[@type=\'radio\'][following-sibling::label[1]/text()=\'No\']',
};

class CommonFields extends authenticatedpage {

  public async input_workspace_name(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.workspaceName, { visible: true });
  }

  public async select_dataSet(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitFor(selectors.dataSet, { visible: true });
  }

  public async checkbox_forProfitPurpose(): Promise<ElementHandle> {
    return new Checkbox(this.puppeteerPage, 'For-Profit Purpose').get();
  }

  public element_diseaseName(): ProjectPurposeQuestion {
    return new ProjectPurposeQuestion(this.puppeteerPage, 'Disease-focused research');
  }

  public element_question1_populationHealth(): ProjectPurposeQuestion {
    return new ProjectPurposeQuestion(this.puppeteerPage, 'Population Health/Public Health Research');
  }

  public element_question1_MethodsDevelopment(): ProjectPurposeQuestion {
    return new ProjectPurposeQuestion(this.puppeteerPage, 'Methods development/validation study');
  }

  public element_question1_DrugTherapeuticsDevelopment(): ProjectPurposeQuestion {
    return new ProjectPurposeQuestion(this.puppeteerPage, 'Drug/Therapeutics Development Research');
  }

  public element_question1_ResearchControl(): ProjectPurposeQuestion {
    return new ProjectPurposeQuestion(this.puppeteerPage, 'Research Control');
  }

  public element_question1_EducationalPurpose(): ProjectPurposeQuestion {
    return new ProjectPurposeQuestion(this.puppeteerPage, 'Educational Purpose');
  }

  public element_question1_GeneticResearch(): ProjectPurposeQuestion {
    return new ProjectPurposeQuestion(this.puppeteerPage, 'Genetic Research');
  }

  public element_question1_SocialBehavioralResearch(): ProjectPurposeQuestion {
    return new ProjectPurposeQuestion(this.puppeteerPage, 'Social/Behavioral Research');
  }

  public element_question1_OtherPurpose(): ProjectPurposeQuestion {
    return new ProjectPurposeQuestion(this.puppeteerPage, 'Other Purpose');
  }

  public async textarea_OtherPurpose(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.otherPurpose);
  }

   // required field
  public async textarea_question2(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.question2);
  }

// required field
  public async textarea_question3(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.question3);
  }

// required field
  public async textarea_question4(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.question4);
  }

  public async radiobutton_request_review_yes(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.radio_request_review_yes);
  }

  public async radiobutton_request_review_no(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.radio_request_review_no);
  }

}

export default class Workspaces extends CommonFields {

  public buttonCreateWorkspace = new Button(this.puppeteerPage,'Create Workspace');
  public radiobutton_question5_notFocusingSpecificPopulation = new Radio(this.puppeteerPage, 'No, I am not interested in focusing on specific population(s) in my research');

  public async waitForReady(): Promise<Workspaces> {
    await super.isLoaded(selectors.pageTitleRegex);
    return this;
  }

   /**
    * go directly to the URL of My Workspaces page
    */
  public async goURL(): Promise<Workspaces> {
    await this.puppeteerPage.goto(configs.uiBaseUrl + configs.workspacesUrlPath, {waitUntil: ['domcontentloaded','networkidle0']});
    await this.puppeteerPage.waitForXPath('//h3[normalize-space(text())="Workspaces"]', {visible: true});
    await this.waitForSpinner();
    return this;
  }

   /**
    * Find and Click "Create a New Workspace" button.
    */
  public async click_button_CreateNewWorkspace(): Promise<void> {
    const buttonSelectr = '//*[@role="button" and normalize-space(.)="Create a New Workspace"]';
    const button = await this.puppeteerPage.waitForXPath(buttonSelectr, { visible: true });
    await button.click();
    await this.waitUntilDocumentTitleMatch('Create Workspace');
    await this.puppeteerPage.waitForXPath('//*[normalize-space(.)="Create a new Workspace"]', {visible: true});
    await this.select_dataSet();
    await this.buttonCreateWorkspace.get();
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

  private workspaceLink(workspaceName: string) {
    return `//*[@role='button'][./*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]`
  }

  private accessLevel(workspaceName: string) {
    return `"//*[.//*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]/*[@data-test-id='workspace-access-level']"`;
  }

}
