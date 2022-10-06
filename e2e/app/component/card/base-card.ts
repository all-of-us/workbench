import { ElementHandle, Page } from 'puppeteer';
import { MenuOption } from 'app/text-labels';
import Container from 'app/container';
import SnowmanMenu, { snowmanIconXpath } from 'app/component/snowman-menu';
import { getPropValue } from 'utils/element-utils';
import AuthenticatedPage from 'app/page/authenticated-page';
import Link from 'app/element/link';

export default abstract class BaseCard extends Container {
  protected constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  abstract getNameTestId(): string;
  abstract getRootXpath(): string;

  getNameXpath(name: string): string {
    return `[.//*[${this.getNameTestId()} and normalize-space(text())="${name}"]]`;
  }

  async asElement(): Promise<ElementHandle | null> {
    return this.page.waitForXPath(this.getXpath(), { visible: true });
  }

  getSnowmanMenuIcon(): Link {
    const xpath = `${this.getXpath()}${snowmanIconXpath}`;
    return new Link(this.page, xpath);
  }

  async clickSnowmanIcon(): Promise<this> {
    const snowmanIcon = this.getSnowmanMenuIcon();
    await snowmanIcon.click();
    await snowmanIcon.dispose();
    return this;
  }

  async getCardSnowmanMenu(): Promise<SnowmanMenu> {
    await this.clickSnowmanIcon();
    const snowmanMenu = new SnowmanMenu(this.page);
    await snowmanMenu.waitUntilVisible();
    return snowmanMenu;
  }

  async selectSnowmanMenu(options: MenuOption, opt: { waitForNav?: boolean } = {}): Promise<void> {
    return this.getCardSnowmanMenu().then((menu) => menu.select(options, opt));
  }

  async getName(): Promise<string> {
    const element = await this.getNameLink().waitForXPath();
    return getPropValue<string>(element, 'innerText');
  }

  /**
   * Click resource name link.
   */
  async clickName<T extends AuthenticatedPage>(opts: { pageExpected?: T } = {}): Promise<string> {
    const { pageExpected } = opts;
    const element = await this.getNameLink().waitForXPath();
    const name = await getPropValue<string>(element, 'innerText');
    await element.click();
    if (pageExpected) {
      await pageExpected.waitForLoad();
    }
    return name;
  }

  private getNameLink(): Link {
    const xpath = `${this.getXpath()}//*[${this.getNameTestId()}]`;
    return new Link(this.page, xpath);
  }
}
