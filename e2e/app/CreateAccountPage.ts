import {ElementHandle} from 'puppeteer';
import Button from './aou-elements/Button';
import CheckBox from './aou-elements/CheckBox';
import ClrIconLink from './aou-elements/ClrIconLink';
import Label from './aou-elements/Label';
import RadioButton from './aou-elements/RadioButton';
import SelectComponent from './aou-elements/SelectComponent';
import TextArea from './aou-elements/TextArea';
import TextBox from './aou-elements/TextBox';
import BasePage from './page-mixin/BasePage';

import {defaultFieldValues} from '../resources/data/user-registration-fields';

const faker = require('faker/locale/en_US');

export const INSTITUTION_AFFILIATION = {
  EARLY_CAREER_TENURE_TRACK_RESEARCHER: 'Early career tenure-track researcher',
  UNDERGRADUATE_STUDENT: 'Undergraduate (Bachelor level) student',
  INDUSTRY: 'Industry',
};

export const EDUCATION_LEVEL = {
  DOCTORATE: 'Doctorate',
};

export const FIELD_LABEL = {
  READ_UNDERSTAND_PRIVACY_STATEMENT: 'I have read and understand the All of Us Research Program Privacy Statement',
  READ_UNDERSTAND_TERMS_OF_USE: 'I have read and understand the All of Us Research Program Terms of Use described above',
  INSTITUTION_NAME: 'Institution Name',
  ARE_YOU_AFFILIATED: 'Are you affiliated with an Academic Research Institution',
  DESCRIBE_RESEARCH_BACKGROUND_EXPERIENCE_INTERESTS: 'Please describe your research background, experience and research interests',
  EDUCATION_LEVEL: 'Highest Level of Education Completed',
  YEAR_OF_BIRTH: 'Year of Birth',
};

export default class CreateAccountPage extends BasePage {

  public async getInvitationKeyInput(): Promise<TextBox> {
    const textbox = new TextBox(this.puppeteerPage);
    await textbox.withCss('#invitationKey');
    return textbox;
  }

  public async getSubmitButton(): Promise<Button> {
    const button = new Button(this.puppeteerPage);
    await button.withLabel({text: 'Submit'});
    return button;
  }

  public async getNextButton(): Promise<Button> {
    const button = new Button(this.puppeteerPage);
    await button.withLabel({text: 'Next'});
    return button;
  }

  public async scrollToNthPdfPage(nth: number): Promise<ElementHandle> {
    const selector = `.react-pdf__Document :nth-child(${nth}).react-pdf__Page.tos-pdf-page`;
    const pdfPage = await this.puppeteerPage.waitForSelector(selector);
    await this.puppeteerPage.evaluate(el => el.scrollIntoView(), pdfPage);
    await this.puppeteerPage.waitFor(1000);
    return pdfPage;
  }

  public async scrollToLastPdfPage(): Promise<ElementHandle> {
    const selector = '.react-pdf__Document :last-child.react-pdf__Page.tos-pdf-page';
    const pdfPage = await this.puppeteerPage.waitForSelector(selector);
    await this.puppeteerPage.evaluate(el => el.scrollIntoView(), pdfPage);
    await this.puppeteerPage.waitFor(1000);
    return pdfPage;
  }

  public async getPrivacyStatementCheckbox(): Promise<CheckBox> {
    const checkbox = new CheckBox(this.puppeteerPage);
    await checkbox.withLabel({normalizeSpace: FIELD_LABEL.READ_UNDERSTAND_PRIVACY_STATEMENT});
    return checkbox;
  }

  public async getTermsOfUseCheckbox(): Promise<CheckBox> {
    const checkbox = new CheckBox(this.puppeteerPage);
    await checkbox.withLabel({normalizeSpace: FIELD_LABEL.READ_UNDERSTAND_TERMS_OF_USE});
    return checkbox;
  }


  public async getPrivacyStatementLabel(): Promise<Label> {
    const label = new Label(this.puppeteerPage);
    await label.withLabel({normalizeSpace: FIELD_LABEL.READ_UNDERSTAND_PRIVACY_STATEMENT});
    return label;
    // return this.getCheckboxLabel(FIELD_LABEL.READ_UNDERSTAND_PRIVACY_STATEMENT);
  }

  public async getTermsOfUseLabel(): Promise<Label> {
    const label = new Label(this.puppeteerPage);
    await label.withLabel({normalizeSpace: FIELD_LABEL.READ_UNDERSTAND_TERMS_OF_USE});
    return label;
    // return this.getCheckboxLabel(FIELD_LABEL.READ_UNDERSTAND_TERMS_OF_USE);
  }

  public async getInstitutionNameInput(): Promise<TextBox> {
    const textbox = new TextBox(this.puppeteerPage);
    await textbox.withLabel({text: FIELD_LABEL.INSTITUTION_NAME});
    return textbox;
  }

