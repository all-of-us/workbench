import SelectMenu from 'app/component/select-menu';
import Button from 'app/element/button';
import Checkbox from 'app/element/checkbox';
import ClrIconLink from 'app/element/clr-icon-link';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import {findIframe} from 'app/element/xpath-finder';
import BasePage from 'app/page/base-page';
import {ElementType} from 'app/xpath-options';
import {Frame, Page} from 'puppeteer';
import {defaultFieldValues} from 'resources/data/user-registration-data';
import {config} from 'resources/workbench-config';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForText} from 'utils/waits-utils';

const faker = require('faker/locale/en_US');

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

export const EDUCATION_LEVEL_VALUE = {
  DOCTORATE: 'Doctorate',
  // other option values here
};

export const LABEL_ALIAS = {
  READ_PRIVACY_STATEMENT: 'I have read, understand, and agree to the All of Us Program Privacy Statement',
  READ_TERMS_OF_USE: 'I have read, understand, and agree to the Terms of Use described above',
  INSTITUTION_NAME: 'Institution Name',
  ARE_YOU_AFFILIATED: 'Are you affiliated with an Academic Research Institution',
  RESEARCH_BACKGROUND: 'Your research background, experience, and research interests',
  EDUCATION_LEVEL: 'Highest Level of Education Completed', // Highest Level of Education Completed
  YEAR_OF_BIRTH: 'Year of Birth',
  INSTITUTION_EMAIL: 'Your institutional email address',
};

export const FIELD = {
  institutionEmailTextbox: {
    textOption: {
      containsText: LABEL_ALIAS.INSTITUTION_EMAIL, ancestorLevel: 2
    }
  },
  educationLevelSelect: {
    textOption: {
      type: ElementType.Dropdown, name: LABEL_ALIAS.EDUCATION_LEVEL, ancestorLevel: 2
    }
  },
  birthYearSelect: {
    textOption: {
      type: ElementType.Dropdown, name: LABEL_ALIAS.YEAR_OF_BIRTH, ancestorLevel: 2
    }
  },
  institutionSelect: {
    textOption: {
      type:ElementType.Dropdown, name:'Select your institution', ancestorLevel:2
    }
  },
  describeRole: {
    textOption: {
      type: ElementType.Dropdown, containsText: 'describes your role', ancestorLevel: 2
    }
  }
};

