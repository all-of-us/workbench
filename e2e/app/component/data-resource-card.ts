import {ElementHandle, Page} from 'puppeteer';
import EllipsisMenu from 'app/component/ellipsis-menu';
import * as fp from 'lodash/fp';
import Link from '../element/link';

const DataResourceCardSelector = {
  cardRootXpath: '//*[@data-test-id="card"]',
  cardNameXpath: '@data-test-id="card-name"',
  ellipsisXpath: './/clr-icon[@shape="ellipsis-vertical"]',
  cardTypeXpath: './/*[@data-test-id="card-type"]',
}

export enum CardType {
  Cohort = 'Cohort',
  ConceptSet = 'Concept Set',
  Notebook = 'Notebook',
  Dataset = 'Dataset',
  CohortReview = 'Cohort Review',
}

/**
 * DataResourceCard represents resource card found on Workspace's data page.
 */
export default class DataResourceCard {

  private cardElement: ElementHandle;

  // **********************
  // static functions
  // **********************

  /**
   * Find all visible Resource Cards. Assume at least one Card exists.
   * @param {Page} page
   * @throws TimeoutError if fails to find Card.
   */
  static async findAllCards(page: Page, timeOut: number = 2000): Promise<DataResourceCard[]> {
    try {
      await page.waitForXPath(DataResourceCardSelector.cardRootXpath, {visible: true, timeout: timeOut});
    } catch (e) {
      return [];
    }
    const cards = await page.$x(DataResourceCardSelector.cardRootXpath);
    // transform to WorkspaceCard object
    const resourceCards = cards.map(card => new DataResourceCard(page).asCard(card));
    return resourceCards;
  }

  static async findAnyCard(page: Page): Promise<DataResourceCard> {
    const cards = await this.findAllCards(page);
    if (cards.length === 0) {
      return null;
    }
    return fp.shuffle(cards)[0];
  }

  static async findCard(page: Page, resourceName: string, timeOut: number = 60000): Promise<DataResourceCard | null> {
    const selector = `.//*[${DataResourceCardSelector.cardNameXpath} and normalize-space(text())="${resourceName}"]`;
    try {
      await page.waitForXPath(selector, {visible: true, timeout: timeOut});
    } catch (err) {
      return null;
    }
    const allCards = await this.findAllCards(page);
    for (const card of allCards) {
      const handle = card.asElementHandle();
      const children = await handle.$x(selector);
      if (children.length > 0) {
        return card; // matched resource name.
      }
      await handle.dispose(); // not it, dispose the ElementHandle.
    }
    return null; // not found
  }


  constructor(private readonly page: Page) {

  }

  async findCard(resourceName: string): Promise<DataResourceCard | null> {
    const selector = `.//*[${DataResourceCardSelector.cardNameXpath} and normalize-space(text())="${resourceName}"]`;
    await this.page.waitForXPath(DataResourceCardSelector.cardRootXpath, {visible: true});
    const elements = await this.page.$x(DataResourceCardSelector.cardRootXpath);
    for (const elem of elements) {
      if ((await elem.$x(selector)).length > 0) {
        return this.asCard(elem);
      }
    }
    return null;
  }

  async getResourceName(): Promise<string> {
    const elemt = await this.cardElement.$x(`.//*[${DataResourceCardSelector.cardNameXpath}]`);
    const jHandle = await elemt[0].getProperty('innerText');
    const name = await jHandle.jsonValue();
    await jHandle.dispose();
    return name.toString();
  }

  asElementHandle(): ElementHandle {
    return this.cardElement.asElement();
  }

  getEllipsis(): EllipsisMenu {
    return new EllipsisMenu(this.page, DataResourceCardSelector.ellipsisXpath, this.asElementHandle());
  }

  /**
   * Find card type: Cohort, Datasets or Concept Sets.
   */
  async getCardType() : Promise<unknown> {
    const [element] = await this.cardElement.$x(DataResourceCardSelector.cardTypeXpath);
    return (await element.getProperty('innerText')).jsonValue();
  }

  /**
   * Find the resource name link in this card.
   */
  async getLink() : Promise<Link> {
    return new Link(this.page, this.resourceNameLinkSelector());
  }

  async getResourceCard(cardType: CardType = CardType.Cohort): Promise<DataResourceCard[]> {
    const matchArray: DataResourceCard[] = [];
    const allCards = await DataResourceCard.findAllCards(this.page);
    for (const card of allCards) {
      const ctype = await card.getCardType();
      if (ctype === cardType) {
        matchArray.push(card);
      }
    }
    return matchArray;
  }

  /**
   * Click resource name link.
   */
  async clickResourceName(): Promise<void> {
    const elemts = await this.asElementHandle().$x(`.//*[${DataResourceCardSelector.cardNameXpath}]`);
    await Promise.all([
      this.page.waitForNavigation({waitUntil: ['domcontentloaded', 'networkidle0']}),
      elemts[0].click(),
    ]);
  }

  /**
   * Determine if resource card with specified name exists.
   * @param {string} cardName
   * @param {CardType} cardType
   */
  async cardExists(cardName: string, cardType: CardType):  Promise<boolean> {
    const cards = await this.getResourceCard(cardType);
    const names = await Promise.all(cards.map(item => item.getResourceName()));
    const filterdList = names.filter(name => name === cardName);
    return filterdList.length === 1;
  }

  private asCard(elementHandle: ElementHandle): DataResourceCard {
    this.cardElement = elementHandle;
    return this;
  }

  private resourceNameLinkSelector(): string {
    return `//*[@role='button'][./*[${DataResourceCardSelector.cardNameXpath} and @text()]]`
  }

}
