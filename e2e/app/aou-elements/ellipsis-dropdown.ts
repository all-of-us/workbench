import {ElementHandle, Page} from 'puppeteer';
import {workspaceAction} from 'util/enums';

export default class Ellipsis{

   // xpath = './/*[data-test-id="workspace-menu-button"]'

  readonly popupRootXpath = '//*[@id="popup-root"]';

  constructor(private readonly page: Page,
              private readonly xpath: string,
              private readonly parent?: ElementHandle) {
  }

   /*
  * Click 'Duplicate' link in ellipsis popup.
   */
  async duplicate() {
    await this.selectAction(workspaceAction.DUPLICATE);
  }

   /*
   * Click 'Edit' link in ellipsis popup.
    */
  async edit() {
    await this.selectAction(workspaceAction.EDIT);
  }

   /*
   * Click 'Share' link in ellipsis popup.
    */
  async share() {
    await this.selectAction(workspaceAction.SHARE);
  }

   /*
   * Click 'Delete' link in ellipsis popup.
    */
  async delete() {
    await this.selectAction(workspaceAction.DELETE);
  }

  private async selectAction(action: string) {
    await this.clickEllipsis();
    const selector = this.dropdownLinkXpath(action);
    const link = await this.page.waitForXPath(selector, {visible: true});
    await link.click();
    await link.dispose();
  }

  private async clickEllipsis(): Promise<void> {
    const element = await this.getEllipsisIcon();
    await element.click();
    await element.dispose();
  }

  private async getEllipsisIcon(): Promise<ElementHandle> {
    if (this.parent === undefined) {
      return this.page.waitForXPath(this.xpath, {visible: true});
    }
    const [elemt] = await this.parent.$x(this.xpath);
    return elemt;
  }

  private dropdownLinkXpath(linkText: string) {
    return  `${this.popupRootXpath}//*[@role='button' and text()='${linkText}']`;
  }

}