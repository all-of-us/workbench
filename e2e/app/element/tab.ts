import { Page } from 'puppeteer';
import { Tabs } from 'app/text-labels';
import { buildXPath } from 'app/xpath-builders';
import { ElementType } from 'app/xpath-options';
import BaseElement from './base-element';
import { waitWhileLoading } from 'utils/waits-utils';
import { getPropValue, getStyleValue } from 'utils/element-utils';
import AuthenticatedPage from 'app/page/authenticated-page';

export default class Tab extends BaseElement {
  constructor(page: Page, tabName: Tabs) {
    super(page, buildXPath({ name: tabName, type: ElementType.Tab }));
  }

  async click(): Promise<void> {
    const tab = await this.waitForXPath();
    await tab.click();
    await waitWhileLoading(this.page);
  }

  async isSelected(): Promise<boolean> {
    const element = await this.asElementHandle();
    const selected = await getPropValue<string>(element, 'ariaSelected');
    return selected !== undefined
      ? selected === 'true'
      : (await getStyleValue<string>(this.page, element, 'border-bottom')) !== null;
  }


  async waitFor(page: AuthenticatedPage): Promise<void> {
    await page.waitForLoad();
  }
}
