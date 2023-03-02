import { Page } from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Checkbox from 'app/element/checkbox';
import Textbox from 'app/element/textbox';
import Textarea from 'app/element/textarea';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import SelectMenu from 'app/component/select-menu';
import { ElementType } from 'app/xpath-options';
import InstitutionNotSavedModal from 'app/modal/institution-not-saved-modal';

const PageTitle = 'Institution Admin | All of Us Researcher Workbench';

const DataTestIdAlias = {
  displayName: 'displayName',
  userEmailInstructions: 'userEmailInstructions',
  rtAcceptedEmailAddresses: 'registered-email-address-input',
  rtAcceptedEmailDomains: 'registered-email-domain-input',
  ctAcceptedEmailAddresses: 'controlled-email-address-input',
  ctAcceptedEmailDomains: 'controlled-email-domain-input',
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
  AcceptedRtAddressSelect: {
    textOption: {
      type: ElementType.Dropdown,
      dataTestId: DataTestIdAlias.registeredCardDetails,
      ancestorLevel: 2
    }
  },
  AcceptedCtAddressSelect: {
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

  getAddButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Add });
  }

  // get the Institution name from the Institution Name input field
  async getInstitutionNameValue(): Promise<string> {
    const InstitutionNameValue = this.getInstitutionNameInput();
    const InstitutionName = await InstitutionNameValue.getProperty<string>('value');
    return InstitutionName;
  }

  // get the Institution Name input field
  getInstitutionNameInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.displayName });
  }

  // get User Email Instructions Textarea field
  getInstructionsTextarea(): Textarea {
    return Textarea.findByName(this.page, { id: DataTestIdAlias.userEmailInstructions });
  }

  // get the Institution Type dropdown
  getInstitutionTypeDropdown(): SelectMenu {
    return SelectMenu.findByName(this.page, Field.InstitutionTypeSelect.textOption);
  }

  // select Institution Type from the dropdown
  async selectInstitutionType(selectTextValue: string): Promise<void> {
    const dropdown = this.getInstitutionTypeDropdown();
    return await dropdown.select(selectTextValue);
  }

  // get the Institution type value
  async getInstitutionTypeValue(): Promise<string> {
    const dropdown = this.getInstitutionTypeDropdown();
    return await dropdown.getSelectedValue();
  }

  // the institution name, type and intructions input field
  async getAddNewInstitutionFields(): Promise<void> {
    this.getInstitutionNameInput();
    this.getInstructionsTextarea();
    await this.selectInstitutionType(InstitutionTypeSelectValue.Industry);
  }

  // get the Institution's RT-eRA-account-required-switch and verify if enabled
  getRtEratoggle(): Checkbox {
    const xpath = '//input[@data-test-id="registered-era-required-switch"]';
    return new Checkbox(this.page, xpath);
  }

  // get the dropdown in registered-card-details
  getRtEmailDropdown(): SelectMenu {
    return SelectMenu.findByName(this.page, Field.AcceptedRtAddressSelect.textOption);
  }

  // select type from dropdown in registered-card-details
  async selectRtEmailOption(option: string): Promise<void> {
    const options = this.getRtEmailDropdown();
    await options.select(option);
  }

  // get the emailAddress textarea in Registered tier access
  getRtEmailAddressInput(): Textarea {
    return Textarea.findByName(this.page, { dataTestId: DataTestIdAlias.rtAcceptedEmailAddresses });
  }

  // get the emailDomains textarea Registered tier access
  getRtEmailDomainsInput(): Textarea {
    return Textarea.findByName(this.page, { dataTestId: DataTestIdAlias.rtAcceptedEmailDomains });
  }

  // get the Institution's controlled-era-required-switch and verify if disabled
  getCtEratoggle(): Checkbox {
    const xpath = '//input[@data-test-id="controlled-era-required-switch"]';
    return new Checkbox(this.page, xpath);
  }

  // get the Institution's controlled-enabled-switch to verify if it is enabled or disabled
  getCtEnabledtoggle(): Checkbox {
    const xpath = '//input[@data-test-id="controlled-enabled-switch"]';
    return new Checkbox(this.page, xpath);
  }

  // enable the CT toggle
  async clickCtEnabledtoggle(): Promise<void> {
    const ctEnabled = this.getCtEnabledtoggle();
    await ctEnabled.toggle(true);
  }

  getCtEmailDropdown(): SelectMenu {
    return SelectMenu.findByName(this.page, Field.AcceptedCtAddressSelect.textOption);
  }

  // select the dropdown option in CT
  async selectCtEmailOption(value: string): Promise<void> {
    const options = this.getCtEmailDropdown();
    await options.select(value);
  }

  // get the CT emailAddress Input
  getCtEmailAddressInput(): Textarea {
    return Textarea.findByName(this.page, { dataTestId: DataTestIdAlias.ctAcceptedEmailAddresses });
  }

  // get the CT emailDomains Input
  getCtEmailDomainsInput(): Textarea {
    return Textarea.findByName(this.page, { dataTestId: DataTestIdAlias.ctAcceptedEmailDomains });
  }

  // get the value of the selected dropdown option in RT
  async getRtEmailAcceptedValue(): Promise<string> {
    const dropdown = this.getRtEmailDropdown();
    return await dropdown.getSelectedValue();
  }

  // get the value of the selected dropdown option in CT
  async getCtEmailAcceptedValue(): Promise<string> {
    const dropdown = this.getCtEmailDropdown();
    return await dropdown.getSelectedValue();
  }

  async clickCancelButton(): Promise<void> {
    const button = this.getCancelButton();
    const navPromise = this.page.waitForNavigation({ waitUntil: ['load', 'networkidle0'] });
    await button.click();
    await navPromise;
    await waitWhileLoading(this.page);
  }

  async clickBackButton(): Promise<InstitutionNotSavedModal> {
    const iconXpath = '//h3[text()="Add new Institution"]/preceding-sibling::div[@tabindex="0"  and @role = "button"]';
    await this.page.waitForXPath(iconXpath, { visible: true }).then((icon) => icon.click());
    const modal = new InstitutionNotSavedModal(this.page);
    await modal.waitForLoad();
    return modal;
  }
}
