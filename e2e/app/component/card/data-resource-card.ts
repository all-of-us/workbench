import { ElementHandle, Page } from 'puppeteer';
import * as fp from 'lodash/fp';
import { getPropValue } from 'utils/element-utils';
import { LinkText, MenuOption, ResourceCard } from 'app/text-labels';
import BaseCard from './base-card';
import { waitWhileLoading } from 'utils/waits-utils';
import Modal from 'app/modal/modal';
import { logger } from 'libs/logger';
import Textbox from 'app/element/textbox';
import Textarea from 'app/element/textarea';
import DataTable from 'app/component/data-table';
import SnowmanMenu from 'app/component/snowman-menu';
import Link from 'app/element/link';
// import Table from 'app/component/table';

/**
 * DataResourceCard represents resource card found on Workspace's data page.
 */
export default class DataResourceCard extends BaseCard {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  getNameTestId(): string {
    return '@data-test-id="card-name"';
  }

  getCardTypeTestId(): string {
    return '@data-test-id="card-type"';
  }

  getRootXpath(): string {
    return '//*[@data-test-id="card"]';
  }

  getCardTypeXpath(cardType: string): string {
    return `[.//*[${this.getCardTypeTestId()} and text()="${cardType}"]]`;
  }

  /**
   * Find all visible Resource Cards. Assume at least one Card exists.
   * @param {Page} page
   * @throws TimeoutError if fails to find Card.
   */
  async findAllCards(opts: { timeout?: number } = {}): Promise<DataResourceCard[]> {
    const { timeout = 5000 } = opts;

    try {
      // Wait until timeout for one card exists and visible. If none, return an empty array.
      await this.page.waitForXPath(this.getRootXpath(), { visible: true, timeout });
    } catch (e) {
      return [];
    }

    const elements = await this.page.$x(this.getRootXpath());
    // transform to DataResourceCard array
    const resourceCards: DataResourceCard[] = await Promise.all(
      elements.map(async (c) => {
        const card = new DataResourceCard(this.page);
        // Find card's name and use it to construct xpath
        const [nameElement] = await c.$x(`.//*[${card.getNameTestId()}]`);
        const name = await getPropValue<string>(nameElement, 'textContent');
        card.setXpath(`${card.getRootXpath()}${card.getNameXpath(name)}`);
        return card;
      })
    );
    return resourceCards;
  }

  async findResourceTableEntryByName(opts: { name?: string }): Promise<ElementHandle> {
    const { name } = opts;

    await waitWhileLoading(this.page);

    const datatable = new DataTable(this.page);
    await waitWhileLoading(this.page);

    const dataTableExist = await datatable.exists();
    if (dataTableExist) {
      const dataTableRows = await datatable.getRowCount();
      let index = 1;
      while (index <= dataTableRows) {
        const nameCell = await datatable.getCellValue(index, 3);
        if (nameCell === name) {
          return await datatable.getCell(index, 3);
        }
        index++;
      }
    }
    return null;
  }

  async findResourceSnowManEntryByName(opts: { name?: string }): Promise<string> {
    const { name } = opts;
    await waitWhileLoading(this.page);

    const datatable = new DataTable(this.page);
    await waitWhileLoading(this.page);

    const e = await datatable.exists();
    if (e) {
      const dataTableRows = await datatable?.getRowCount();
      let index = 1;
      while (index <= dataTableRows) {
        const resourceTypeCell = await datatable.getCellValue(index, 3);
        if (name === resourceTypeCell) {
          return datatable.getCellXpath(index, 1);
        }
        index++;
      }
    }
    return null;
  }

  async selectSnowmanMenu(options: MenuOption, opt: { name?: string; waitForNav?: boolean } = {}): Promise<void> {
    const { name } = opt;
    const snowmanIcon = await this.findResourceSnowManEntryByName({ name });
    if (!snowmanIcon) {
      throw new Error(` card "${name}"`);
    }

    const snowMan = new Link(this.page, snowmanIcon);
    await snowMan.click();
    await snowMan.dispose();
    const snowmanMenu = new SnowmanMenu(this.page);
    await snowmanMenu.waitUntilVisible();
    await snowmanMenu.select(options, { waitForNav: false });
    return;
  }

  async findCard(
    opts: { name?: string; cardType?: ResourceCard; timeout?: number } = {}
  ): Promise<DataResourceCard | null> {
    const { name, cardType, timeout } = opts;
    const appendCardTypeXpath = cardType ? this.getCardTypeXpath(cardType) : '';
    const appendNameXpath = name ? this.getNameXpath(name) : '';
    const selector =
      name || cardType ? `${this.getRootXpath()}${appendCardTypeXpath}${appendNameXpath}` : this.getXpath();
    if (selector === null || selector.length === 0) {
      throw new Error('INVALID XPATH: Resource card xpath is invalid.');
    }

    return this.page
      .waitForXPath(selector, { timeout, visible: true })
      .then(() => {
        logger.info(`Found ${cardType} card "${name}"`);
        this.setXpath(selector);
        return this;
      })
      .catch(() => {
        logger.warn(`${cardType} card "${name}" is not found`);
        return null;
      });
  }

