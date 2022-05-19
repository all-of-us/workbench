import { ElementHandle, Page } from 'puppeteer';
import Container from 'app/container';
import Button from 'app/element/button';
import { AccessModule } from 'app/page/data-access/data-access-requirements-page';
import { getStyleValue } from 'utils/element-utils';

export default class Module extends Container {
  private readonly rootXpath: string;

  constructor(page: Page, module: AccessModule, container?: Container) {
    super(page);
    this.rootXpath =
      container === undefined
        ? `//*[@data-test-id="${module}"]`
        : `${container.getXpath()}//*[@data-test-id="${module}"]`;
  }

  getClickableText(): Button {
    return new Button(this.page, `${this.rootXpath}/*[@role="button"]`);
  }

  async getCheckIcon(): Promise<ElementHandle> {
    return this.page.waitForXPath(`${this.rootXpath}//*[local-name()="svg" and @data-icon="check-circle"]`, {
      visible: true
    });
  }

  // Find green check icon that indicates module has been completed
  async hasCompletedModule(): Promise<boolean> {
    const ICON_GREEN_COLOR = 'rgb(139, 201, 144)';
    const checkIcon = await this.getCheckIcon();
    const backgroundColor = await getStyleValue<string>(page, checkIcon, 'color');
    return ICON_GREEN_COLOR === backgroundColor;
  }
}
