import CopyToWorkspaceModal from 'app/modal/copy-to-workspace-modal';
import DataResourceCard from 'app/component/data-resource-card';
import NewNotebookModal from 'app/modal/new-notebook-modal';
import Link from 'app/element/link';
import {MenuOption, Language, LinkText, ResourceCard} from 'app/text-labels';
import {Page} from 'puppeteer';
import {getPropValue} from 'utils/element-utils';
import {waitForDocumentTitle, waitWhileLoading} from 'utils/waits-utils';
import NotebookPage from './notebook-page';
import WorkspaceBase from './workspace-base';

const PageTitle = 'View Notebooks';

export default class WorkspaceAnalysisPage extends WorkspaceBase {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      waitForDocumentTitle(this.page, PageTitle),
      waitWhileLoading(this.page),
    ]);
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
  async createNotebook(notebookName: string, language: Language): Promise<NotebookPage> {
    const link = await this.createNewNotebookLink();
    await link.click();
    const modal = new NewNotebookModal(this.page);
    await modal.waitForLoad();
    await modal.fillInModal(notebookName, language);

    // Log notebook page heading.
    const pageHeadingCss = '[data-test-id="notebook-redirect"] > h2';
    const headingTextElement = await this.page.waitForSelector(pageHeadingCss, {visible: true});
    const headingText = await getPropValue<string>(headingTextElement, 'textContent');

    // Log notebook progress text message
    const progressCss = '[data-test-id="current-progress-card"]';
    const progressTextElement = await this.page.waitForSelector(progressCss, {visible: true});
    const progressText = await getPropValue<string>(progressTextElement, 'textContent');

    console.log(`${headingText}. ${progressText}`);

    // Wait for existances of important messages.
    const warningTexts = 'You are prohibited from taking screenshots or attempting in any way to remove participant-level data from the workbench.';
    const warningTextsXpath = `//*[contains(normalize-space(text()), "${warningTexts}")]`

    const authenticateTexts = 'Authenticating with the notebook server';
    const authenticateTextsXpath = `//*[@data-test-id and contains(normalize-space(), "${authenticateTexts}")]`;

    const creatingTexts = 'Creating the new notebook';
    const creatingTextsXpath = `//*[@data-test-id and contains(normalize-space(), "${creatingTexts}")]`

    const redirectingTexts = 'Redirecting to the notebook server';
    const redirectingTextsXpath = `//*[@data-test-id and contains(normalize-space(), "${redirectingTexts}")]`;

    await Promise.all([
      this.page.waitForXPath(warningTextsXpath, {visible: true}),
      this.page.waitForXPath(authenticateTextsXpath, {visible: true}),
      this.page.waitForXPath(creatingTextsXpath, {visible: true}),
      this.page.waitForXPath(redirectingTextsXpath, {visible: true}),
    ]);

    // Waiting up to 20 minutes
    await waitWhileLoading(this.page, (20 * 60 * 1000));

    const notebook = new NotebookPage(this.page, notebookName);
    await notebook.waitForLoad();
    return notebook;
  }

  async createNewNotebookLink(): Promise<Link> {
    return Link.findByName(this.page, {normalizeSpace: LinkText.CreateNewNotebook});
  }

  /**
   * Duplicate notebook using Ellipsis menu in Workspace Analysis page.
   * @param {string} notebookName The notebook name to clone from.
   */
  async duplicateNotebook(notebookName: string): Promise<string> {
    const resourceCard = new DataResourceCard(this.page);
    const notebookCard = await resourceCard.findCard(notebookName, ResourceCard.Notebook);
    await notebookCard.selectSnowmanMenu(MenuOption.Duplicate, {waitForNav: false});
    await waitWhileLoading(this.page);
    return `Duplicate of ${notebookName}`; // name of clone notebook
  }

  /**
   * Copy notebook to another Workspace using Ellipsis menu in Workspace Analysis page.
   * @param {string} notebookName The notebook name to clone from.
   * @param {string} destinationWorkspace Copy To Workspace.
   * @param {string} destinationNotebookName New notebook book.
   */
  async copyNotebookToWorkspace(notebookName: string, destinationWorkspace: string, destinationNotebookName?: string): Promise<void> {
    // Open Copy modal.s
    const resourceCard = new DataResourceCard(this.page);
    const notebookCard = await resourceCard.findCard(notebookName, ResourceCard.Notebook);
    await notebookCard.selectSnowmanMenu(MenuOption.CopyToAnotherWorkspace, {waitForNav: false});
    // Fill out modal fields.
    const copyModal = await new CopyToWorkspaceModal(this.page);
    await copyModal.waitForLoad();
    await copyModal.copyToAnotherWorkspace(destinationWorkspace, destinationNotebookName);
  }

}
