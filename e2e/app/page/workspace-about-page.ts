import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForAttributeEquality, waitForDocumentTitle} from 'utils/waits-utils';
import {xPathOptionToXpath} from 'app/element/xpath-defaults';
import {ElementType} from 'app/xpath-options';
import AuthenticatedPage from './authenticated-page';
import {LabelAlias} from './data-page';

export const PageTitle = 'View Workspace Details';

export default class WorkspaceAboutPage extends AuthenticatedPage{

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle, 60000),
        waitWhileLoading(this.page),
        this.page.waitForXPath(xPathOptionToXpath({name: LabelAlias.About, type: ElementType.Tab}), {timeout: 60000}),
      ]);
      return true;
    } catch (err) {
      console.log(`WorkspaceAboutPage isLoaded() encountered ${err}`);
      return false;
    }
  }

  async isOpen(): Promise<boolean> {
    const selector = xPathOptionToXpath({name: LabelAlias.About, type: ElementType.Tab});
    return waitForAttributeEquality(page, {xpath: selector}, 'aria-selected', 'true');
  }

}
