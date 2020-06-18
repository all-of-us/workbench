import {ElementHandle, Page} from 'puppeteer';
import {WorkspaceAccessLevel} from 'app/page-identifiers';
import EllipsisMenu from 'app/component/ellipsis-menu';
import * as fp from 'lodash/fp';
import DataPage from 'app/page/data-page';

const WorkspaceCardSelector = {
  cardRootXpath: '//*[child::*[@data-test-id="workspace-card"]]', // finds 'workspace-card' parent container node
  cardNameXpath: '@data-test-id="workspace-card-name"',
  ellipsisXpath: './/clr-icon[@shape="ellipsis-vertical"]',
  accessLevelXpath: './/*[@data-test-id="workspace-access-level"]',
}


/**
 * WorkspaceCard represents workspace card user found on Home and All Workspaces pages.
 * A Workspace Card is element that contains a child element with attribute: @data-test-id='workspace-card'
 */
export default class WorkspaceCard {

  private cardElement: ElementHandle;

  // **********************
  // static functions
  // **********************

  /**
   * Find all visible Workspace Cards. Assume at least one Card exists.
   * @param {Page} page
   * @throws TimeoutError if fails to find Card.
   */
  static async findAllCards(page: Page): Promise<WorkspaceCard[]> {
    try {
      await page.waitForXPath(WorkspaceCardSelector.cardRootXpath, {visible: true, timeout: 1000});
    } catch (e) {
      return [];
    }
    const cards = await page.$x(WorkspaceCardSelector.cardRootXpath);
    // transform to WorkspaceCard object
    const resourceCards = cards.map(card => new WorkspaceCard(page).asCard(card));
    return resourceCards;
  }

  static async findAnyCard(page: Page): Promise<WorkspaceCard> {
    const cards = await this.findAllCards(page);
    if (cards.length === 0) {
      throw new Error('FAILED to find any Workspace card on page.');
    }
    const anyCard = fp.shuffle(cards)[0];
    return anyCard;
  }

  static async findCard(page: Page, workspaceName: string): Promise<WorkspaceCard | null> {
    const selector = `.//*[${WorkspaceCardSelector.cardNameXpath} and normalize-space(text())="${workspaceName}"]`;
    const allCards = await this.findAllCards(page);
    for (const card of allCards) {
      const handle = card.asElementHandle();
      const children = await handle.$x(selector);
      if (children.length > 0) {
        return card; // matched workspace name, found the Workspace card.
      }
      await handle.dispose(); // not it, dispose the ElementHandle.
    }
    return null; // not found
  }


  constructor(private readonly page: Page) {

  }

  async findCard(workspaceName: string): Promise<WorkspaceCard | null> {
    const selector = `.//*[${WorkspaceCardSelector.cardNameXpath} and normalize-space(text())="${workspaceName}"]`;
    await this.page.waitForXPath(WorkspaceCardSelector.cardRootXpath, {visible: true});
    const elements = await this.page.$x(WorkspaceCardSelector.cardRootXpath);
    for (const elem of elements) {
      if ((await elem.$x(selector)).length > 0) {
        return this.asCard(elem);
      }
    }
    return null;
  }

  async getWorkspaceName(): Promise<string> {
    const workspaceNameElemt = await this.cardElement.$x(`.//*[${WorkspaceCardSelector.cardNameXpath}]`);
    const jHandle = await workspaceNameElemt[0].getProperty('innerText');
    const name = await jHandle.jsonValue();
    await jHandle.dispose();
    return name.toString();
  }

  asElementHandle(): ElementHandle {
    return this.cardElement.asElement();
  }

  getEllipsis(): EllipsisMenu {
    return new EllipsisMenu(this.page, WorkspaceCardSelector.ellipsisXpath, this.asElementHandle());
  }

  /**
   * Find workspace access level.
   * @param workspaceName
   */
  async getWorkspaceAccessLevel() : Promise<unknown> {
    const [element] = await this.cardElement.$x(WorkspaceCardSelector.accessLevelXpath);
    return (await element.getProperty('innerText')).jsonValue();
  }

  /**
   * Find element with specified workspace name on the page.
   * @param {string} workspaceName
   */
  async getWorkspaceNameLink(workspaceName: string) : Promise<ElementHandle> {
    return this.page.waitForXPath(this.workspaceNameLinkSelector(workspaceName));
  }

  async getWorkspaceMatchAccessLevel(level: WorkspaceAccessLevel = WorkspaceAccessLevel.Owner): Promise<WorkspaceCard[]> {
    const matchWorkspaceArray: WorkspaceCard[] = [];
    const allWorkspaceCards = await WorkspaceCard.findAllCards(this.page);
    for (const card of allWorkspaceCards) {
      const accessLevel = await card.getWorkspaceAccessLevel();
      if (accessLevel === level) {
        matchWorkspaceArray.push(card);
      }
    }
    return matchWorkspaceArray;
  }

  /**
   * Click workspace name link in Workspace Card.
   * @param {boolean} waitForDataPage Waiting for Data page load and ready after click on Workspace name link.
   */
  async clickWorkspaceName(waitForDataPage: boolean = true): Promise<string> {
    const [elemt] = await this.asElementHandle().$x(`.//*[${WorkspaceCardSelector.cardNameXpath}]`);
    const textProp = await elemt.getProperty('textContent');
    const name = await textProp.jsonValue();
    await Promise.all([
      this.page.waitForNavigation({waitUntil: ['domcontentloaded', 'networkidle0']}),
      elemt.click(),
    ]);
    if (waitForDataPage) {
      const dataPage = new DataPage(page);
      await dataPage.waitForLoad();
    }
    return name.toString();
  }

  private asCard(elementHandle: ElementHandle): WorkspaceCard {
    this.cardElement = elementHandle;
    return this;
  }

  private workspaceNameLinkSelector(workspaceName: string): string {
    return `//*[@role='button'][./*[${WorkspaceCardSelector.cardNameXpath} and normalize-space(text())="${workspaceName}"]]`
  }

}
