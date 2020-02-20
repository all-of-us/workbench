import {ElementHandle} from 'puppeteer';
import BasePage from './mixin/basepage';
import DropdownSelect from './mixin/dropdown-list-select';

export default class CreateAccountPage extends BasePage {
  public async getInvitationKeyInput(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForSelector('#invitationKey');
  }

  public async getSubmitButton(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath('//*[@role=\'button\'][(text()=\'Next\')]', {visible:true})
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
    const selector = '[type=\'checkbox\']';
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
    const selector = '[type=\'checkbox\']';
    const element = await this.puppeteerPage.waitForSelector(selector, {visible: true});
    return element;
  }

  public async getTermOfUseLabel(): Promise<ElementHandle> {
    const label = 'I have read and understand the All of Us Research Program Terms of Use described above.';
    return this.getCheckboxLabel(label);
  }

  // find checkbox label by matching label
  public async getCheckboxLabel(checkboxLabel: string): Promise<ElementHandle> {
    const selector = '[type=\'checkbox\'] + label';
    await this.puppeteerPage.waitForSelector(selector);
    const elements = await this.puppeteerPage.$$(selector);
    for (const element of elements) {
      const innerTxt  = await (await element.getProperty('innerText')).jsonValue();
      if (innerTxt === checkboxLabel) {
        return element;
      }
    }

  }

  public async getNextButton(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath('//*[@role=\'button\'][text()=\'Next\']');
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
        await this.puppeteerPage.waitForSelector('clr-icon.is-solid[shape=\'success-standard\']', {visible: true});
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

  public async getInstitutionDisplayedValue() {
    const dropdown = new DropdownSelect(this.puppeteerPage);
    return await dropdown.displayedValue();
  }


}
