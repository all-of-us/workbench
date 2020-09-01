import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForAttributeEquality} from 'utils/waits-utils';
import Link from 'app/element/link';
import {buildXPath} from 'app/xpath-builders';
import {ElementType} from 'app/xpath-options';
import AuthenticatedPage from './authenticated-page';
import {TabLabelAlias} from './workspace-data-page';

export default abstract class WorkspaceBase extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

   /**
    * Select DATA, ANALYSIS or ABOUT page tab.
    * @param {TabLabel} tabName
    * @param opts
    */
  async openTab(tabName: TabLabelAlias, opts: {waitPageChange?: boolean} = {}): Promise<void> {
    const { waitPageChange = true } = opts;
    const selector = buildXPath({name: tabName, type: ElementType.Tab});
    const tab = new Link(this.page, selector);
    waitPageChange ? await tab.clickAndWait() : await tab.click();
    await tab.dispose();
    return waitWhileLoading(this.page);
  }

  /**
   * Is tab currently open or selected?
   */
  async isOpen(tabName: TabLabelAlias): Promise<boolean> {
    const selector = buildXPath({name: tabName, type: ElementType.Tab});
    return waitForAttributeEquality(this.page, {xpath: selector}, 'aria-selected', 'true');
  }


}
