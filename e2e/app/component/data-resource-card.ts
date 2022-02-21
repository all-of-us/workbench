import { ElementHandle, Page } from 'puppeteer';
import * as fp from 'lodash/fp';
import { getPropValue } from 'utils/element-utils';
import { LinkText, MenuOption, ResourceCard } from 'app/text-labels';
import BaseCard from './base-card';
import { waitWhileLoading } from 'utils/waits-utils';
import Modal from 'app/modal/modal';
import { logger } from 'libs/logger';

const DataResourceCardSelector = {
  cardRootXpath: '//*[@data-test-id="card"]',
  cardNameXpath: '@data-test-id="card-name"',
  cardTypeXpath: '@data-test-id="card-type"'
};

/**
 * DataResourceCard represents resource card found on Workspace's data page.
 */
export default class DataResourceCard extends BaseCard {
  // **********************
  // static functions
  // **********************

  /**
   * Find all visible Resource Cards. Assume at least one Card exists.
   * @param {Page} page
   * @throws TimeoutError if fails to find Card.
   */
  static async findAllCards(page: Page, timeOut = 2000): Promise<DataResourceCard[]> {
    await waitWhileLoading(page);
    try {
      await page.waitForXPath(DataResourceCardSelector.cardRootXpath, { visible: true, timeout: timeOut });
    } catch (e) {
      return [];
    }
    const cards = await page.$x(DataResourceCardSelector.cardRootXpath);
    // transform to WorkspaceCard object
    const resourceCards = cards.map((card) => new DataResourceCard(page).asCard(card));
    return resourceCards;
  }

  static async findAnyCard(page: Page): Promise<DataResourceCard> {
    const cards = await this.findAllCards(page);
    if (cards.length === 0) {
      return null;
    }
    return fp.shuffle(cards)[0];
  }

  static async findCard(page: Page, resourceName: string, timeout = 2000): Promise<DataResourceCard | null> {
    const selector =
      `${DataResourceCardSelector.cardRootXpath}[.//*[${DataResourceCardSelector.cardNameXpath}` +
      ` and normalize-space(text())="${resourceName}"]]`;
    return page
      .waitForXPath(selector, { timeout })
      .then((element) => {
        logger.info(`Found data resource card: "${resourceName}"`);
        return new DataResourceCard(page).asCard(element);
      })
      .catch(() => {
        logger.info(`Data resource card: "${resourceName}" is not found`);
        return null;
      });
  }

  constructor(page: Page) {
    super(page);
  }

  async findCard(
    resourceName: string,
    cardType: ResourceCard,
    opts: { timeout?: number } = {}
  ): Promise<DataResourceCard | null> {
    const { timeout = 2000 } = opts;
    const selector =
      DataResourceCardSelector.cardRootXpath +
      `[.//*[${DataResourceCardSelector.cardTypeXpath} and text()="${cardType}"]]` +
      `[.//*[${DataResourceCardSelector.cardNameXpath}` +
      ` and normalize-space(text())="${resourceName}"]]`;

    return this.page
      .waitForXPath(selector, { timeout })
      .then((element) => {
        logger.info(`Found ${cardType} card: "${resourceName}"`);
        return new DataResourceCard(this.page).asCard(element);
      })
      .catch(() => {
        logger.info(`${cardType} card: "${resourceName}" is not found`);
        return null;
      });
  }

  async findAnyCard(cardType: ResourceCard): Promise<DataResourceCard | null> {
    const cards = await this.getResourceCard(cardType);
    return cards.length ? fp.shuffle(cards)[0] : null;
  }

  async getResourceName(): Promise<string> {
    const elemt = await this.cardElement.$x(`.//*[${DataResourceCardSelector.cardNameXpath}]`);
    return getPropValue<string>(elemt[0], 'innerText');
  }

  /**
   * Find card type: Cohort, Datasets or Concept Sets.
   */
  async getCardType(): Promise<string> {
    const [element] = await this.cardElement.$x(`.//*[${DataResourceCardSelector.cardTypeXpath}]`);
    return getPropValue<string>(element, 'innerText');
  }

  /**
   * Find the resource name link in this card.
   */
  async getLink(): Promise<ElementHandle> {
    const [element] = await this.cardElement.$x(this.resourceNameLinkSelector());
    return element;
  }

  async getResourceCard(cardType: ResourceCard = ResourceCard.Cohort): Promise<DataResourceCard[]> {
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
  async clickResourceName(): Promise<string> {
    const name = await this.getResourceName();
    const elemts = await this.asElementHandle().$x(`.//*[${DataResourceCardSelector.cardNameXpath}]`);
    await Promise.all([this.page.waitForNavigation(), elemts[0].click()]);
    await waitWhileLoading(this.page);
    return name;
  }

  /**
   * Determine if resource card with specified name exists.
   * @param {string} cardName
   * @param {CardType} cardType
   */
  async cardExists(cardName: string, cardType: ResourceCard): Promise<boolean> {
    const cards = await this.getResourceCard(cardType);
    const names = await Promise.all(cards.map((item) => item.getResourceName()));
    const filteredList = names.filter((name) => name === cardName);
    return filteredList.length === 1;
  }

  async delete(cardName: string, cardType: ResourceCard): Promise<string[]> {
    const card = await this.findCard(cardName, cardType);
    await card.selectSnowmanMenu(MenuOption.Delete, { waitForNav: false });
    const modal = new Modal(this.page);
    await modal.waitForLoad();
    const modalTextContent = await modal.getTextContent();
    let link;
    switch (cardType) {
      case ResourceCard.Cohort:
        link = LinkText.DeleteCohort;
        break;
      case ResourceCard.ConceptSet:
        link = LinkText.DeleteConceptSet;
        break;
      case ResourceCard.Dataset:
        link = LinkText.DeleteDataset;
        break;
      case ResourceCard.Notebook:
        link = LinkText.DeleteNotebook;
        break;
      case ResourceCard.CohortReview:
        link = MenuOption.Delete;
        break;
      default:
        throw new Error(`Case ${cardType} handling is not defined.`);
    }
    await modal.clickButton(link, { waitForClose: true });
    await waitWhileLoading(this.page);
    logger.info(`Deleted ${cardType} "${cardName}"`);
    return modalTextContent;
  }

  private asCard(elementHandle: ElementHandle): DataResourceCard {
    this.cardElement = elementHandle;
    return this;
  }

  private resourceNameLinkSelector(): string {
    return `.//*[@role='button'][./*[${DataResourceCardSelector.cardNameXpath} and text()]]`;
  }
}
