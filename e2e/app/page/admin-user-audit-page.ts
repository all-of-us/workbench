import { Page } from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import Textbox from 'app/element/textbox';
import Link from 'app/element/link';

const PageTitle = 'User Audit | All of Us Researcher Workbench';

export default class UserAuditPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle)]);
    return true;
  }

  getAuditButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Audit });
  }

  getDownloadSqlButton(): Button {
    return Button.findByName(this.page, { name: LinkText.DownloadSql });
  }

  getUsernameInput(): Textbox {
    const selector = "//label[text()='Username without domain']/following-sibling::div/input[@type='text']";
    return new Textbox(this.page, selector);
  }

  async getUsernameValue(): Promise<string> {
    const usernameInput = this.getUsernameInput();
    const usernameValue = await usernameInput.getProperty<string>('value');
    return usernameValue;
  }

  async clickUserAdminLink(): Promise<Page> {
    const link = new Link(this.page, '//a[text()="User"]');
    await link.click();
    return page;
  }
}
