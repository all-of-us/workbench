import { ElementHandle, Page } from 'puppeteer';
import { MenuOption, WorkspaceAccessLevel } from 'app/text-labels';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { getPropValue } from 'utils/element-utils';
import BaseCard from './base-card';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { waitWhileLoading } from 'utils/waits-utils';
import { logger } from 'libs/logger';
import { asyncFilter } from 'utils/test-utils';
import Link from 'app/element/link';
import WorkspaceAboutPage from 'app/page/workspace-about-page';

const WorkspaceCardSelector = {
  cardRootXpath: './/*[child::*[@data-test-id="workspace-card"]]', // finds 'workspace-card' parent container node
  cardNameXpath: '@data-test-id="workspace-card-name"',
  accessLevelXpath: './/*[@data-test-id="workspace-access-level"]',
  dateTimeXpath:
    './/*[@data-test-id="workspace-card"]//*[@data-test-id="workspace-access-level"]/following-sibling::div',
  lockedIconXpath: 
    './/*[@data-test-id="workspace-card"]//*[@data-test-id="workspace-lock"]//*[local-name()="svg" and @data-icon="lock-alt"]'
};

/**
 * WorkspaceCard represents workspace card user found on Home and All Workspaces pages.
 * A Workspace Card is element that contains a child element with attribute: @data-test-id='workspace-card'
 */
export default class WorkspaceCard extends BaseCard {
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
    const modalText = await new WorkspaceEditPage(page).dismissDeleteWorkspaceModal();
    await WorkspaceCard.waitUntilGone(page, workspaceName, 10000);
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
      return asyncFilter(
        allCards,
        async (card: WorkspaceCard) => accessLevel === (await card.getWorkspaceAccessLevel())
      );
    }
    return allCards;
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
        logger.info(`Workspace card "${workspaceName}" was not found`);
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

  async findCard(workspaceName: string, timeout = 5000): Promise<WorkspaceCard | null> {
    const selector = `.//*[${WorkspaceCardSelector.cardNameXpath} and normalize-space(text())="${workspaceName}"]`;
    try {
      await this.page.waitForXPath(WorkspaceCardSelector.cardRootXpath, { visible: true, timeout });
    } catch (err) {
      // Workspace card not found.
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
    const workspaceName = await this.getWorkspaceName();
    const link = new Link(this.page, this.getWorkspaceNameXpath(workspaceName));
    await link.click();
    logger.info(`Click workspace name "${workspaceName}" on Workspace card to open workspace.`);

    if (waitForDataPage) {
      const dataPage = new WorkspaceDataPage(this.page);
      await dataPage.waitForLoad();
    }
    return workspaceName;
  }

  private asCard(elementHandle: ElementHandle): WorkspaceCard {
    this.cardElement = elementHandle;
    return this;
  }

  private getWorkspaceNameXpath(workspaceName: string): string {
    return (
      WorkspaceCardSelector.cardRootXpath +
      `//*[@role='button']/*[${WorkspaceCardSelector.cardNameXpath} and normalize-space(text())="${workspaceName}"]`
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

  async getWorkspaceLockedIcon(): Promise<void> {
    await this.cardElement.$x(WorkspaceCardSelector.lockedIconXpath);
  } 

  // for a locked-workspace only the edit options is enabled
  async verifyLockedWorkspaceMenuOptions(): Promise<void> {
    const snowmanMenu = await this.getSnowmanMenu();
    const links = await snowmanMenu.getAllOptionTexts();
    expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));

    expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Share)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(true);
  }


  /**
   * Clicking on a locked-workspace, user is navigated to about page
   * Click workspace name link in Workspace Card.
   * @param {boolean} waitForAboutPage Waiting for About page load and ready after click on Workspace name link.
   */
  async clickLockedWorkspaceName(waitForAboutPage: boolean = true): Promise<string> {
    const workspaceName = await this.getWorkspaceName();
    const link = new Link(this.page, this.getWorkspaceNameXpath(workspaceName));
    await link.click();
    logger.info(`Click workspace name "${workspaceName}" on Workspace card to open workspace.`);

    if (waitForAboutPage) {
      const aboutPage = new WorkspaceAboutPage(this.page);
      await aboutPage.waitForLoad();
    }
    return workspaceName;
  }
}
