import { ElementHandle, Page } from 'puppeteer';
import { MenuOption } from 'app/text-labels';
import Container from 'app/container';
import SnowmanMenu, { snowmanIconXpath } from './snowman-menu';

export default abstract class BaseCard extends Container {
  protected constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async asElement(): Promise<ElementHandle | null> {
    return this.page.waitForXPath(this.getXpath(), { visible: true });
  }

  async clickSnowmanIcon(): Promise<this> {
    const [snowmanIcon] = await (await this.asElement()).$x(`.${snowmanIconXpath}`);
    await snowmanIcon.hover();
    await snowmanIcon.click();
    await snowmanIcon.dispose();
    return this;
  }

  async getSnowmanMenu(): Promise<SnowmanMenu> {
    await this.clickSnowmanIcon();
    const snowmanMenu = new SnowmanMenu(this.page);
    await snowmanMenu.waitUntilVisible();
    return snowmanMenu;
  }

  async selectSnowmanMenu(options: MenuOption, opt: { waitForNav?: boolean } = {}): Promise<void> {
    return this.getSnowmanMenu().then((menu) => menu.select(options, opt));
  }
}
