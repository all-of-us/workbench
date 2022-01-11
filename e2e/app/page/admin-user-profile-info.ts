import { Page } from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle } from 'utils/waits-utils';
import Checkbox from 'app/element/checkbox';
import Textbox from 'app/element/textbox';
import BaseElement from 'app/element/base-element';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import SelectMenu from 'app/component/select-menu';
import { ElementType } from 'app/xpath-options';

const PageTitle = 'User Admin | All of Us Researcher Workbench';

const DataTestIdAlias = {
  UserFullName: 'userFullName',
  Username: 'username',
  ContactEmail: 'contactEmail',
  InitialCreditsUsed: 'initial-credits-used',
  InitialCreditsDropdown: 'initial-credits-dropdown',
  VerifiedInstitution: 'verifiedInstitution',
  InstitutionalRole: 'institutionalRole',
  EmailErrorMessage: 'email-error-message',
  TwoFactorAuth: 'twoFactorAuthBypassToggle',
  ComplianceTraining: 'complianceTrainingBypassToggle',
  EraCommons: 'eRA Commons',
  DataUserCodeOfConduct: 'dataUseAgreementBypassToggle',
  rasLinkLoginGov: 'rasLinkLoginGovBypassToggle'
};

export const LabelAlias = {
  VerifiedInstitution: 'Verified institution',
  FreeCreditLimit: 'Free credit limit',
  InstitutionalRole: 'Institutional role'
};

export const FreeCreditSelectValue = {
  three: '$300.00',
  three1: '$350.00',
  four: '$400.00',
  four1: '$450.00',
  five: '$500.00',
  five1: '$550.00',
  six: '$600.00',
  six1: '$650.00',
  seven: '$700.00',
  seven1: '$750.00',
  eight: '$800.00'
};

export const InstitutionRoleSelectValue = {
  ProjectPersonnel:
    'Project Personnel (eg: Research Assistant, Data Analyst, Project Manager, Research Coordinator, or other roles)',
  ResearchAssistant: 'Research Assistant (pre-doctoral)',
  ResearchAssociate: 'Research associate (post-doctoral; early/mid career)',
  SeniorResearcher: 'Senior Researcher (PI/Team Lead, senior scientist)',
  Other: 'Other (free text)'
};

export const FieldSelector = {
  FreeCreditLimitSelect: {
    textOption: {
      type: ElementType.Dropdown,
      name: LabelAlias.FreeCreditLimit,
      ancestorLevel: 2
    }
  },
  VerifiedInstitutionSelect: {
    textOption: {
      type: ElementType.Dropdown,
      name: LabelAlias.VerifiedInstitution,
      ancestorLevel: 2
    }
  },
  InstitutionalRoleSelect: {
    textOption: {
      type: ElementType.Dropdown,
      name: LabelAlias.InstitutionalRole,
      ancestorLevel: 2
    }
  }
};

export default class UserProfileInfo extends AuthenticatedPage {
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

  async waitForSaveButton(isActive: boolean): Promise<Button> {
    const button = this.getSaveButton();
    const isCursorEnabled = !(await button.isCursorNotAllowed());
    expect(isCursorEnabled).toBe<boolean>(isActive);
    return button;
  }

  getAccountAccessToggle(): Checkbox {
    const xpath = "//label[text()='Account access']/following-sibling::label/div/input[@type='checkbox']";
    return new Checkbox(this.page, xpath);
  }

  // extract only the Account access text to verify the user is enabled or disabled
  async getAccountAccessText(): Promise<string> {
    const xpath = "//label[text()='Account access']/following-sibling::label/span";
    const element = BaseElement.asBaseElement(this.page, await this.page.waitForXPath(xpath, { visible: true }));
    const textContent = element.getTextContent();
    return textContent;
  }

  getNameInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.UserFullName });
  }

  getUsernameInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.Username });
  }

  // get the placeholder in username field
  getUserNamePlaceHolder(): Promise<string> {
    const userName = this.getUsernameInput();
    const userProfileEmail = userName.getProperty<string>('placeholder');
    return userProfileEmail;
  }

  getContactEmailInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.ContactEmail });
  }

  getFreeCreditsUsedInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.InitialCreditsUsed });
  }

  // get the free credits used value
  async getFreeCreditMaxValue(): Promise<string> {
    const freeCreditsMax = this.getFreeCreditsUsedInput();
    const freeCreditsMaxValue = await freeCreditsMax.getProperty<string>('value');
    const regex = new RegExp(/(?<=of ).*(?= limit)/);
    return regex.exec(freeCreditsMaxValue)[0];
  }

  // get the free credits limit value
  async getFreeCreditsLimitValue(): Promise<string> {
    const freeCreditsDropdown = SelectMenu.findByName(this.page, { dataTestId: DataTestIdAlias.InitialCreditsDropdown });
    return await freeCreditsDropdown.getSelectedValue();
  }

  // select a different Verified Institution to verify email match with institution
  async selectVerifiedInstitution(selectTextValue: string): Promise<void> {
    const dropdown = SelectMenu.findByName(this.page, FieldSelector.VerifiedInstitutionSelect.textOption);
    return dropdown.select(selectTextValue);
  }

  // select a new Free credit value
  async selectFreeCredits(selectTextValue: string): Promise<void> {
    const dropdown = SelectMenu.findByName(this.page, FieldSelector.FreeCreditLimitSelect.textOption);
    return dropdown.select(selectTextValue);
  }

  // update free credit
  async updateFreeCredits(): Promise<void> {
    await this.selectFreeCredits(FreeCreditSelectValue.six);
    const creditValue = await this.getFreeCreditsLimitValue();
    const newfreeCreditMaxValue = await this.getFreeCreditMaxValue();
    expect(creditValue).toEqual(newfreeCreditMaxValue);
    await this.waitForSaveButton(true);
  }
}
