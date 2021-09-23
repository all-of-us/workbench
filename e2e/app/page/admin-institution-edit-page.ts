import { Page } from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle } from 'utils/waits-utils';
import Checkbox from 'app/element/checkbox';
import Textbox from 'app/element/textbox';
import Textarea from 'app/element/textarea';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import SelectMenu from 'app/component/select-menu';
import { ElementType } from 'app/xpath-options';

const PageTitle = 'Institution Admin | All of Us Researcher Workbench';

const DataTestIdAlias = {
  displayName: 'displayName',
  userEmailInstructions: 'userEmailInstructions',
  rtAcceptedEmailAddresses: 'registered-email-address-input',
  rtAcceptedEmailDomains: 'registered-email-domain-input',
  ctAcceptedEmailAddresses: 'controlled-email-address-input',
  ctAcceptedEmailDomains: 'controlled-email-domain-input',
  ControlledTierAccess: 'ct-card-container',
  registeredCardDetails: 'registered-card-details',
  controlledCardDetails: 'controlled-card-details'
};

export const LabelAlias = {
  InstitutionType: 'Institution Type'
};

export const InstitutionTypeSelectValue = {
  Industry: 'Industry',
  Academic: 'Academic Research Institution',
  Educational: 'Educational Institution',
  HealthCenter: 'Health Center / Non-Profit',
  Other: 'Other'
};

export const AcceptedAddressSelectValue = {
  Domains: 'Email address is at one of the following domains:',
  Individual: 'Individual email address is listed below:'
};

export const Field = {
  InstitutionTypeSelect: {
    textOption: {
      type: ElementType.Dropdown,
      name: LabelAlias.InstitutionType,
      ancestorLevel: 2
    }
  },
  AcceptedRTAddressSelect: {
    textOption: {
      type: ElementType.Dropdown,
      dataTestId: DataTestIdAlias.registeredCardDetails,
      ancestorLevel: 2
    }
  },
  AcceptedCTAddressSelect: {
    textOption: {
      type: ElementType.Dropdown,
      dataTestId: DataTestIdAlias.controlledCardDetails,
      ancestorLevel: 2
    }
  }
};

export default class InstitutionEditPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle)]);
    return true;
  }

  getSaveButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Save });
  }

  getCancelButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Cancel });
  }

  async waitForSaveButton(isActive: boolean): Promise<Button> {
    const button = this.getSaveButton();
    const isCursorEnabled = !(await button.isCursorNotAllowed());
    expect(isCursorEnabled).toBe<boolean>(isActive);
    return button;
  }

  getAddButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Add });
  }

  async waitForAddButton(isActive: boolean): Promise<Button> {
    const button = this.getAddButton();
    const isCursorEnabled = !(await button.isCursorNotAllowed());
    expect(isCursorEnabled).toBe<boolean>(isActive);
    return button;
  }

  // get the institute name from the Institution Name input field
  async getInstituteNameValue(): Promise<string> {
    const instituteNameValue = this.getInstituteNameInput();
    const instituteName = await instituteNameValue.getProperty<string>('value');
    return instituteName;
  }

  // get the Institution Name input field
  getInstituteNameInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.displayName });
  }

  // get User Email Instructions Textarea field
  getInstructionsTextarea(): Textarea {
    return Textarea.findByName(this.page, { id: DataTestIdAlias.userEmailInstructions });
  }

  // get the Institution Type dropdown
  getInstituteTypeDropdown(): SelectMenu {
    return SelectMenu.findByName(this.page, Field.InstitutionTypeSelect.textOption);
  }

  // select Institution Type from the dropdown
  async selectInstitutionType(selectTextValue: string): Promise<void> {
    const dropdown = this.getInstituteTypeDropdown();
    return await dropdown.select(selectTextValue);
  }

  // get the institute type value
  async getInstitutionTypeValue(): Promise<string> {
    const dropdown = this.getInstituteTypeDropdown();
    return await dropdown.getSelectedValue();
  }

  // the institution name, type and intructions input field
  async getAddNewInstituteFields(): Promise<void> {
    this.getInstituteNameInput();
    this.getInstructionsTextarea();
    this.selectInstitutionType(InstitutionTypeSelectValue.Industry);
  }

  // get the Institution's RT-eRA-account-required-switch and verify if enabled
  getRTeRAtoggle(): Checkbox {
    const xpath = '//div[@data-test-id="registered-era-required-switch"]/div/input[@type="checkbox"]';
    return new Checkbox(this.page, xpath);
  }

  // get the dropdown in registered-card-details
  getRTEmailDropdown(): SelectMenu {
    return SelectMenu.findByName(this.page, Field.AcceptedRTAddressSelect.textOption);
  }

  // select type from dropdown in registered-card-details
  async selectRTEmailOption(option: string): Promise<void> {
    const options = this.getRTEmailDropdown();
    await options.select(option);
  }

  // get the emailAddress textarea in Registered tier access
  getRTEmailAddressInput(): Textarea {
    return Textarea.findByName(this.page, { dataTestId: DataTestIdAlias.rtAcceptedEmailAddresses });
  }

  // get the emailDomains textarea Registered tier access
  getRTEmailDomainsInput(): Textarea {
    return Textarea.findByName(this.page, { dataTestId: DataTestIdAlias.rtAcceptedEmailDomains });
  }

  // get the Institution's controlled-era-required-switch and verify if disabled
  getCTeRAtoggle(): Checkbox {
    const xpath = '//div[@data-test-id="controlled-era-required-switch"]/div/input[@type="checkbox"]';
    return new Checkbox(this.page, xpath);
  }

  // get the Institution's controlled-enabled-switch to verify if it is enabled or disabled
  getCTEnabledtoggle(): Checkbox {
    const xpath = '//div[@data-test-id="controlled-enabled-switch"]/div/input[@type="checkbox"]';
    return new Checkbox(this.page, xpath);
  }

  // enable the CT toggle
  async clickCTEnabledtoggle(): Promise<boolean> {
    const ctEnabled = this.getCTEnabledtoggle();
    await ctEnabled.toggle(true);
    return true;
  }

  getCTEmailDropdown(): SelectMenu {
    return SelectMenu.findByName(this.page, Field.AcceptedCTAddressSelect.textOption);
  }

  // select the dropdown option in CT
  async selectCTEmailOption(value: string): Promise<void> {
    const options = this.getCTEmailDropdown();
    await options.select(value);
  }

  // get the CT emailAddress Input
  getCTEmailAddressInput(): Textarea {
    return Textarea.findByName(this.page, { dataTestId: DataTestIdAlias.ctAcceptedEmailAddresses });
  }

  // get the CT emailDomains Input
  getCTEmailDomainsInput(): Textarea {
    return Textarea.findByName(this.page, { dataTestId: DataTestIdAlias.ctAcceptedEmailDomains });
  }

  // get the value of the selected dropdown option in RT
  async getRTEmailAcceptedValue(): Promise<string> {
    const dropdown = this.getRTEmailDropdown();
    return await dropdown.getSelectedValue();
  }

  // get the value of the selected dropdown option in CT
  async getCTEmailAcceptedValue(): Promise<string> {
    const dropdown = this.getCTEmailDropdown();
    return await dropdown.getSelectedValue();
  }

  async clickCancelButton(): Promise<void> {
    const button = this.getCancelButton();
    await button.click();
  }
}
