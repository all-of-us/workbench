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
  rtAcceptedEmailAddresses: 'rtEmailAddressInput',
  ctAcceptedEmailAddresses: 'ctEmailAddressInput',
  ControlledTierAccess: 'ct-card-container'
};

export const LabelAlias = {
  InstitutionType: 'Organization Type',
  SelectRTEmailType: 'Select type',
  SelectCTEmailType: 'Email address is at one of the following domains:',
};


export const InstitutionTypeSelectValue = {
  Industry: 'Industry',
  Academic: 'Academic Research Institution',
  Educational: 'Educational Institution',
  HealthCenter: 'Health Center / Non-Profit',
  Other: 'Other'
};

export const AcceptedAddressSelectValue = {
  Domains : 'Email address is at one of the following domains:',
  Individual : 'Individual email address is listed below:'
};

export const Field = {
  InstitutionTypeSelect: {
    textOption: {
      type: ElementType.Dropdown,
      name: LabelAlias.InstitutionType,
      ancestorLevel: 2
    }
  },
  AcceptedCTAddressSelect: {
    textOption: {
      type: ElementType.Dropdown,
      name: LabelAlias.SelectCTEmailType,
      ancestorLevel: 2
    }
  },
  AcceptedRTAddressSelect: {
    textOption: {
      type: ElementType.Dropdown,
      name: LabelAlias.SelectRTEmailType,
      ancestorLevel: 2
    }
  },
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
  
  getCancelButton():Button {
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

  getHeaderText(instituteName: string): string{
    return `//div/h3[text()= '${instituteName}']`;
  }

  // get the institute name from the Institution Name input field
  async getInstituteNameValue(): Promise<string> {
    const instituteNameValue = this.getInstituteNameInput();
      const instituteName = await instituteNameValue.getProperty<string>('value');
      //const regex = new RegExp(/(?<=of ).*(?= limit)/);
      return instituteName;
    }

    // get the Institution Name input field
  getInstituteNameInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.displayName });
  }

  // get the Institution Type dropdown field
  getInstituteTypeDropdown(): SelectMenu {
    return SelectMenu.findByName(this.page, Field.InstitutionTypeSelect.textOption);
  }

  // get User Email Instructions Textarea field
  getInstructionsTextarea(): Textarea { 
    return Textarea.findByName(this.page, { id: DataTestIdAlias.userEmailInstructions });
    }

  // select Institution Type from a dropdown
  async selectInstitutionType(selectTextValue: string): Promise<void> {
    //const dropdown = SelectMenu.findByName(this.page, FieldSelector.InstitutionTypeSelect.textOption);
    const dropdown = this.getInstituteTypeDropdown();
    return dropdown.select(selectTextValue);
  }

    // get the institute type value
    async getInstitutionTypeValue(): Promise<string> {
      const dropdown = SelectMenu.findByName(this.page, Field.InstitutionTypeSelect.textOption);
      return dropdown.getSelectedValue();
    }

    // select Institution Type from a dropdown
    // async selectAgreementTypeType(selectTextValue: string): Promise<void> {
    //   const dropdown = SelectMenu.findByName(this.page, Field.AgreementTypeSelect.textOption);
    //   return dropdown.select(selectTextValue);
    // }

    async getAddNewInstituteFields(): Promise<void> {
      this.getInstituteNameInput();
      this.getInstructionsTextarea();
      this.selectInstitutionType(InstitutionTypeSelectValue.Industry);
    }

    // edit the whitelist emails Controlled tier access
    getCTAcceptedEmailInput(): Textarea {
      return Textarea.findByName(this.page, { dataTestId: DataTestIdAlias.ctAcceptedEmailAddresses });
    }
  
    // edit the whitelist emails Registered tier access
    getRTAcceptedEmailInput(): Textarea {
      return Textarea.findByName(this.page, { dataTestId: DataTestIdAlias.rtAcceptedEmailAddresses });
    }

    // get the Institution's CT-eRA-account-required-switch and verify if enabled
    getRTeRAtoggle(): Checkbox {
      const xpath = '//div[@data-test-id="rt-era-required-switch"]/div/input[@type="checkbox"]';
      return new Checkbox(this.page, xpath);
    }

    // get the Institution's CT-eRA-account-required-switch and verify if disabled
    getCTeRAtoggle(): Checkbox {
      const xpath = '//div[@data-test-id="ct-era-required-switch"]/div/input[@type="checkbox"]';
      return new Checkbox(this.page, xpath);
    }

    // get the Institution's CT-enabled-switch to verify if it is enabled or disabled
    getCTEnabledtoggle(): Checkbox {
      const xpath = '//div[@data-test-id="ct-enabled-switch"]/div/input[@type="checkbox"]';
      return new Checkbox(this.page, xpath);
    }

    // enable the CT toggle
    async clickCTEnabledtoggle(): Promise<boolean> {
      const ctEnabled = this.getCTEnabledtoggle();
      await ctEnabled.toggle(true);
      return true;
    }

    getCTDomainsDropdown(): SelectMenu {
      return SelectMenu.findByName(this.page, Field.AcceptedCTAddressSelect.textOption);
    }

    async selectCTEmailOption(value: string): Promise<void> {
      const options =this.getCTDomainsDropdown();
      await options.select(value);
    }

    async isDivCardContainerPresent(labelText: string): Promise<boolean> {
      const selector = `//div[@data-test-id="ct-card-container"]//label[text()='${labelText}']`;
      const elements = await page.$x(selector);
      return elements.length > 0;
    }

    async isDivWithLabelPresent(labelText: string): Promise<boolean> {
      return this.isDivCardContainerPresent(`${labelText}`);
    }

    getRTDomainsDropdown(): SelectMenu {
      // const xpath = '//div//label[text()="Registered tier access"]/following-sibling::div//div[@role="button"]';
      // const button = new Button(this.page, xpath);
      // await button.click();
      return SelectMenu.findByName(this.page, Field.AcceptedRTAddressSelect.textOption);
      //return new SelectMenu(this.page);
    }
}
