import { Page } from 'puppeteer';
import { Tabs } from 'app/page/workspace-base';
import { buildXPath } from 'app/xpath-builders';
import { ElementType } from 'app/xpath-options';
import BaseElement from './base-element';
import { waitWhileLoading } from 'utils/waits-utils';
import { getStyleValue } from 'utils/element-utils';

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
    const isSelected = await this.getProperty<boolean>('aria-selected');
    if (isSelected !== null) {
      return isSelected;
    }
    // subtabs do not have property aria-selected
    const element = await this.asElementHandle();
    return !!(await getStyleValue<string>(this.page, element, 'border-bottom'));
  }
}