  // true for Yes radiobutton. false for No.
  public async areYouAffiliatedRadioButton(yesOrNo: boolean): Promise<RadioButton> {
    let selector;
    if (yesOrNo) {
      selector = '//input[@id="show-institution-yes"]';
    } else {
      selector = '//input[@id="show-institution-no"]';
    }
    const radio = new RadioButton(this.puppeteerPage);
    await radio.withXpath(selector);
    return radio;
  }

  public async getResearchBackgroundTextarea(): Promise<TextArea> {
    const textarea = new TextArea(this.puppeteerPage);
    await textarea.withLabel({text: FIELD_LABEL.DESCRIBE_RESEARCH_BACKGROUND_EXPERIENCE_INTERESTS});
    return textarea;
  }

  public async getUsernameDomain(): Promise<unknown> {
    const elem = await this.puppeteerPage.waitForXPath('//*[input[@id="username"]]/i');
    return await (await elem.getProperty('innerText')).jsonValue();
  }

  public async fillInFormFields(fields: Array<{ label: string; value: string; }>): Promise<string> {
    let newUserName;
    for (const field of fields) {
      const textbox = new TextBox(this.puppeteerPage);
      await textbox.withLabel({text: field.label});
      await textbox.type(field.value);
      await textbox.pressKeyboard('Tab', { delay: 100 });
      if (field.label === 'New Username') {
        await new ClrIconLink(this.puppeteerPage).withLabel('New Username', 'success-standard');
        newUserName = field.value; // store new username for return
      }
    }
    return newUserName;
  }

  // select Institution Affiliation from a dropdown
  public async selectInstitution(selectTextValue: string) {
    const dropdown = new SelectComponent(this.puppeteerPage);
    await dropdown.select(selectTextValue);
  }

  public async getInstitutionValue() {
    const dropdown = new SelectComponent(this.puppeteerPage);
    return await dropdown.getSelectedValue();
  }

  // select Education Level from a dropdown
  public async selectEducationLevel(selectTextValue: string) {
    const dropdown = new SelectComponent(this.puppeteerPage, FIELD_LABEL.EDUCATION_LEVEL);
    await dropdown.select(selectTextValue);
  }

  // select Year of Birth from a dropdown
  public async selectYearOfBirth(year: string) {
    const dropdown = new SelectComponent(this.puppeteerPage, FIELD_LABEL.YEAR_OF_BIRTH);
    await dropdown.select(year);
  }

  // Combined steps to make test code cleaner and shorter

  // Step 1: Enter Invitation key
  public async fillOutInvitationKey(invitationKey: string) {
    await this.getInvitationKeyInput()
    .then(invitationKeyInput => invitationKeyInput.type(invitationKey))
    .then(() => this.getNextButton())
    .then(submitButton => submitButton.click());
  }

  // Step 2: Accepting Terms of Use and Privacy statement.
  public async acceptTermsOfUseAgreement() {
    await this.getPrivacyStatementCheckbox();
    await this.getTermsOfUseCheckbox();
    await this.getNextButton();

    await this.scrollToLastPdfPage();

    // check by click on label works
    await (await this.getPrivacyStatementLabel()).click();
    await (await this.getTermsOfUseLabel()).click();
  }

  // Step 3: Enter user default information
  public async fillOutUserInformation() {
    const newUserName = await this.fillInFormFields(defaultFieldValues);
    await (await this.getResearchBackgroundTextarea()).type(faker.lorem.word());
    // a different dropdown selection depending on Yes or No radiobutton was selected
    await (await this.areYouAffiliatedRadioButton(true)).click();
    await (await this.getInstitutionNameInput()).type(faker.company.companyName());
    await this.selectInstitution(INSTITUTION_AFFILIATION.EARLY_CAREER_TENURE_TRACK_RESEARCHER);
    await this.puppeteerPage.waitFor(1000);
    return newUserName;
  }

  // Step 4: Enter demographic survey default information (All Survey Fields are optional)
  public async fillOutDemographicSurvey() {
    // Find and check on all checkboxes with same label: Prefer not to answer
    const targetXpath = '//*[normalize-space(text())="Prefer not to answer"]/ancestor::node()[1]/input[@type="checkbox"]';
    await this.puppeteerPage.waitForXPath(targetXpath, { visible: true });
    const checkboxs = await this.puppeteerPage.$x(targetXpath);
    for (const ck of checkboxs) {
      await ck.click();
    }
    // Select year of birth 1955
    await this.selectYearOfBirth('1955');
    // Select Highest Education completed
    await this.selectEducationLevel(EDUCATION_LEVEL.DOCTORATE);
    await this.puppeteerPage.waitFor(1000);
  }

}
