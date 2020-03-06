import {ElementHandle, Page} from 'puppeteer';
import WebElement from './aou-elements/web-element';
const _ = require('lodash');


export const LINK_LABEL = {
  DUPLICATE: 'Duplicate',
  DELETE: 'Delete',
  EDIT: 'Edit',
  SHARE: 'Share',
};


/**
 * A Workspace Card is the parent element that contains child element with attribute: @data-test-id='workspace-card'
 */
export default class WorkspaceCard extends WebElement {

  static readonly popupRootXpath = '//*[@id="popup-root"]'; // element is not a child of workspace-card
  static readonly cardRootXpath = '//*[child::*[@data-test-id="workspace-card"]]';

  // **********************
  // static functions
  // **********************

  static async getAllCards(page: Page): Promise<WorkspaceCard[]> {
    const cards = await page.$x(this.cardRootXpath);
    // transform to WorkspaceResourceCard object
    const resourceCards = cards.map(card => new WorkspaceCard(page).asCardElement(card));
    return resourceCards;
  }

  static async getAnyCard(page: Page): Promise<WorkspaceCard> {
    const cards = await this.getAllCards(page);
    if (cards.length === 0) {
      throw new Error('FAILED to find any Workspace card on page.');
    }
    const anyCard = _.shuffle(cards)[0];
    return anyCard;
  }

  static async findCard(page: Page, workspaceName: string): Promise<WorkspaceCard | null> {
    const cards = await page.$x(WorkspaceCard.cardRootXpath);
    for (const card of cards) {
      const children = await card.$x(`.//*[@data-test-id="workspace-card-name" and text()="${workspaceName}"]`);
      if (children.length > 0) {
        return new WorkspaceCard(page).asCardElement(card);
      }
      await card.dispose();
    }
    return null;
  }


  constructor(page: Page) {
    super(page);
  }

  async getResourceCardName(): Promise<unknown> {
    const cardNameElem = await this.element.$x('.//*[@data-test-id="workspace-card-name"]');
    const jHandle = await cardNameElem[0].getProperty('innerText');
    const name = await jHandle.jsonValue();
    await jHandle.dispose();
    return name;
  }

  /**
   * Returns an array of link texts in ellipsis popup. Used for UI verification tests
   */
  async getPopupLinkTextsArray(): Promise<unknown> {
    await this.clickEllipsis();
    const selector = `${WorkspaceCard.popupRootXpath}//*[@role="button"]/text()`;
    await this.page.waitForXPath(selector, {visible: true});
    const elems = await this.page.$x(selector);
    const textsArray = [];
    for (const elem of elems) {
      textsArray.push(await (await elem.getProperty('textContent')).jsonValue());
      await elem.dispose();
    }
    return textsArray;
  }

  /*
  * Click 'Duplicate' link in ellipsis popup.
   */
  async duplicate() {
    await this.clickEllipsis();
    const selector = this.linkSelector(LINK_LABEL.DUPLICATE);
    const link = await this.page.waitForXPath(selector, {visible: true});
    await link.click();
    await link.dispose();
  }

  /*
  * Click 'Edit' link in ellipsis popup.
   */
  async edit() {
    await this.clickEllipsis();
    const selector = this.linkSelector(LINK_LABEL.EDIT);
    const link = await this.page.waitForXPath(selector, {visible: true});
    await link.click();
    await link.dispose();
  }

  /*
  * Click 'Share' link in ellipsis popup.
   */
  async share() {
    await this.clickEllipsis();
    const selector = this.linkSelector(LINK_LABEL.SHARE);
    const link = await this.page.waitForXPath(selector, {visible: true});
    await link.click();
    await link.dispose();
  }

  /*
  * Click 'Delete' link in ellipsis popup.
   */
  async delete() {
    await this.clickEllipsis();
    const selector = this.linkSelector(LINK_LABEL.DELETE);
    const link = await this.page.waitForXPath(selector, {visible: true});
    await link.click();
    await link.dispose();
  }

  asElementHandle(): ElementHandle {
    return this.element.asElement();
  }

  async getEllipsisIcon(): Promise<ElementHandle | null> {
    const ellipsis = await this.element.$x('.//clr-icon[@shape="ellipsis-vertical"]');
    if (ellipsis.length === 0) {return null;}
    return ellipsis[0];
  }

  private async clickEllipsis(): Promise<void> {
    const ellipsis = await this.getEllipsisIcon();
    await ellipsis.click();
    await ellipsis.dispose();
  }

  private asCardElement(elementHandle: ElementHandle): WorkspaceCard {
    this.element = elementHandle;
    return this;
  }

  private linkSelector(linkText: string) {
    return  `${WorkspaceCard.popupRootXpath}//*[@role='button' and text()='${linkText}']`;
  }

}
