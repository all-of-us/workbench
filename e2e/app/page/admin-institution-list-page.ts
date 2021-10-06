import { ElementHandle, Page } from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle } from 'utils/waits-utils';
import AdminTable from 'app/component/admin-table';
import ClrIconLink from 'app/element/clr-icon-link';

const PageTitle = 'Institution Admin | All of Us Researcher Workbench';

export const LabelAlias = {
  CreateNewInstitution: 'Institution admin table'
};

export default class InstitutionAdminPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle)]);
    await this.getInstitutionTable().exists();
    return true;
  }

  getInstitutionTable(): AdminTable {
    return new AdminTable(this.page);
  }

  getCreateNewInstitutionBtn(): ClrIconLink {
    return ClrIconLink.findByName(this.page, { name: LabelAlias.CreateNewInstitution, iconShape: 'plus-circle' });
  }

  // click institution name on the institution-list-page
  async clickInstitutionNameLink(institutionName: string): Promise<Page> {
    const nameLink = await this.getInstitutionNameLink(institutionName);
    await nameLink.click();
    return page;
  }

  async getInstitutionNameLink(institutionName: string): Promise<ElementHandle> {
    return this.page.waitForXPath(this.institutionNameLinkSelector(institutionName));
  }

  private institutionNameLinkSelector(institutionName: string): string {
    return `//tr/td/a[text()="${institutionName}"]`;
  }
}
