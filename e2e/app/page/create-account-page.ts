import SelectMenu from 'app/component/select-menu';
import Button from 'app/element/button';
import Checkbox from 'app/element/checkbox';
import ClrIconLink from 'app/element/clr-icon-link';
import Textarea from 'app/element/textarea';
import Textbox from 'app/element/textbox';
import { findIframe } from 'app/xpath-finder';
import BasePage from 'app/page/base-page';
import { ElementType } from 'app/xpath-options';
import { Frame, Page } from 'puppeteer';
import { defaultCountrySelection, defaultFieldValues } from 'resources/data/user-registration-data';
import { config } from 'resources/workbench-config';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import { LinkText, Institution, InstitutionRole } from 'app/text-labels';
import { getPropValue } from 'utils/element-utils';
import ReactSelect from 'app/element/react-select';

const faker = require('faker/locale/en_US');

export const EducationLevelValue = {
  Doctorate: 'Doctorate'
  // other option values here
};

export const LabelAlias = {
  InstitutionName: 'Institution Name',
  AreYouAffiliated: 'Are you affiliated with an Academic Research Institution',
  ResearchBackground: 'Your research background, experience, and research interests',
  EducationLevel: 'Highest Level of Education Completed', // Highest Level of Education Completed
  YearOfBirth: 'Year of Birth',
  InstitutionEmail: 'Your institutional email address',
  Ethnicity: 'Ethnicity'
};

export const FieldSelector = {
  InstitutionEmailTextbox: {
    textOption: {
      containsText: LabelAlias.InstitutionEmail,
      ancestorLevel: 2
    }
  },
  EducationLevelSelect: {
    textOption: {
      type: ElementType.Dropdown,
      name: LabelAlias.EducationLevel,
      ancestorLevel: 2
    }
  },
  EthnicitySelect: {
    textOption: {
      type: ElementType.Dropdown,
      name: LabelAlias.Ethnicity,
      ancestorLevel: 2
    }
  },
  BirthYearSelect: {
    textOption: {
      type: ElementType.Dropdown,
      name: LabelAlias.YearOfBirth,
      ancestorLevel: 2
    }
  },
  InstitutionSelect: {
    textOption: {
      type: ElementType.Dropdown,
      name: 'Select your institution',
      ancestorLevel: 2
    }
  },
  DescribeRole: {
    textOption: {
      type: ElementType.Dropdown,
      containsText: 'describes your role',
      ancestorLevel: 2
    }
  },
  AddressCountry: {
    name: 'Country'
  }
};

export default class CreateAccountPage extends BasePage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      waitForText(this.page, 'Please read through the entire agreement to continue'),
      this.page.waitForXPath('//*[@data-test-id="terms-of-service"]', { visible: true })
    ]);
    await waitWhileLoading(this.page);
    return true;
  }

  getSubmitButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Submit });
  }

  getNextButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Next });
  }

  async agreementLoaded(): Promise<boolean> {
    const iframe = await findIframe(this.page, 'terms of use and privacy statement');
    const bodyHandle = await iframe.$('body');
    return iframe.evaluate((body) => body.scrollHeight > 0, bodyHandle);
  }

  async readAgreement(): Promise<Frame> {
    const iframe = await findIframe(this.page, 'terms of use and privacy statement');
    const bodyHandle = await iframe.$('body');
    await iframe.evaluate((body) => body.scrollTo(0, body.scrollHeight), bodyHandle);
    await this.page.waitForTimeout(1000);
    return iframe;
  }

  getTermsOfUseCheckbox(): Checkbox {
    const selector = '//*[@id=//label[contains(normalize-space(), "Terms of Use")]/@for]';
    return new Checkbox(this.page, selector);
  }

  getResearchBackgroundTextarea(): Textarea {
    return Textarea.findByName(this.page, { normalizeSpace: LabelAlias.ResearchBackground });
  }

  async selectAddressCountry(selectTextValue: string): Promise<void> {
    const select = new ReactSelect(this.page, { name: FieldSelector.AddressCountry.name });
    return select.selectOption(selectTextValue);
  }

  async getUsernameDomain(): Promise<string> {
    const elem = await this.page.waitForXPath('//*[./input[@id="username"]]/i', { visible: true });
    return getPropValue<string>(elem, 'innerText');
  }

  async fillInFormFields(fields: { label: string; value: string }[]): Promise<string> {
    let newUserName;
    for (const field of fields) {
      const textbox = Textbox.findByName(this.page, { name: field.label });
      await textbox.type(field.value);
      await textbox.pressTab();
      if (field.label === 'New Username') {
        await ClrIconLink.findByName(this.page, { name: field.label, iconShape: 'success-standard' }).asElementHandle();
        newUserName = field.value; // store new username for return
      }
    }
    return newUserName;
  }

  // select Institution Affiliation from a dropdown
  async selectInstitution(selectTextValue: string): Promise<void> {
    const dropdown = SelectMenu.findByName(this.page, FieldSelector.InstitutionSelect.textOption);
    return dropdown.select(selectTextValue);
  }

  async getInstitutionValue(): Promise<string> {
    const dropdown = SelectMenu.findByName(this.page, FieldSelector.InstitutionSelect.textOption);
    return dropdown.getSelectedValue();
  }

  // select Education Level from a dropdown
  async selectEducationLevel(selectTextValue: string): Promise<void> {
    const dropdown = SelectMenu.findByName(this.page, FieldSelector.EducationLevelSelect.textOption);
    return dropdown.select(selectTextValue);
  }

  // select Year of Birth from a dropdown
  async selectYearOfBirth(year: string): Promise<void> {
    const dropdown = SelectMenu.findByName(this.page, FieldSelector.BirthYearSelect.textOption);
    return dropdown.select(year);
  }

  // select Ethnicity from a dropdown
  async selectEthnicity(option: string): Promise<void> {
    const dropdown = SelectMenu.findByName(this.page, FieldSelector.EthnicitySelect.textOption);
    return dropdown.select(option);
  }

  // Combined steps to make test code cleaner and shorter

  // Step 2: Fill out institution affiliation details
  async fillOutInstitution(): Promise<void> {
    await Promise.all([waitForText(this.page, 'complete Step 1 of 3'), waitWhileLoading(this.page)]);

    await this.selectInstitution(Institution.Broad);
    await this.getInstitutionValue();
    const emailAddressTextbox = Textbox.findByName(this.page, FieldSelector.InstitutionEmailTextbox.textOption);
    await emailAddressTextbox.type(config.INSTITUTION_CONTACT_EMAIL);
    await emailAddressTextbox.pressTab(); // tab out to start email validation
    await ClrIconLink.findByName(this.page, {
      containsText: LabelAlias.InstitutionEmail,
      ancestorLevel: 2,
      iconShape: 'success-standard'
    }).asElementHandle();

    const roleSelect = SelectMenu.findByName(this.page, FieldSelector.DescribeRole.textOption);
    await roleSelect.select(InstitutionRole.UndergraduateStudent);
  }

  // Step 1: Accepting Terms of Use and Privacy statement.
  async acceptTermsOfUseAgreement(): Promise<void> {
    const termsOfUseCheckbox = this.getTermsOfUseCheckbox();

    await this.readAgreement();

    // check by click on label works
    await termsOfUseCheckbox.check();
  }

  // Step 3: Fill out user information with default values
  async fillOutUserInformation(): Promise<string> {
    const newUserName = await this.fillInFormFields(defaultFieldValues);
    await this.selectAddressCountry(defaultCountrySelection);
    await this.getResearchBackgroundTextarea().type(faker.lorem.word());
    return newUserName;
  }
}
