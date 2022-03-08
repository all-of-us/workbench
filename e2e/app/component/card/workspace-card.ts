import { ElementHandle, Page } from 'puppeteer';
import { MenuOption, WorkspaceAccessLevel } from 'app/text-labels';
import { getPropValue } from 'utils/element-utils';
import BaseCard from './base-card';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { logger } from 'libs/logger';
import { asyncFilter } from 'utils/test-utils';

const WorkspaceCardSelector = {
  accessLevelXpath: './/*[@data-test-id="workspace-access-level"]',
  dateTimeXpath:
    './/*[@data-test-id="workspace-card"]//*[@data-test-id="workspace-access-level"]/following-sibling::div',
  lockedIconXpath:
    './/*[@data-test-id="workspace-card"]' +
    '//*[@data-test-id="workspace-lock"]//*[local-name()="svg" and @data-icon="lock-alt"]'
};

export enum CardXpaths {
  rootXpath = './/*[child::*[@data-test-id="workspace-card"]]', // finds 'workspace-card' parent container node
  nameXpath = '@data-test-id="workspace-card-name"',
  accessLevelXpath = '//*[@data-test-id="workspace-access-level"]',
  dateTimeXpath = '//*[@data-test-id="workspace-access-level"]/following-sibling::div',
  lockedIconXpath = '//*[@data-test-id="workspace-lock"]//*[local-name()="svg" and @data-icon="lock-alt"]'
}

/**
 * WorkspaceCard represents workspace card user found on Home and All Workspaces pages.
 * A Workspace Card is element that contains a child element with attribute: @data-test-id='workspace-card'
 */
export default class WorkspaceCard extends BaseCard {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  getNameTestId(): string {
    return '@data-test-id="workspace-card-name"';
  }

  getRootXpath(): string {
    return './/*[child::*[@data-test-id="workspace-card"]]';
  }

  getAccessLevelTestId(): string {
    return '@data-test-id="workspace-access-level"';
  }

  getDateTimeXpath(): string {
    return `${this.getRootXpath}//*[${this.getAccessLevelTestId()}]/following-sibling::div`;
  }

  /**
   * Find all visible Workspace Cards.
   * @throws TimeoutError if fails to find Card.
   */
  async findAllCards(opts: { accessLevel?: WorkspaceAccessLevel; timeout?: number } = {}): Promise<WorkspaceCard[]> {
    const { accessLevel, timeout = 5000 } = opts;

    // Wait until timeout for one card exists and visible. If none, return an empty array.
    await this.page.waitForXPath(this.getRootXpath(), { visible: true, timeout }).catch(() => {
      return [];
    });

    // Turn elements into WorkspaceCard objects.
    const elements = await this.page.$x(this.getRootXpath());
    const allCards: WorkspaceCard[] = await Promise.all(
      elements.map(async (c) => {
        const card = new WorkspaceCard(this.page);
        // Find card's name and use it to construct xpath
        const [nameElement] = await c.$x(`.//*[${card.getNameTestId()}]`);
        const name = await getPropValue<string>(nameElement, 'textContent');
        card.setXpath(`${card.getRootXpath()}${card.getNameXpath(name)}`);
        return card;
      })
    );

    if (accessLevel) {
      return asyncFilter(allCards, async (card: WorkspaceCard) => accessLevel === (await card.getAccessLevel()));
    }
    return allCards;
  }

  async findCard(opts: { name?: string; timeout?: number } = {}): Promise<WorkspaceCard | null> {
    const { name, timeout } = opts;
    const selector = name && this.getXpath() === null ? this.getRootXpath() + this.getNameXpath(name) : this.getXpath();

    return this.page
      .waitForXPath(selector, { visible: true, timeout })
      .then(() => {
        logger.info(`Found Workspace card "${name}"`);
        this.setXpath(selector);
        return this;
      })
      .catch(() => {
        logger.info(`Workspace card "${name}" is not found`);
        return null;
      });
  }

  /**
   * Find workspace access level.
   * @param workspaceName
   */
  async getAccessLevel(): Promise<string> {
    const xpath = `${this.getXpath()}//*[${this.getAccessLevelTestId()}]`;
    const element = await this.page.waitForXPath(xpath, { visible: true });
    return getPropValue<string>(element, 'innerText');
  }

  async getLastChangedTime(): Promise<string> {
    const element = await this.page.waitForXPath(this.getDateTimeXpath(), { visible: true });
    const wholeText = await getPropValue<string>(element, 'innerText');
    // datetime format is "Last Changed: 01/08/21, 05:22 PM"
    return wholeText.replace('Last Changed: ', '').trim();
  }

  /**
   * Delete workspace via Workspace card "Delete" dropdown menu option.
   */
  async delete(opts: { name?: string } = {}): Promise<string[]> {
    const { name } = opts;
    const card = await this.findCard({ name: name });
    await card.selectSnowmanMenu(MenuOption.Delete, { waitForNav: false });
    // Handle Delete Confirmation modal
    const modalText = await new WorkspaceEditPage(this.page).dismissDeleteWorkspaceModal();
    return modalText;
  }

  // Snowman menu options are all enabled for OWNER.
  // Snowman menu option "Duplicate" is enabled for WRITER and READER. Other options are disabled for WRITER and READER.
  async verifyWorkspaceCardMenuOptions(accessLevel?: string): Promise<void> {
    accessLevel = accessLevel || (await this.getAccessLevel());

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

  async getWorkspaceLockedIcon(): Promise<ElementHandle> {
    const [element] = await (await this.asElement()).$x(WorkspaceCardSelector.lockedIconXpath);
    return element;
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
}
