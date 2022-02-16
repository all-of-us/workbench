import AuthenticatedPage from './authenticated-page';
import { ElementHandle, Page } from 'puppeteer';
import { waitForDocumentTitle, waitForText, waitWhileLoading } from 'utils/waits-utils';
import Link from 'app/element/link';
import Textbox from 'app/element/textbox';
import SelectMenu from 'app/component/select-menu';
import { ElementType } from 'app/xpath-options';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import Switch, { defaultSwitchXpath } from 'app/component/switch';
import StaticText from 'app/element/staticText';
import { parseForNumbericalString } from 'utils/test-utils';
import DataTable from 'app/component/data-table';
import { getPropValue } from 'utils/element-utils';
import Cell from 'app/component/cell';
import ClrIconLink from 'app/element/clr-icon-link';

const pageTitle = 'User Profile Admin';

export default class UserProfileAdminPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      waitForDocumentTitle(this.page, pageTitle),
      this.getAuditLink().exists(60 * 1000),
      waitForText(this.page, 'User Profile Information', { timeout: 60 * 1000 })
    ]);
    await waitWhileLoading(this.page);
    return true;
  }

  getBackLink(): ClrIconLink {
    return ClrIconLink.findByName(this.page, { iconShape: 'arrow' });
  }

  getAuditLink(): Link {
    return new Link(this.page, '//a[normalize-space(text())="AUDIT"]');
  }

  async getName(): Promise<string> {
    const name = StaticText.findByName(this.page, { name: 'Name' });
    const text = await name.getText();
    return text.split('\n')[1];
  }

  async getUserName(): Promise<string> {
    const userName = StaticText.findByName(this.page, { name: 'User name' });
    const text = await userName.getText();
    return text.split('\n')[1];
  }

  async getDataAccessTiers(): Promise<string> {
    const tiers = StaticText.findByName(this.page, { name: 'Data Access Tiers' });
    const text = await tiers.getText();
    return text.split('\n')[1];
  }

  async getInitialCreditsUsed(): Promise<[number, number]> {
    const creditsUsed = StaticText.findByName(this.page, { name: 'Initial Credits Used' });
    const text = await creditsUsed.getText();
    const words = text.split('\n')[1];
    const currencies = parseForNumbericalString(words);
    return [parseInt(currencies[0]), parseInt(currencies[1])];
  }

  getContactEmail(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: 'contactEmail' });
  }

  getInitialCreditLimit(): SelectMenu {
    return SelectMenu.findByName(this.page, {
      type: ElementType.Dropdown,
      dataTestId: 'initial-credits-dropdown',
      ancestorLevel: 0
    });
  }

  getInstitutionalRole(): SelectMenu {
    return SelectMenu.findByName(this.page, {
      type: ElementType.Dropdown,
      dataTestId: 'institutionalRole',
      ancestorLevel: 0
    });
  }

  getVerifiedInstitution(): SelectMenu {
    return SelectMenu.findByName(this.page, {
      type: ElementType.Dropdown,
      dataTestId: 'verifiedInstitution',
      ancestorLevel: 0
    });
  }

  getSaveButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Save });
  }

  getCancelButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Cancel });
  }

  getAccountAccessSwitch(): Switch {
    return Switch.findByName(this.page, { containsText: 'Account' });
  }

  getAccessStatusTable(): DataTable {
    const table = new DataTable(this.page);
    // Append to table xpath with xpath that check for table title. There's another data table in same page. It's the Egress Alert table.
    table.setXpath(`${table.getXpath()}[./preceding-sibling::*[contains(., "Access status")]]`);
    return table;
  }

  async getEmailErrorMessage(): Promise<string> {
    const emailErrorElement = await this.page.waitForXPath('//*[@data-test-id="email-error-message"]', {
      visible: true
    });
    return getPropValue<string>(emailErrorElement, 'innerText');
  }

  async getEmailErrorMessageLink(): Promise<ElementHandle> {
    return this.page.waitForXPath('//*[@data-test-id="email-error-message"]/a', {
      visible: true
    });
  }

  async getBypassSwitchCell(rowValue: string): Promise<Cell> {
    const accountAccessTable = this.getAccessStatusTable();
    return await accountAccessTable.getCellByValue(rowValue, 'Bypass');
  }

  async getBypassSwitchForRow(rowValue: string): Promise<Switch> {
    const cell = await this.getBypassSwitchCell(rowValue);
    return new Switch(this.page, defaultSwitchXpath, cell);
  }
}
