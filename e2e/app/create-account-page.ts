import {ElementHandle} from 'puppeteer';
import {getCursorValue} from '../driver/elementHandle-util';
import {inputFieldsValues, institutionAffiliationValues} from '../resources/user-registration-fields';
import * as widgetXpath from './elements/widgetxpath';
import BasePage from './mixin/basepage';
import DropdownSelect from './mixin/dropdown-list-select';

const faker = require('faker/locale/en_US');

export default class CreateAccountPage extends BasePage {
  public async getInvitationKeyInput(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForSelector('#invitationKey');
  }

  public async getSubmitButton(): Promise<ElementHandle> {
    const xpath = widgetXpath.button('Submit');
    return await this.puppeteerPage.waitForXPath(xpath, {visible:true})
  }

  public async getNextButton(): Promise<ElementHandle> {
    const xpath = widgetXpath.button('Next');
    return await this.puppeteerPage.waitForXPath(xpath, {visible:true})
  }

  public async scrollToLastPdfPage(): Promise<ElementHandle> {
    const selectr = '.react-pdf__Document :last-child.react-pdf__Page.tos-pdf-page';
    const pdfPage = await this.puppeteerPage.waitForSelector('.react-pdf__Document :last-child.react-pdf__Page.tos-pdf-page');
    /*
    const [response] = await Promise.all([
       this.puppeteerPage.waitForNavigation({waitUntil: 'domcontentloaded'}),
       this.puppeteerPage.click(selectr, {delay: 20}),
     ]); */
    await this.puppeteerPage.evaluate(el => el.scrollIntoView(), pdfPage);
    await this.puppeteerPage.waitFor(1000);
    return pdfPage;
  }

  // find the second checkbox. it should be for the privacy statement
  public async getPrivacyStatementCheckbox(): Promise<ElementHandle> {
    const label = 'I have read and understand the All of Us Research Program Privacy Statement.';
    const selector = '[type="checkbox"]';
    const element = await this.puppeteerPage.waitForSelector(selector, {visible: true});
    return element;
  }

  public async getPrivacyStatementLabel(): Promise<ElementHandle> {
    const label = 'I have read and understand the All of Us Research Program Privacy Statement.';
    return this.getCheckboxLabel(label);
  }

  // find the second checkbox
  public async getTermOfUseCheckbox(): Promise<ElementHandle> {
    const label = 'I have read and understand the All of Us Research Program Terms of Use described above.';
    const selector = '[type="checkbox"]';
    const element = await this.puppeteerPage.waitForSelector(selector, {visible: true});
    return element;
  }

  public async getTermOfUseLabel(): Promise<ElementHandle> {
    const label = 'I have read and understand the All of Us Research Program Terms of Use described above.';
    return this.getCheckboxLabel(label);
  }

  // find checkbox label by matching label
  public async getCheckboxLabel(checkboxLabel: string): Promise<ElementHandle> {
    const selector = '[type="checkbox"] + label';
    await this.puppeteerPage.waitForSelector(selector);
    const elements = await this.puppeteerPage.$$(selector);
    for (const element of elements) {
      const innerTxt  = await (await element.getProperty('innerText')).jsonValue();
      if (innerTxt === checkboxLabel) {
        return element;
      }
    }

  }

  public async getInstitutionNameInput(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath('//input[@placeholder=\'Institution Name\']');
  }

  public async getResearchBackgroundTextarea(): Promise<ElementHandle> {
    const label = 'Please describe your research background, experience and research interests';
    return await this.puppeteerPage.waitForXPath(`//label[contains(normalize-space(.),'${label}')]/parent::*//textarea`)
  }

  public async fillInFormFields(fields: Array<{ label: string; value: string; }>): Promise<string> {
    let newUserName;

    function formFieldXpathHelper(aLabel: string): string {
      return `//label[contains(normalize-space(.),'${aLabel}')]/parent::*//input[@type='text']`;
    }

    for (const field of fields) {
      const selector = formFieldXpathHelper(field.label);
      const e = await this.puppeteerPage.waitForXPath(selector, {visible: true});
      await e.focus();
      await e.type(field.value);
      await e.press('Tab', { delay: 100 }); // tab out
      if (field.label === 'New Username') {
        await this.puppeteerPage.waitForSelector('clr-icon.is-solid[shape="success-standard"]', {visible: true});
        newUserName = field.value;
      }
    }
    return newUserName;
  }

  // select Institution Affiliation from a dropdown
  public async selectInstitution(selectTextValue: string) {
    const dropdown = new DropdownSelect(this.puppeteerPage);
    await dropdown.select(selectTextValue);
  }

  public async getInstitutionValue() {
    const dropdown = new DropdownSelect(this.puppeteerPage);
    return await dropdown.displayedValue();
  }

  // select Institution Affiliation from a dropdown
  public async selectEducationLevel(selectTextValue: string) {
    const dropdown = new DropdownSelect(this.puppeteerPage, 'Highest Level of Education Completed');
    await dropdown.select(selectTextValue);
  }

