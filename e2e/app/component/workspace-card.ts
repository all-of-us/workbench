import { ElementHandle, Page } from 'puppeteer';
import * as fp from 'lodash/fp';
import { MenuOption, WorkspaceAccessLevel } from 'app/text-labels';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { getPropValue } from 'utils/element-utils';
import CardBase from './card-base';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { waitForFn, waitWhileLoading } from 'utils/waits-utils';
import { logger } from 'libs/logger';
import BaseElement from 'app/element/base-element';
import { asyncFilter } from 'utils/test-utils';

const WorkspaceCardSelector = {
  cardRootXpath: './/*[child::*[@data-test-id="workspace-card"]]', // finds 'workspace-card' parent container node
  cardNameXpath: '@data-test-id="workspace-card-name"',
  accessLevelXpath: './/*[@data-test-id="workspace-access-level"]',
  dateTimeXpath:
    './/*[@data-test-id="workspace-card"]//*[@data-test-id="workspace-access-level"]/following-sibling::div'
};

/**
 * WorkspaceCard represents workspace card user found on Home and All Workspaces pages.
 * A Workspace Card is element that contains a child element with attribute: @data-test-id='workspace-card'
 */
export default class WorkspaceCard extends CardBase {
  // **********************
  // static functions
  // **********************

  /**
   * Delete workspace via Workspace card "Delete" dropdown menu option.
   */
  static async deleteWorkspace(page: Page, workspaceName: string): Promise<string[]> {
    const card = await WorkspaceCard.findCard(page, workspaceName, 30000);
    await card.selectSnowmanMenu(MenuOption.Delete, { waitForNav: false });
    // Handle Delete Confirmation modal
    const modalText = new WorkspaceEditPage(page).dismissDeleteWorkspaceModal();
    await WorkspaceCard.waitUntilGone(page, workspaceName, 120000);
    return modalText;
  }

  /**
   * Find all visible Workspace Cards.
   * @param {Page} page
   * @throws TimeoutError if fails to find Card.
   */
  static async findAllCards(
    page: Page,
    opts: { accessLevel?: WorkspaceAccessLevel; timeout?: number } = {}
  ): Promise<WorkspaceCard[]> {
    const { accessLevel, timeout = 5000 } = opts;
    await waitWhileLoading(page);
    // Wait until timeout for one card exists and visible. If none, return an empty array.
    await page.waitForXPath(WorkspaceCardSelector.cardRootXpath, { visible: true, timeout }).catch(() => {
      return [];
    });

    // Turn elements into WorkspaceCard objects.
    const allCards: WorkspaceCard[] = (await page.$x(WorkspaceCardSelector.cardRootXpath)).map((card) =>
      new WorkspaceCard(page).asCard(card)
    );

    if (accessLevel !== undefined) {
      await asyncFilter(
        allCards,
        async (card: WorkspaceCard) => accessLevel === (await card.getWorkspaceAccessLevel())
      );
    }
    return allCards;
  }

  static async findAnyCard(page: Page): Promise<WorkspaceCard> {
    const cards = await this.findAllCards(page);
    if (cards.length === 0) {
      throw new Error('FAILED to find any Workspace card on page.');
    }
    return fp.shuffle(cards)[0];
  }

  static async findCard(page: Page, workspaceName: string, timeout = 5000): Promise<WorkspaceCard | null> {
    const selector =
      `${WorkspaceCardSelector.cardRootXpath}[.//*[${WorkspaceCardSelector.cardNameXpath}` +
      ` and normalize-space(text())="${workspaceName}"]]`;
    return page
      .waitForXPath(selector, { timeout, visible: true })
      .then((element: ElementHandle) => {
        logger.info(`Found workspace card: "${workspaceName}"`);
        return new WorkspaceCard(page).asCard(element);
      })
      .catch(() => {
        logger.info(`Workspace card: "${workspaceName}" is not found`);
        return null;
      });
  }

  static async waitUntilGone(page: Page, workspaceName: string, timeout = 60000): Promise<void> {
    const selector =
      `${WorkspaceCardSelector.cardRootXpath}//*[${WorkspaceCardSelector.cardNameXpath}` +
      ` and normalize-space(text())="${workspaceName}"]`;
    await page.waitForXPath(selector, { hidden: true, timeout });
  }

  constructor(page: Page) {
    super(page);
  }

