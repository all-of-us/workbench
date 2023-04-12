import { Page } from 'puppeteer';
import DataResourceCard from 'app/component/card/data-resource-card';
import { LinkText, MenuOption, ResourceCard, WorkspaceAccessLevel } from 'app/text-labels';
import { waitWhileLoading } from 'utils/waits-utils';
import SnowmanMenu from 'app/component/snowman-menu';
import Modal from 'app/modal/modal';
import AuthenticatedPage from './authenticated-page';
import BaseElement from 'app/element/base-element';
import ShareModal from 'app/modal/share-modal';
import { logger } from 'libs/logger';

export const UseFreeCredits = 'Use All of Us initial credits';

export default abstract class WorkspaceBase extends AuthenticatedPage {
  protected constructor(page: Page) {
    super(page);
  }

  /**
   * Delete Notebook, Concept Set, Dataset, Cohort or Cohort Review via snowman menu located inside the data resource card.
   * @param {string} resourceName
   * @param {ResourceCard} resourceType
   */
  async deleteResource(resourceName: string, resourceType: ResourceCard): Promise<string[]> {
    return new DataResourceCard(this.page).delete(resourceName, resourceType);
  }

  /**
   * Delete Notebook, Concept Set, Dataset, Cohort or Cohort Review via snowman menu located inside the data resource table.
   * @param {string} resourceName
   * @param {ResourceCard} resourceType
   */
  async deleteResourceFromTable(resourceName: string, resourceType: ResourceCard): Promise<string[]> {
    return new DataResourceCard(this.page).deleteEntryFromTable(resourceName, resourceType);
  }

  /**
   * Rename Notebook, Concept Set, Dataset, Cohorts or Cohort Review via the snowman menu from the Resource table.
   * @param {string} resourceName
   * @param {string} newResourceName
   * @param {ResourceCard} resourceType
   */
  async renameResourceFromTable(
    resourceName: string,
    newResourceName: string,
    resourceType: ResourceCard
  ): Promise<string[]> {
    return new DataResourceCard(this.page).renameFromTable(resourceName, newResourceName, resourceType);
  }

  /**
   * Select Workspace action snowman menu option.
   * @param {MenuOption} option
   * @param opts
   */
  async selectWorkspaceAction(option: MenuOption, opts?: { waitForNav: false }): Promise<void> {
    const snowmanMenu = await this.getWorkspaceActionMenu();
    await snowmanMenu.select(option, opts);
    logger.info(`Selected Workspace Action menu option: ${option}`);
  }

  /**
   * Delete workspace via Workspace Actions snowman menu "Delete" option.
   */
  async deleteWorkspace(): Promise<string[]> {
    await this.selectWorkspaceAction(MenuOption.Delete, { waitForNav: false });
    // Handle Delete Confirmation modal
    const modalText = await this.dismissDeleteWorkspaceModal();
    logger.info('Deleted workspace');
    return modalText;
  }

  /**
   * Dismss Delete Workspace Confirmation modal by click a button.
   * @return {LinkText} clickButtonText Button to click.
   */
  async dismissDeleteWorkspaceModal(clickButtonText: LinkText = LinkText.DeleteWorkspace): Promise<string[]> {
    const modal = new Modal(this.page);
    await modal.waitForLoad();
    const modalText = await modal.getTextContent();
    const textBox = modal.waitForTextbox('type DELETE to confirm');
    await textBox.type('delete');
    await modal.clickButton(clickButtonText, { waitForClose: true });
    await waitWhileLoading(this.page);
    return modalText;
  }

  /**
   * Edit workspace via Workspace Actions snowman menu "Edit" option.
   */
  async editWorkspace(): Promise<void> {
    await this.selectWorkspaceAction(MenuOption.Edit);
  }

  async getCdrVersion(): Promise<string> {
    const xpath = '//*[@data-test-id="cdr-version"]';
    const element = BaseElement.asBaseElement(this.page, await this.page.waitForXPath(xpath, { visible: true }));
    return element.getTextContent();
  }

  async getNewCdrVersionFlag(): Promise<BaseElement> {
    const xpath = '//*[@data-test-id="new-version-flag"]';
    return BaseElement.asBaseElement(this.page, await this.page.waitForXPath(xpath, { visible: true }));
  }

  /**
   * Share workspace via Workspace Actions snowman menu "Share" option.
   */
  async shareWorkspaceWithUser(email: string, role: WorkspaceAccessLevel): Promise<void> {
    await this.selectWorkspaceAction(MenuOption.Share, { waitForNav: false });
    const shareModal = new ShareModal(this.page);
    await shareModal.waitForLoad();

    await shareModal.shareWithUser(email, role);
    await waitWhileLoading(this.page);
    await this.waitForLoad();
  }

  async getWorkspaceActionMenu(): Promise<SnowmanMenu> {
    const iconXpath = './/*[@data-test-id="workspace-menu-button"]';
    await this.page.waitForXPath(iconXpath, { visible: true }).then((icon) => icon.click());
    const snowmanMenu = new SnowmanMenu(this.page);
    return snowmanMenu;
  }

  //verify that only edit option is enabled on the workspace-action menu
  async verifyLockedWorkspaceActionOptions(): Promise<void> {
    const snowmanMenu = await this.getWorkspaceActionMenu();
    const links = await snowmanMenu.getAllOptionTexts();
    expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));

    expect(await snowmanMenu.isOptionDisabled(MenuOption.Duplicate)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(false);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Share)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(true);
  }
}