  async findAnyCard(cardType: ResourceCard): Promise<DataResourceCard | null> {
    const cards = await this.getResourceCard(cardType);
    return cards.length > 0 ? fp.shuffle(cards)[0] : null;
  }

  /**
   * Find card type: Cohort, Datasets or Concept Sets.
   */
  async getCardType(): Promise<string> {
    const xpath = `${this.getXpath()}//*[${this.getCardTypeTestId()}]`;
    const element = await this.page.waitForXPath(xpath, { visible: true });
    return getPropValue<string>(element, 'innerText');
  }

  async getResourceCard(cardType: ResourceCard = ResourceCard.Cohort): Promise<DataResourceCard[]> {
    const matchArray: DataResourceCard[] = [];
    const allCards = await this.findAllCards();
    for (const card of allCards) {
      const ctype = await card.getCardType();
      if (ctype === cardType) {
        matchArray.push(card);
      }
    }
    return matchArray;
  }

  /**
   * Determine if resource entry with specified name exists.
   * @param {string} cardName
   * @param {CardType} cardType
   */
  async resourceTableEntryExists(cardName: string): Promise<boolean> {
    return (await this.findResourceTableEntryByName({ name: cardName })) !== null;
  }

  /**
   * Determine if resource card with specified name exists.
   * @param {string} cardName
   * @param {CardType} cardType
   */
  async cardExists(cardName: string, cardType: ResourceCard, opts: { timeout?: number } = {}): Promise<boolean> {
    return (await this.findCard({ name: cardName, timeout: opts.timeout, cardType })) !== null;
  }

  async delete(name: string, cardType: ResourceCard): Promise<string[]> {
    const card = await this.findCard({ name, cardType });
    if (!card) {
      throw new Error(`ERROR: Failed to find ${cardType} card "${name}"`);
    }

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
    logger.info(`Deleted ${cardType} "${name}"`);
    return modalTextContent;
  }

  async deleteFromTable(name: string, cardType: ResourceCard): Promise<string[]> {
    const snowmanIcon = await this.findResourceSnowManEntryByName({ name });
    if (!snowmanIcon) {
      throw new Error(`ERROR: Failed to find ${cardType} card "${name}"`);
    }

    const snowMan = new Link(this.page, snowmanIcon);
    await snowMan.click();
    await snowMan.dispose();
    const snowmanMenu = new SnowmanMenu(this.page);
    await snowmanMenu.waitUntilVisible();
    await snowmanMenu.select(MenuOption.Delete, { waitForNav: false });

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
    logger.info(`Deleted ${cardType} "${name}"`);
    return modalTextContent;
  }

  /**
   * Rename Notebook, Concept Set, Dataset or Cohorts thru the snowman menu located inside the Dataset Resource card.
   * @param {string} name
   * @param {string} newName
   */
  async rename(name: string, newName: string, resourceType: ResourceCard): Promise<string[]> {
    // Find the Data resource card that match the resource name.
    const resourceCard = new DataResourceCard(this.page);
    const card = await resourceCard.findCard({ name, cardType: resourceType });
    if (!card) {
      throw new Error(`ERROR: Failed to find ${resourceType} card "${name}"`);
    }

    let option: MenuOption;
    switch (resourceType) {
      case ResourceCard.Dataset:
        option = MenuOption.RenameDataset;
        break;
      default:
        option = MenuOption.Rename;
        break;
    }
    await card.selectSnowmanMenu(option, { waitForNav: false });

    const modal = new Modal(this.page);
    await modal.waitForLoad();
    const modalTextContents = await modal.getTextContent();

    // Type new name.
    const newNameTextbox = new Textbox(this.page, `${modal.getXpath()}//*[@id="new-name"]`);
    await newNameTextbox.type(newName);

    // Type description. Notebook rename modal does not have Description textarea.
    if (resourceType !== ResourceCard.Notebook) {
      const descriptionTextarea = Textarea.findByName(this.page, { containsText: 'Description:' }, modal);
      await descriptionTextarea.type(`Puppeteer automation test. Rename ${name}.`);
    }

    let buttonLink;
    switch (resourceType) {
      case ResourceCard.Cohort:
        buttonLink = LinkText.RenameCohort;
        break;
      case ResourceCard.ConceptSet:
        buttonLink = LinkText.RenameConceptSet;
        break;
      case ResourceCard.Dataset:
        buttonLink = LinkText.RenameDataset;
        break;
      case ResourceCard.Notebook:
        buttonLink = LinkText.RenameNotebook;
        break;
      case ResourceCard.CohortReview:
        buttonLink = LinkText.RenameCohortReview;
        break;
      default:
        throw new Error(`Case ${resourceType} handling is not defined.`);
    }

    await modal.clickButton(buttonLink, { waitForClose: true });
    await waitWhileLoading(this.page);
    logger.info(`Renamed ${resourceType} "${name}" to "${newName}"`);
    return modalTextContents;
  }
}