  // select Year of Birth from a dropdown
  public async selectYearOfBirth(year: string) {
    const dropdown = new DropdownSelect(this.puppeteerPage, 'Year of Birth');
    await dropdown.select(year);
  }

  // Combined steps to make test code cleaner and shorter

  // Step 1: Enter Invitation key
  public async fillOutInvitationKey() {
    await this.getInvitationKeyInput()
    .then(invitationKeyInput => invitationKeyInput.type(process.env.INVITATION_KEY))
    .then(() => this.getNextButton())
    .then(submitButton => submitButton.click());
  }

  // Step 2: Accepting Terms of Use and Privacy statement.
  public async acceptTermsOfUseAgreement() {
    const privacyStatementCheckbox = await this.getPrivacyStatementCheckbox();
    expect(privacyStatementCheckbox.asElement()).toBeTruthy();
    expect(await privacyStatementCheckbox.getAttribute('disabled')).toBeDefined();

    const termOfUseCheckbox = await this.getTermOfUseCheckbox();
    expect(termOfUseCheckbox).toBeTruthy();
    expect(await termOfUseCheckbox.getAttribute('disabled')).toBeDefined();

    expect(await (privacyStatementCheckbox.asAouElement()).getProp('checked')).toBe(false);
    expect(await (termOfUseCheckbox.asAouElement()).getProp('checked')).toBe(false);

    const nextButton = await this.getNextButton();
    // Next button should be disabled
    let cursor = await getCursorValue(this.puppeteerPage, nextButton);
    expect(cursor).toEqual('not-allowed');

    await this.scrollToLastPdfPage();
    expect(await privacyStatementCheckbox.getAttribute('disabled')).toBeNull();
    expect(await termOfUseCheckbox.getAttribute('disabled')).toBeNull();

    // check by click on label works
    await (await this.getPrivacyStatementLabel()).click();
    await (await this.getTermOfUseLabel()).click();
    // await this.puppeteerPage.evaluate(e => e.click(), await this.getPrivacyStatementLabel());
    // await this.puppeteerPage.evaluate(e => e.click(), await this.getTermOfUseLabel());

    expect(await privacyStatementCheckbox.asAouElement().getProp('checked')).toBe(true);
    expect(await termOfUseCheckbox.asAouElement().getProp('checked')).toBe(true);

    // TODO uncomment after bug fixed https://precisionmedicineinitiative.atlassian.net/browse/RW-4487
    // uncheck a checkbox to ensure NEXT button becomes disabled
    // await page.evaluate(e => e.click(), await createAccountPage.getTermOfUseLabel());
    // cursor = await getCursorValue(page, nextButton);
    // expect(cursor).toEqual('not-allowed');
    // back to check on
    // await page.evaluate(e => e.click(), await createAccountPage.getTermOfUseLabel());

    // NEXT button should be enabled with all required fields filled out
    cursor = await getCursorValue(this.puppeteerPage, nextButton);
    expect(cursor).toEqual('pointer');
  }

  // Step 3: Enter user default information
  public async fillOutUserInformation() {
    const newUserName = await this.fillInFormFields(inputFieldsValues);

    // NEXT button should be disabled until all required fields are filled
    const nextButton = await this.getNextButton();
    let cursor = await getCursorValue(this.puppeteerPage, nextButton);
    expect(cursor).toEqual('not-allowed');

    await (await this.getResearchBackgroundTextarea()).type(faker.lorem.word());
    await (await this.getInstitutionNameInput()).type(faker.company.companyName());
    await this.selectInstitution(institutionAffiliationValues.EARLY_CAREER_TENURE_TRACK_RESEARCHER);
    await this.puppeteerPage.waitFor(1000);
    expect(await this.getInstitutionValue()).toEqual(institutionAffiliationValues.EARLY_CAREER_TENURE_TRACK_RESEARCHER);

    // NEXT button should be enabled. Click it.
    cursor = await getCursorValue(this.puppeteerPage, nextButton);
    expect(cursor).toEqual('pointer');
  }

  // Step 4: Enter demographic survey default information (All Survey Fields are optional)
  public async fillOutDemographicSurvey() {
    // SUBMIT button should be enabled and clickable
    const submitButton = await this.getSubmitButton();
    const cursor = await getCursorValue(this.puppeteerPage, submitButton);
    expect(cursor).toEqual('pointer');

    // Find and check on all checkboxes with same label: Prefer not to answer
    const targetXpath = '//*[contains(normalize-space(.),"Prefer not to answer")]/ancestor::*/input[@type="checkbox"]';
    await this.puppeteerPage.waitForXPath(targetXpath, { visible: true });
    const checkboxs = await this.puppeteerPage.$x(targetXpath);
    for (const ck of checkboxs) {
      await ck.click();
    }
    // Select year of birth 1955
    await this.selectYearOfBirth('1955');
    // Select Highest Education completed
    await this.selectEducationLevel('Master’s degree');
    await this.puppeteerPage.waitFor(1000);
  }

}
