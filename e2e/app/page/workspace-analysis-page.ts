import CopyToWorkspaceModal from 'app/modal/copy-to-workspace-modal';
import DataResourceCard from 'app/component/card/data-resource-card';
import NewNotebookModal from 'app/modal/new-notebook-modal';
import Link from 'app/element/link';
import { Language, LinkText, MenuOption, ResourceCard } from 'app/text-labels';
import { Page } from 'puppeteer';
import { getPropValue } from 'utils/element-utils';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import NotebookPage from './notebook-page';
import WorkspaceBase from './workspace-base';
import { initializeRuntimeIfModalPresented } from 'utils/runtime-utils';
import { logger } from 'libs/logger';

const PageTitle = 'View Notebooks';

export default class WorkspaceAnalysisPage extends WorkspaceBase {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle), this.createNewNotebookLink().waitUntilEnabled()]);
    await waitWhileLoading(this.page);
    return true;
  }

  /**
   * Create a new notebook.
   * - Click "Create a New Notebook" link in Analysis page.
   * - Fill in Notebook name and choose language in New Notebook modal.
   * - Wait for Jupyter notebook page load.
   * @param {string} notebookName New notebook name.
   * @param {Language} language Notebook language.
   */
  async createNotebook(notebookName: string, language: Language = Language.Python): Promise<NotebookPage> {
    const modal = new NewNotebookModal(this.page);
    let maxRetries = 3;
    const clickCreateButtonWithRetries = async (): Promise<void> => {
      const link = this.createNewNotebookLink();
      await link.click();
      try {
        await modal.waitForLoad();
        return;
      } catch (err) {
        if (maxRetries < 1) {
          throw new Error(`Click link "Create a New Notebook" failed after trying ${maxRetries} times.\n${err}`);
        }
      }
      if (maxRetries < 1) {
        throw new Error('Investigate why waitForLoad() did not throw error. It should not happen.');
      }
      maxRetries--;
      await this.page.waitForTimeout(2000).then(() => clickCreateButtonWithRetries());
    };

    await clickCreateButtonWithRetries();
    await modal.fillInModal(notebookName, language);

    // Log notebook page heading.
    const pageHeadingCss = '[data-test-id="leo-app-launcher"] > h2';
    const headingTextElement = await this.page.waitForSelector(pageHeadingCss, { visible: true });
    const headingText = await getPropValue<string>(headingTextElement, 'textContent');

    // Log notebook progress text message
    const progressCss = '[data-test-id="current-progress-card"]';
    const progressTextElement = await this.page.waitForSelector(progressCss, { visible: true });
    const progressText = await getPropValue<string>(progressTextElement, 'textContent');

    logger.info(headingText);
    logger.info(progressText.trim());

    // Wait for existences of important messages.
    const warningTexts =
      'You are prohibited from taking screenshots or attempting in any way to remove participant-level data' +
      ' from the workbench.';
    const warningTextsXpath = `//*[contains(normalize-space(text()), "${warningTexts}")]`;

    const authenticateTexts = 'Authenticating with the notebook server';
    const authenticateTextsXpath = `//*[@data-test-id and contains(normalize-space(), "${authenticateTexts}")]`;

    const creatingTexts = 'Creating the new notebook';
    const creatingTextsXpath = `//*[@data-test-id and contains(normalize-space(), "${creatingTexts}")]`;

    const redirectingTexts = 'Redirecting to the notebook server';
    const redirectingTextsXpath = `//*[@data-test-id and contains(normalize-space(), "${redirectingTexts}")]`;

    await Promise.all([
      initializeRuntimeIfModalPresented(this.page),
      this.page.waitForXPath(warningTextsXpath, { visible: true }),
      this.page.waitForXPath(authenticateTextsXpath, { visible: true }),
      this.page.waitForXPath(creatingTextsXpath, { visible: true }),
      this.page.waitForXPath(redirectingTextsXpath, { visible: true })
    ]);

    // Waiting up to 15 minutes
    await waitWhileLoading(this.page, { timeout: 15 * 60 * 1000 });
    const notebook = new NotebookPage(this.page, notebookName);
    await notebook.waitForLoad();
    return notebook;
  }

  createNewNotebookLink(): Link {
    return Link.findByName(this.page, { normalizeSpace: LinkText.CreateNewNotebook });
  }

  /**
   * Duplicate notebook using Ellipsis menu in Workspace Analysis page.
   * @param {string} notebookName The notebook name to clone from.
   */
  async duplicateNotebook(notebookName: string): Promise<string> {
    const notebookCard = await new DataResourceCard(this.page).findCard({ name: notebookName });
    await notebookCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: false });
    await waitWhileLoading(this.page);
    return `Duplicate of ${notebookName}`; // name of clone notebook
  }

  /**
   * Copy notebook to another Workspace using Ellipsis menu in Workspace Analysis page.
   * @param {string} notebookName The notebook name to clone from.
   * @param {string} destinationWorkspace Copy To Workspace.
   * @param {string} destinationNotebookName New notebook book.
   */
  async copyNotebookToWorkspace(
    notebookName: string,
    destinationWorkspace: string,
    destinationNotebookName?: string
  ): Promise<void> {
    // Open Copy modal
    const resourceCard = new DataResourceCard(this.page);
    const notebookCard = await resourceCard.findCard({ name: notebookName, cardType: ResourceCard.Notebook });
    await notebookCard.selectSnowmanMenu(MenuOption.CopyToAnotherWorkspace, { waitForNav: false });
    // Fill out modal fields.
    const copyModal = new CopyToWorkspaceModal(this.page);
    await copyModal.waitForLoad();
    await copyModal.copyToAnotherWorkspace(destinationWorkspace, destinationNotebookName);
  }

  /**
   *  Find Notebook that match specified notebook name.
   * @param notebookName
   */
  async findNotebookCard(notebookName?: string): Promise<DataResourceCard | null> {
    if (notebookName) {
      return new DataResourceCard(this.page).findCard({ name: notebookName, cardType: ResourceCard.Notebook });
    }
    // if notebook name isn't specified, find any existing notebook.
    return new DataResourceCard(this.page).findAnyCard(ResourceCard.Notebook);
  }
}
