import {ElementHandle, Page} from 'puppeteer';
import {defaultFieldValues} from '../resources/data/user-registration-fields';
import Button from './aou-elements/button';
import Checkbox from './aou-elements/checkbox';
import ClrIconLink from './aou-elements/clr-icon-link';
import SelectComponent from './aou-elements/select-component';
import Textarea from './aou-elements/textarea';
import Textbox from './aou-elements/textbox';
import BasePage from './base-page';

const configs = require('../resources/workbench-config');
const faker = require('faker/locale/en_US');

export const PAGE = {
  TITLE: 'Sign In',
};

export const INSTITUTION_VALUE = {
  VANDERBILT: 'Vanderbilt University Medical Center',
  BROAD: 'Broad Institute',
  VERILY: 'Verily LLC',
  NATIONAL_INSTITUTE_HEALTH: 'National Institute of Health',
  WONDROS: 'Wondros'
};

export const INSTITUTION_ROLE_VALUE = {
  EARLY_CAREER_TENURE_TRACK_RESEARCHER: 'Early career tenure-track researcher',
  UNDERGRADUATE_STUDENT: 'Undergraduate (Bachelor level) student',
  INDUSTRY: 'Industry',
  RESEARCH_ASSISTANT: 'Research Assistant (pre-doctoral)',
  RESEARCH_ASSOCIATE: 'Research associate (post-doctoral; early/mid career)',
  SENIOR_RESEARCHER: 'Senior Researcher (PI/Team Lead, senior scientist)',
};

export const EDUCATION_LEVEL = {
  DOCTORATE: 'Doctorate',
};

export const FIELD_LABEL = {
  READ_UNDERSTAND_PRIVACY_STATEMENT: 'I have read and understand the All of Us Research Program Privacy Statement',
  READ_UNDERSTAND_TERMS_OF_USE: 'I have read and understand the All of Us Research Program Terms of Use',
  INSTITUTION_NAME: 'Institution Name',
  ARE_YOU_AFFILIATED: 'Are you affiliated with an Academic Research Institution',
  RESEARCH_BACKGROUND: 'describe your research background, experience, and research interests',
  EDUCATION_LEVEL: 'Highest Level of Education Completed', // Highest Level of Education Completed
  YEAR_OF_BIRTH: 'Year of Birth',
  INSTITUTION_EMAIL: 'institutional email address',
};