export default class CreateAccountPage extends BasePage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForText(this.page, 'Please read through the entire agreement to continue'),
        this.page.waitForXPath('//*[@data-test-id="account-creation-tos"]', {visible: true}),
      ])
      return true;
    } catch (err) {
      console.log(`CreateAccountPage isLoaded() encountered ${err}`);
      return false;
    }
  }

  async getSubmitButton(): Promise<Button> {
    return await Button.findByName(this.page, {name: 'Submit'});
  }

  async getNextButton(): Promise<Button> {
    return await Button.findByName(this.page, {name: 'Next'});
  }

  async agreementLoaded(): Promise<boolean> {
    const iframe = await findIframe(this.page, 'terms of service agreement');
    const bodyHandle = await iframe.$('body');
    return await iframe.evaluate(body => body.scrollHeight > 0, bodyHandle);
  }

  async readAgreement(): Promise<Frame> {
    const iframe = await findIframe(this.page, 'terms of service agreement');
    const bodyHandle = await iframe.$('body');
    await iframe.evaluate(body =>  body.scrollTo(0, body.scrollHeight), bodyHandle);
    return iframe;  
  }

  async getPrivacyStatementCheckbox(): Promise<Checkbox> {
    return await Checkbox.findByName(this.page, {normalizeSpace: LABEL_ALIAS.READ_PRIVACY_STATEMENT});
  }

  async getTermsOfUseCheckbox(): Promise<Checkbox> {
    return await Checkbox.findByName(this.page, {normalizeSpace: LABEL_ALIAS.READ_TERMS_OF_USE});
  }

  async getInstitutionNameInput(): Promise<Textbox> {
    return await Textbox.findByName(this.page, {name: LABEL_ALIAS.INSTITUTION_NAME});
  }

  async getResearchBackgroundTextarea(): Promise<Textarea> {
    return await Textarea.findByName(this.page, {normalizeSpace: LABEL_ALIAS.RESEARCH_BACKGROUND});
  }

  async getUsernameDomain(): Promise<unknown> {
    const elem = await this.page.waitForXPath('//*[./input[@id="username"]]/i');
    return await (await elem.getProperty('innerText')).jsonValue();
  }

  async fillInFormFields(fields: { label: string; value: string; }[]): Promise<string> {
    let newUserName;
    for (const field of fields) {
      const textbox = await Textbox.findByName(this.page, {name: field.label});
      await textbox.type(field.value);
      await textbox.tabKey();
      if (field.label === 'New Username') {
        await ClrIconLink.findByName(this.page, {name: field.label, iconShape: 'success-standard'});
        newUserName = field.value; // store new username for return
      }
    }
    return newUserName;
  }

  // select Institution Affiliation from a dropdown
  async selectInstitution(selectTextValue: string) {
    const dropdown = await SelectMenu.findByName(this.page, FIELD.institutionSelect.textOption);
    await dropdown.clickMenuItem(selectTextValue);
  }

  async getInstitutionValue() {
    const dropdown = await SelectMenu.findByName(this.page, FIELD.institutionSelect.textOption);
    return await dropdown.getSelectedValue();
  }

  // select Education Level from a dropdown
  async selectEducationLevel(selectTextValue: string) {
    const dropdown = await SelectMenu.findByName(this.page, FIELD.educationLevelSelect.textOption);
    await dropdown.clickMenuItem(selectTextValue);
  }

  // select Year of Birth from a dropdown
  async selectYearOfBirth(year: string) {
    const dropdown = await SelectMenu.findByName(this.page, FIELD.birthYearSelect.textOption);
    await dropdown.clickMenuItem(year);
  }

  // Combined steps to make test code cleaner and shorter

  // Step 1: Fill out institution affiliation details
  async fillOutInstitution() {
    await Promise.all([
      waitForText(this.page, 'complete Step 1 of 3', {css: 'body'}, 60000),
      waitWhileLoading(this.page, 60000),
    ]);

    await this.selectInstitution(INSTITUTION_VALUE.BROAD);
    console.log(await this.getInstitutionValue());
    const emailAddressTextbox = await Textbox.findByName(this.page, FIELD.institutionEmailTextbox.textOption);
    await emailAddressTextbox.type(config.institutionContactEmail);
    await emailAddressTextbox.tabKey(); // tab out to start email validation
    await ClrIconLink.findByName(this.page, {containsText: LABEL_ALIAS.INSTITUTION_EMAIL, ancestorLevel: 2, iconShape: 'success-standard'});

    const roleSelect = await SelectMenu.findByName(this.page, FIELD.describeRole.textOption);
    await roleSelect.clickMenuItem(INSTITUTION_ROLE_VALUE.UNDERGRADUATE_STUDENT);
  }

  // Step 3: Accepting Terms of Use and Privacy statement.
  async acceptTermsOfUseAgreement() {
    await this.getPrivacyStatementCheckbox();
    await this.getTermsOfUseCheckbox();
    await this.getNextButton();

    await this.readAgreement();

    // check by click on label works
    await (await this.getPrivacyStatementCheckbox()).check();
    await (await this.getTermsOfUseCheckbox()).check();
  }

  // Step 3: Fill out user information with default values
  async fillOutUserInformation() {
    const newUserName = await this.fillInFormFields(defaultFieldValues);
    await (await this.getResearchBackgroundTextarea()).type(faker.lorem.word());
    return newUserName;
  }

  // Step 4: Fill out demographic survey information with default values
  async fillOutDemographicSurvey() {
    await waitForText(this.page, 'Optional Demographics Survey');
    // Find and check on all checkboxes with same label: Prefer not to answer
    const targetXpath = '//*[normalize-space(text())="Prefer not to answer"]/ancestor::node()[1]/input[@type="checkbox"]';
    await this.page.waitForXPath(targetXpath, { visible: true });
    const checkboxes = await this.page.$x(targetXpath);
    for (const ck of checkboxes) {
      await ck.click();
    }
    await this.selectYearOfBirth('1955');
    await this.selectEducationLevel(EDUCATION_LEVEL_VALUE.DOCTORATE);
  }

}
