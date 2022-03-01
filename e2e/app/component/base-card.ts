import { ElementHandle, Page } from 'puppeteer';
import { MenuOption } from 'app/text-labels';
import Container from 'app/container';
import SnowmanMenu, { snowmanIconXpath } from 'app/component/snowman-menu';
import AuthenticatedPage from 'app/page/authenticated-page';

export default abstract class BaseCard extends Container {
  protected cardElement: ElementHandle;

  protected constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  /**
   * @deprecated
   * Replace with asElement()
   */
  asElementHandle(): ElementHandle {
    return this.cardElement.asElement();
  }

  async asElement(): Promise<ElementHandle | null> {
    if (this.getXpath() !== undefined) {
      // Refresh cardElement even if cardElement is already initialized.
      // Throws error if find fails. Error is likely caused by the element no longer exists or becomes stale.
      this.cardElement = await this.page.waitForXPath(this.getXpath(), { visible: true });
    }
    if (this.cardElement === null || this.cardElement === undefined) {
      throw new Error(`FAIL: Failed to find card element. Xpath: ${this.getXpath()}`);
    }
    return this.cardElement.asElement();
  }

  async clickSnowmanIcon(): Promise<this> {
    const iconXpath = `.${snowmanIconXpath}`;
    const [snowmanIcon] = await this.asElementHandle().$x(iconXpath);
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

  async selectSnowmanMenu(
    options: MenuOption,
    opt: { waitForNav?: boolean; pageExpected?: AuthenticatedPage } = {}
  ): Promise<void> {
    const { waitForNav, pageExpected } = opt;
    const menu = await this.getSnowmanMenu();
    await menu.select(options, { waitForNav });
    // Workaround for https://precisionmedicineinitiative.atlassian.net/browse/RW-7928
    if (pageExpected !== undefined) {
      await this.waitFor(pageExpected, { reloadIfFail: true });
    }
  }
}