export default class CreateAccountPage extends BasePage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await this.waitForTextExists(PAGE.TITLE);
    return true;
  }

  async getInvitationKeyInput(): Promise<Textbox> {
    const textbox = new Textbox(this.page);
    await textbox.withCss('#invitationKey');
    return textbox;
  }

  async getSubmitButton(): Promise<Button> {
    return await Button.forLabel(this.page, {text: 'Submit'});
  }

  async getNextButton(): Promise<Button> {
    return await Button.forLabel(this.page, {text: 'Next'});
  }

  async scrollToLastPdfPage(): Promise<ElementHandle> {
    const selector = '.react-pdf__Document :last-child.react-pdf__Page.tos-pdf-page';
    const pdfPage = await this.page.waitForSelector(selector);
    await this.page.evaluate(el => el.scrollIntoView(), pdfPage);
    return pdfPage;
  }

  async getPrivacyStatementCheckbox(): Promise<Checkbox> {
    return await Checkbox.forLabel(this.page, {normalizeSpace: FIELD_LABEL.READ_UNDERSTAND_PRIVACY_STATEMENT});
  }

  async getTermsOfUseCheckbox(): Promise<Checkbox> {
    return await Checkbox.forLabel(this.page, {normalizeSpace: FIELD_LABEL.READ_UNDERSTAND_TERMS_OF_USE});
  }

  async getInstitutionNameInput(): Promise<Textbox> {
    return await Textbox.forLabel(this.page, {text: FIELD_LABEL.INSTITUTION_NAME});
  }

  async getResearchBackgroundTextarea(): Promise<Textarea> {
    return await Textarea.forLabel(this.page, {normalizeSpace: FIELD_LABEL.RESEARCH_BACKGROUND});
  }

  async getUsernameDomain(): Promise<unknown> {
    const elem = await this.page.waitForXPath('//*[./input[@id="username"]]/i');
    return await (await elem.getProperty('innerText')).jsonValue();
  }

  async fillInFormFields(fields: { label: string; value: string; }[]): Promise<string> {
    let newUserName;
    for (const field of fields) {
      const textbox = await Textbox.forLabel(this.page, {text: field.label});
      await textbox.type(field.value);
      await textbox.pressKeyboard('Tab', { delay: 100 });
      if (field.label === 'New Username') {
        await ClrIconLink.forLabel(this.page, 'New Username', 'success-standard');
        newUserName = field.value; // store new username for return
      }
    }
    return newUserName;
  }

  // select Institution Affiliation from a dropdown
  async selectInstitution(selectTextValue: string) {
    const dropdown = new SelectComponent(this.page);
    await dropdown.select(selectTextValue);
  }

  async getInstitutionValue() {
    const dropdown = new SelectComponent(this.page);
    return await dropdown.getSelectedValue();
  }

  // select Education Level from a dropdown
  async selectEducationLevel(selectTextValue: string) {
    const dropdown = new SelectComponent(this.page, FIELD_LABEL.EDUCATION_LEVEL, 2);
    await dropdown.select(selectTextValue);
  }

  // select Year of Birth from a dropdown
  async selectYearOfBirth(year: string) {
    const dropdown = new SelectComponent(this.page, FIELD_LABEL.YEAR_OF_BIRTH, 2);
    await dropdown.select(year);
  }

  // Combined steps to make test code cleaner and shorter

  // Step 1: Fill out Institution
  async fillOutInstitution() {
    const institutionSelect = new SelectComponent(this.page, 'Select your institution');
    await institutionSelect.select(INSTITUTION_VALUE.BROAD);
    const emailAddress = await Textbox.forLabel(this.page, {textContains: FIELD_LABEL.INSTITUTION_EMAIL, ancestorNodeLevel: 2});
    await emailAddress.type(configs.contactEmail);
    const roleSelect = new SelectComponent(this.page, 'describes your role');
    await roleSelect.select(INSTITUTION_ROLE_VALUE.UNDERGRADUATE_STUDENT);
  }

  // Step 2: Enter Invitation key
  async fillOutInvitationKey(invitationKey: string) {
    await this.getInvitationKeyInput()
    .then(invitationKeyInput => invitationKeyInput.type(invitationKey))
    .then(() => this.getNextButton())
    .then(submitButton => submitButton.click());
  }

  // Step 3: Accepting Terms of Use and Privacy statement.
  async acceptTermsOfUseAgreement() {
    await this.getPrivacyStatementCheckbox();
    await this.getTermsOfUseCheckbox();
    await this.getNextButton();

    await this.scrollToLastPdfPage();

    // check by click on label works
    await (await this.getPrivacyStatementCheckbox()).check();
    await (await this.getTermsOfUseCheckbox()).check();
  }

  // Step 3: Enter user default information
  async fillOutUserInformation() {
    const newUserName = await this.fillInFormFields(defaultFieldValues);
    await (await this.getResearchBackgroundTextarea()).type(faker.lorem.word());
    return newUserName;
  }

  // Step 4: Enter demographic survey default information (All Survey Fields are optional)
  async fillOutDemographicSurvey() {
    const demograpicsSurveyPageHeader = 'Optional Demographics Survey';
    await this.waitForTextExists(demograpicsSurveyPageHeader);
    // Find and check on all checkboxes with same label: Prefer not to answer
    const targetXpath = '//*[normalize-space(text())="Prefer not to answer"]/ancestor::node()[1]/input[@type="checkbox"]';
    await this.page.waitForXPath(targetXpath, { visible: true });
    const checkboxes = await this.page.$x(targetXpath);
    for (const ck of checkboxes) {
      await ck.click();
    }
    await this.selectYearOfBirth('1955');
    await this.selectEducationLevel(EDUCATION_LEVEL.DOCTORATE);
  }

}
