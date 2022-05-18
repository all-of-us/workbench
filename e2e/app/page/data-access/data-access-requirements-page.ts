import AuthenticatedPage from 'app/page/authenticated-page';
import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { getStyleValue } from 'utils/element-utils';

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

  getModuleButton(module: AccessModule): Button {
    return new Button(this.page, `//*[@data-test-id="${module}"]/*[@role="button"]`);
  }

  async clickModuleButton(module: AccessModule): Promise<Page> {
    await this.getModuleButton(module).click();
    // New tab opens. "browser" is a Jest-Puppeteer global variable.
    const newTarget = await browser.waitForTarget((target) => target.opener() === this.page.target());
    return newTarget.page();
  }

  // Find green check icon that indicates module has been completed
  async hasCompletedModule(module: AccessModule): Promise<boolean> {
    const ICON_GREEN_COLOR = 'rgb(139, 201, 144)';
    const checkIcon = await this.page.waitForXPath(
      `//*[@data-test-id="${module}"]//*[local-name()="svg" and @data-icon="check-circle"]`,
      { visible: true }
    );
    const backgroundColor = await getStyleValue<string>(page, checkIcon, 'color');
    return ICON_GREEN_COLOR === backgroundColor;
  }
}