  async findCard(workspaceName: string): Promise<WorkspaceCard | null> {
    const selector = `.//*[${WorkspaceCardSelector.cardNameXpath} and normalize-space(text())="${workspaceName}"]`;
    try {
      await this.page.waitForXPath(WorkspaceCardSelector.cardRootXpath, { visible: true, timeout: 5000 });
    } catch (err) {
      // no Workspace card.
      return null;
    }
    const elements = await this.page.$x(WorkspaceCardSelector.cardRootXpath);
    for (const elem of elements) {
      if ((await elem.$x(selector)).length > 0) {
        return this.asCard(elem);
      }
    }
    // WorkspaceName not found.
    return null;
  }

  async getWorkspaceName(): Promise<string> {
    const workspaceNameElement = await this.cardElement.$x(`.//*[${WorkspaceCardSelector.cardNameXpath}]`);
    return getPropValue<string>(workspaceNameElement[0], 'innerText');
  }

  /**
   * Find workspace access level.
   * @param workspaceName
   */
  async getWorkspaceAccessLevel(): Promise<string> {
    const [element] = await this.cardElement.$x(WorkspaceCardSelector.accessLevelXpath);
    return getPropValue<string>(element, 'innerText');
  }

  /**
   * Find element with specified workspace name on the page.
   * @param {string} workspaceName
   */
  async getWorkspaceNameLink(workspaceName: string): Promise<ElementHandle> {
    return this.page.waitForXPath(this.workspaceNameLinkSelector(workspaceName), { visible: true });
  }

  async getWorkspaceMatchAccessLevel(
    level: WorkspaceAccessLevel = WorkspaceAccessLevel.Owner
  ): Promise<WorkspaceCard[]> {
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

  async getLastChangedTime(): Promise<string> {
    const [element] = await this.cardElement.$x(WorkspaceCardSelector.dateTimeXpath);
    const wholeText = await getPropValue<string>(element, 'innerText');
    // datetime format is "Last Changed: 01/08/21, 05:22 PM"
    return wholeText.replace('Last Changed: ', '').trim();
  }

  /**
   * Click workspace name link in Workspace Card.
   * @param {boolean} waitForDataPage Waiting for Data page load and ready after click on Workspace name link.
   */
  async clickWorkspaceName(waitForDataPage = true): Promise<string> {
    const [elementHandle] = await this.asElementHandle().$x(`.//*[${WorkspaceCardSelector.cardNameXpath}]`);
    await waitForFn(() => {
      return elementHandle && elementHandle.boxModel() && elementHandle.boundingBox();
    });
    const element = BaseElement.asBaseElement(this.page, elementHandle);
    const name = await getPropValue<string>(elementHandle, 'textContent');
    if (waitForDataPage) {
      const navPromise = this.page.waitForNavigation({ waitUntil: ['load', 'networkidle0'] });
      await element.click();
      await navPromise;
      const dataPage = new WorkspaceDataPage(this.page);
      await dataPage.waitForLoad();
    } else {
      await element.click();
      await waitWhileLoading(this.page);
    }
    logger.info(`Click name "${name}" on Workspace card to open workspace.`);
    return name;
  }

  private asCard(elementHandle: ElementHandle): WorkspaceCard {
    this.cardElement = elementHandle;
    return this;
  }

  private workspaceNameLinkSelector(workspaceName: string): string {
    return (
      `//*[@role='button'][./*[${WorkspaceCardSelector.cardNameXpath}` +
      ` and normalize-space(text())="${workspaceName}"]]`
    );
  }

  // Snowman menu options are all enabled for OWNER.
  // Snowman menu option "Duplicate" is enabled for WRITER and READER. Other options are disabled for WRITER and READER.
  async verifyWorkspaceCardMenuOptions(accessLevel?: string): Promise<void> {
    accessLevel = accessLevel || (await this.getWorkspaceAccessLevel());

    const snowmanMenu = await this.getSnowmanMenu();
    const links = await snowmanMenu.getAllOptionTexts();
    expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));

    if (accessLevel === WorkspaceAccessLevel.Owner) {
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Share)).toBe(false);
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(false);
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(false);
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    } else {
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Share)).toBe(true);
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(true);
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(true);
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    }
    // Close menu
    await this.clickSnowmanIcon();
    await snowmanMenu.waitUntilClose();
  }
}
