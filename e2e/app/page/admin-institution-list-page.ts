import { ElementHandle, Page } from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle } from 'utils/waits-utils';
import AdminTable from 'app/component/admin-table';
import ClrIconLink from 'app/element/clr-icon-link';

const PageTitle = 'Institution Admin | All of Us Researcher Workbench';

export const LabelAlias = {
  CreateNewInstitute: 'Institution admin table'
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

  getCreateNewInstituteBtn(): ClrIconLink {
    return ClrIconLink.findByName(this.page, { name: LabelAlias.CreateNewInstitute, iconShape: 'plus-circle' });
  }

  // click institution name on the institution-list-page
  async clickInstitutionNameLink(instituteName: string): Promise<Page> {
    const dataTable = this.getInstitutionTable();
    dataTable.getFrozenBody();
    const nameLink = await this.getinstitutionNameLink(instituteName);
    // const nameLink = this.institutionNameLinkSelector(instituteName);
    await nameLink.click();
    return page;
  }

  async getinstitutionNameLink(instituteName: string): Promise<ElementHandle> {
    return this.page.waitForXPath(this.institutionNameLinkSelector(instituteName));
  }

  private institutionNameLinkSelector(instituteName: string): string {
    return `//tr/td/a[text()="${instituteName}"]`;
  }
}
