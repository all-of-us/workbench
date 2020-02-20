import {ElementHandle, JSHandle, Page} from 'puppeteer';
import AouElement from '../driver/AouElement';
import {waitUntilTitleMatch} from '../driver/waitFuncs';
import Button from './elements/button';
import Radio from './elements/radio';
import Widget from './elements/widget';
import authenticatedpage from './mixin/authenticatedpage';
require('../driver/waitFuncs');

const configs = require('../resources/config.js');

const selectors = {
  pageTitleRegex: '/(Create|View) Workspaces?/i',
  // select - Synthetic DataSet
  dataSet: 'input[type=text][placeholder="Workspace Name"] + div[id] > select', // css
  // input - Workspace Name
  workspaceName: '//*[normalize-space(@placeholder)="Workspace Name"][@type="text"]',
  // input - required when Disease-focused research is checked
  nameOfDisease: '//*[normalize-space(@placeholder)="Name of Disease"][@type="text"]',
  // textarea - Other Purpose: required when Other Purpose is checked
  otherPurpose: '//*[normalize-space(text())="Other Purpose"]/following-sibling::*/textarea',
  // question #2 textarea (required): Provide the reason for choosing All of Us data for your investigation
  // xpath with full texts
  question2: '//*[starts-with(normalize-space(.), \'2. Provide the reason for choosing All of Us data for your investigation\')]/following-sibling::*/textarea',
  // question #3 textarea (required)
  question3: '//*[starts-with(normalize-space(.), \'3. What are the specific scientific question(s) you intend to study\')]/following-sibling::*/textarea',
  // question #4 textarea (required)
  question4: '//*[starts-with(normalize-space(.), \'4. What are your anticipated findings from this study\')]/following-sibling::*/textarea',
  // request review Yes or No radiobutton
  radio_request_review_yes: '//*[label[contains(normalize-space(.),"Would you like to request a review of your research purpose")]]/following-sibling::*/input[@type="radio"][following-sibling::label[1]/text()="Yes"]',
  radio_request_review_no: '//*[label[contains(normalize-space(.),"Would you like to request a review of your research purpose")]]/following-sibling::*/input[@type="radio"][following-sibling::label[1]/text()="No"]',
  // button CREATE WORKSPACE
  createWorkspaceButton: '//*[@role="button"][(text()="Create Workspace")]',
};


export default class WorkspacePage extends authenticatedpage {


  public buttonCreateWorkspace = new Button(this.puppeteerPage,'Create Workspace');
  public radiobuttonQuestion5NotFocusingSpecificPopulation = new Radio(this.puppeteerPage, 'No, I am not interested in focusing on specific population(s) in my research');

  public async createWorkspaceButton(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.createWorkspaceButton, { visible: true });
  }

  public async inputTextWorkspaceName(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.workspaceName, { visible: true });
  }

  public async select_dataSet(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitFor(selectors.dataSet, { visible: true });
  }

  public question1ForProfit(): ComponentWebElement {
    return new ComponentWebElement(this.puppeteerPage, 'For-Profit Purpose');
  }

  public diseaseName(): ComponentWebElement {
    return new ComponentWebElement(this.puppeteerPage, 'Disease-focused research');
  }

  public question1PopulationHealth(): ComponentWebElement {
    return new ComponentWebElement(this.puppeteerPage, 'Population Health/Public Health Research');
  }

  public question1MethodsDevelopment(): ComponentWebElement {
    return new ComponentWebElement(this.puppeteerPage, 'Methods development/validation study');
  }

  public question1DrugTherapeuticsDevelopment(): ComponentWebElement {
    return new ComponentWebElement(this.puppeteerPage, 'Drug/Therapeutics Development Research');
  }

  public question1ResearchControl(): ComponentWebElement {
    return new ComponentWebElement(this.puppeteerPage, 'Research Control');
  }

  public question1EducationalPurpose(): ComponentWebElement {
    return new ComponentWebElement(this.puppeteerPage, 'Educational Purpose');
  }

  public question1GeneticResearch(): ComponentWebElement {
    return new ComponentWebElement(this.puppeteerPage, 'Genetic Research');
  }

  public question1SocialBehavioralResearch(): ComponentWebElement {
    return new ComponentWebElement(this.puppeteerPage, 'Social/Behavioral Research');
  }

  public question1OtherPurpose(): ComponentWebElement {
    return new ComponentWebElement(this.puppeteerPage, 'Other Purpose');
  }

  public async inputTextAreaOtherPurpose(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.otherPurpose);
  }

  // required field
  public async question2ReasonForChoosing(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.question2);
  }

// required field
  public async question3ScienficQuestionsToStudy(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.question3);
  }

// required field
  public async question4AnticipatedFindingsFromStudy(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.question4);
  }

  public async radioButtonRequestReviewYes(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.radio_request_review_yes);
  }

  public async radioButtonRequestReviewNo(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.radio_request_review_no);
  }

  public async waitForReady(): Promise<WorkspacePage> {
    await super.isLoaded(selectors.pageTitleRegex);
    return this;
  }

  /**
   * go directly to the URL of My Workspaces page
   */
  public async goURL(): Promise<WorkspacePage> {
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

  public async waitUntilPageReady() {
    await waitUntilTitleMatch(this.puppeteerPage, 'Create Workspace');
    await this.waitForSpinner();
    await this.puppeteerPage.waitForXPath('//*[normalize-space(.)="Create a new Workspace"]', {visible: true});
    await this.select_dataSet();
    await this.buttonCreateWorkspace.get();

  }

  private workspaceLink(workspaceName: string) {
    return `//*[@role='button'][./*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]`
  }

  private accessLevel(workspaceName: string) {
    return `"//*[.//*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]/*[@data-test-id='workspace-access-level']"`;
  }


}

export class ComponentWebElement extends Widget {

  public labelStr: string;

  constructor(page: Page, label: string) {
    super(page);
    this.labelStr = label;
  }

  public async checkbox(): Promise<AouElement> {
    const selectr = this.appendXpath() + '//input[@type=\'checkbox\']';
    return new AouElement(await this.puppeteerPage.waitForXPath(selectr, {visible: true}));
  }

  public async textfield(): Promise<AouElement> {
    const selectr = this.appendXpath() + '//input[@type=\'text\']';
    return new AouElement(await this.puppeteerPage.waitForXPath(selectr, {visible: true}));
  }

  public async textarea(): Promise<AouElement> {
    const selectr = this.appendXpath() + '//textarea';
    return new AouElement(await this.puppeteerPage.waitForXPath(selectr, {visible: true}));
  }

  public async label(): Promise<ElementHandle> {
    const xpath = `//label[contains(normalize-space(text()),"${this.labelStr}")]`;
    return await this.puppeteerPage.waitForXPath(xpath, {visible: true});
  }

  private appendXpath(): string {
    return `//*[child::*/label[contains(normalize-space(text()),"${this.labelStr}")]]`;
  }

}
