import {ElementHandle, Page} from 'puppeteer';
import {WORKSPACE_ACCESS_LEVEL, WORKSPACE_ACTION} from 'resources/enums';
import BaseElement from './aou-elements/base-element';
const fp = require('lodash/fp');


/**
 * WorkspaceCard represents workspace card user found on Home and All Workspaces pages.
 * A Workspace Card is element that contains a child element with attribute: @data-test-id='workspace-card'
 */
export default class WorkspaceCard extends BaseElement {

  static readonly popupRootXpath = '//*[@id="popup-root"]'; // element is not a child of workspace-card
  static readonly cardRootXpath = '//*[child::*[@data-test-id="workspace-card"]]';

  // **********************
  // static functions
  // **********************

  /**
   * Find all visible Workspace Cards. Assume at least one Card exists.
   * @param {Page} page
   * @throws TimeoutError if fails to find Card.
   */
  static async getAllCards(page: Page): Promise<WorkspaceCard[]> {
    await page.waitForXPath(WorkspaceCard.cardRootXpath, {visible: true, timeout: 60000});
    const cards = await page.$x(this.cardRootXpath);
    // transform to WorkspaceCard object
    const resourceCards = cards.map(card => new WorkspaceCard(page).asCardElement(card));
    return resourceCards;
  }

  static async getAnyCard(page: Page): Promise<WorkspaceCard> {
    const cards = await this.getAllCards(page);
    if (cards.length === 0) {
      throw new Error('FAILED to find any Workspace card on page.');
    }
    const anyCard = fp.shuffle(cards)[0];
    return anyCard;
  }

  static async findCard(page: Page, workspaceName: string): Promise<WorkspaceCard | null> {
    const selector = `.//*[@data-test-id="workspace-card-name" and text()="${workspaceName}"]`;
    const allCards = await this.getAllCards(page);
    for (const card of allCards) {
      const children = await card.asElementHandle().$x(selector);
      if (children.length > 0) {
        return card; // matched workspace name, found the Workspace card.
      }
      await card.dispose(); // not it, dispose the ElementHandle.
    }
    return null; // not found
  }


  constructor(page: Page) {
    super(page);
  }

  async getWorkspaceName(): Promise<unknown> {
    const selector = './/*[@data-test-id="workspace-card-name"]';
    const workspaceNameElemt = await this.element.$x(selector);
    const jHandle = await workspaceNameElemt[0].getProperty('innerText');
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
    const selector = this.linkSelector(WORKSPACE_ACTION.DUPLICATE);
    const link = await this.page.waitForXPath(selector, {visible: true});
    await link.click();
    await link.dispose();
  }

  /*
  * Click 'Edit' link in ellipsis popup.
   */
  async edit() {
    await this.clickEllipsis();
    const selector = this.linkSelector(WORKSPACE_ACTION.EDIT);
    const link = await this.page.waitForXPath(selector, {visible: true});
    await link.click();
    await link.dispose();
  }

  /*
  * Click 'Share' link in ellipsis popup.
   */
  async share() {
    await this.clickEllipsis();
    const selector = this.linkSelector(WORKSPACE_ACTION.SHARE);
    const link = await this.page.waitForXPath(selector, {visible: true});
    await link.click();
    await link.dispose();
  }

  /*
  * Click 'Delete' link in ellipsis popup.
   */
  async delete() {
    await this.clickEllipsis();
    const selector = this.linkSelector(WORKSPACE_ACTION.DELETE);
    const link = await this.page.waitForXPath(selector, {visible: true});
    await link.click();
    await link.dispose();
  }

  asElementHandle(): ElementHandle {
    return this.element.asElement();
  }

  async getEllipsisIcon(): Promise<ElementHandle | null> {
    const ellipsis = await this.element.$x('.//clr-icon[@shape="ellipsis-vertical"]');
    if (ellipsis.length === 0) {
      return null;
    }
    return ellipsis[0];
  }

  /**
   * Find workspace access level.
   * @param workspaceName
   */
  async getWorkspaceAccessLevel() : Promise<unknown> {
    const element = await this.page.waitForXPath(this.accessLevelSelector(), {visible: true});
    return (await element.getProperty('innerText')).jsonValue();
  }

  /**
   * Find element with specified workspace name on the page.
   * @param {string} workspaceName
   */
  async getWorkspaceNameLink(workspaceName: string) : Promise<ElementHandle> {
    return await this.page.waitForXPath(this.workspaceNameLinkSelector(workspaceName));
  }

  async getWorkspaceMatchAccessLevel(level: WORKSPACE_ACCESS_LEVEL = WORKSPACE_ACCESS_LEVEL.OWNER): Promise<WorkspaceCard[]> {
    const matchWorkspaceArray: WorkspaceCard[] = [];
    const allWorkspaceCards = await WorkspaceCard.getAllCards(page);
    for (const card of allWorkspaceCards) {
      const accessLevel = await card.getWorkspaceAccessLevel();
      if (accessLevel === level) {
        matchWorkspaceArray.push(card);
      }
    }
    return matchWorkspaceArray;
  }

  /**
   * Click Workspace Name in Workspace Card.
   */
  async clickWorkspaceName() {
    const elemt = await this.page.waitForXPath('//*[@data-test-id="workspace-card-name"]', {visible: true});
    await Promise.all([
      this.page.waitForNavigation({waitUntil: ['domcontentloaded', 'networkidle0'], timeout: 60000}),
      elemt.click()
    ]);
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

  private accessLevelSelector() {
    return `.//*[@data-test-id='workspace-access-level']`;
  }

  private workspaceNameLinkSelector(workspaceName: string) {
    return `//*[@role='button'][./*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]`
  }

}
