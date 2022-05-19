import AuthenticatedPage from 'app/page/authenticated-page';
import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Module from './module';

// data-test-id
export enum AccessModule {
  GOOGLE_TWO_FACTOR = 'module-TWO_FACTOR_AUTH',
  RT_TRAINING = 'module-COMPLIANCE_TRAINING',
  CT_TRAINING = 'module-CT_COMPLIANCE_TRAINING',
  DUCC = 'module-DATA_USER_CODE_OF_CONDUCT',
  RAS = 'module-RAS_LINK_LOGIN_GOV'
}

const PageTitle = 'Data Access Requirements';

export default class DataAccessRequirementsPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitForDocumentTitle(this.page, PageTitle);
    await waitWhileLoading(this.page);
    return true;
  }

  findModule(module: AccessModule): Module {
    return new Module(this.page, module);
  }

  async clickModule(module: AccessModule): Promise<Page> {
    const moduleButton = this.findModule(module);
    await moduleButton.getClickableText().click();
    // New tab opens. "browser" is a Jest-Puppeteer global variable.
    const newTarget = await browser.waitForTarget((target) => target.opener() === this.page.target());
    return newTarget.page();
  }
}
