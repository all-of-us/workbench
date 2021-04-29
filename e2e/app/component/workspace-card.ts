import { ElementHandle, Page } from 'puppeteer';
import * as fp from 'lodash/fp';
import { MenuOption, WorkspaceAccessLevel } from 'app/text-labels';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { getPropValue } from 'utils/element-utils';
import CardBase from './card-base';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { waitWhileLoading } from 'utils/waits-utils';
import { logger } from 'libs/logger';

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
    const card = await WorkspaceCard.findCard(page, workspaceName);
    await card.selectSnowmanMenu(MenuOption.Delete, { waitForNav: false });
    // Handle Delete Confirmation modal
    const modalText = new WorkspaceEditPage(page).dismissDeleteWorkspaceModal();
    await WorkspaceCard.waitUntilGone(page, workspaceName);
    return modalText;
  }

  /**
   * Find all visible Workspace Cards.
   * @param {Page} page
   * @throws TimeoutError if fails to find Card.
   */
  static async findAllCards(page: Page, accessLevel?: WorkspaceAccessLevel): Promise<WorkspaceCard[]> {
    try {
      await waitWhileLoading(page);
      await page.waitForXPath(WorkspaceCardSelector.cardRootXpath, { visible: true, timeout: 1000 });
    } catch (e) {
      return [];
    }
    const workspaceCards = (await page.$x(WorkspaceCardSelector.cardRootXpath)).map((card) =>
      new WorkspaceCard(page).asCard(card)
    );

    const filtered: WorkspaceCard[] = [];
    if (accessLevel !== undefined) {
      for (const card of workspaceCards) {
        const cardAccessLevel = await card.getWorkspaceAccessLevel();
        if (cardAccessLevel === accessLevel) {
          filtered.push(card);
        }
      }
      return filtered;
    }
    return workspaceCards;
  }

  static async findAnyCard(page: Page): Promise<WorkspaceCard> {
    const cards = await this.findAllCards(page);
    if (cards.length === 0) {
      throw new Error('FAILED to find any Workspace card on page.');
    }
    return fp.shuffle(cards)[0];
  }

  static async findCard(page: Page, workspaceName: string): Promise<WorkspaceCard | null> {
    const selector = `.//*[${WorkspaceCardSelector.cardNameXpath} and normalize-space(text())="${workspaceName}"]`;
    const allCards = await this.findAllCards(page);
    for (const card of allCards) {
      const handle = card.asElementHandle();
      const children = await handle.$x(selector);
      if (children.length > 0) {
        logger.info(`Found "${workspaceName}" workspace card`);
        return card; // matched workspace name, found the Workspace card.
      }
      await handle.dispose(); // not it, dispose the ElementHandle.
    }
    logger.info(`"${workspaceName}" workspace card not found`);
    return null; // not found
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
      await this.page.waitForXPath(WorkspaceCardSelector.cardRootXpath, { visible: true });
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
    const [element] = await this.asElementHandle().$x(`.//*[${WorkspaceCardSelector.cardNameXpath}]`);
    const name = await getPropValue<string>(element, 'textContent');
    if (waitForDataPage) {
      await Promise.all([
        this.page.waitForNavigation({ waitUntil: ['load', 'domcontentloaded', 'networkidle0'] }),
        element.click()
      ]);
      const dataPage = new WorkspaceDataPage(this.page);
      await dataPage.waitForLoad();
    } else {
      await element.click();
      await waitWhileLoading(this.page);
    }
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

  // if the snowman menu options for WRITER & READER are disabled except duplicate option and all options are enabled for OWNER.
  async verifyWorkspaceCardMenuOptions(accessLevel?: string): Promise<void> {
    const snowmanMenu = await this.getSnowmanMenu();
    accessLevel = accessLevel || (await this.getWorkspaceAccessLevel());
    if (accessLevel !== WorkspaceAccessLevel.Owner) {
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Share)).toBe(true);
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(true);
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(true);
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    } else if (accessLevel === WorkspaceAccessLevel.Owner) {
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Share)).toBe(false);
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(false);
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(false);
      expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(false);
    }
  }

}
