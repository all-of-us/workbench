import { Frame, Page } from 'puppeteer';
import Link from 'app/element/link';
import AuthenticatedPage from './authenticated-page';
import NotebookPage from './notebook-page';
import WorkspaceAnalysisPage from './workspace-analysis-page';
import { waitWhileLoading } from 'utils/waits-utils';
import { initializeRuntimeIfModalPresented } from 'utils/runtime-utils';
import { getFormattedPreviewCode, waitForPreviewCellsRendered } from 'utils/notebook-preview-utils';

const Selector = {
  editLink: '//div[contains(normalize-space(text()), "Edit")]',
  runPlaygroundModeLink: '//*[contains(normalize-space(text()), "Run")]',
  previewLink: '//*[contains(normalize-space(text()), "Preview")]'
};

export default class NotebookPreviewPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      this.page.waitForXPath(Selector.editLink, { visible: true }),
      this.page.waitForXPath(Selector.runPlaygroundModeLink, { visible: true }),
      this.page.waitForXPath(Selector.previewLink, { visible: true })
    ]);
    await this.waitForNotebookCellsRendered();
    await waitWhileLoading(this.page);
    return true;
  }

  /**
   * Click "Edit" link to open notebook in Edit mode.
   */
  async openEditMode(notebookName: string): Promise<NotebookPage> {
    const editLink = this.getEditLink();
    await editLink.waitUntilEnabled();
    await editLink.click();

    await initializeRuntimeIfModalPresented(this.page);

    // Restarting notebook server may take a while.
    await waitWhileLoading(this.page, { timeout: 15 * 60 * 1000 });

    const notebookPage = new NotebookPage(this.page, notebookName);
    await notebookPage.waitForLoad();
    return notebookPage;
  }

  async waitForNotebookCellsRendered(): Promise<void> {
    return waitForPreviewCellsRendered(await this.findNotebookIframe());
  }

  async getFormattedCode(): Promise<string[]> {
    return getFormattedPreviewCode(await this.findNotebookIframe());
  }

  getEditLink(): Link {
    return new Link(this.page, Selector.editLink);
  }

  getRunPlaygroundModeLink(): Link {
    return new Link(this.page, Selector.runPlaygroundModeLink);
  }

  /**
   * Click "Analysis" link, goto Workspace Analysis page.
   * This function does not handle Unsaved Changes confirmation.
   */
  async goAnalysisPage(): Promise<WorkspaceAnalysisPage> {
    const analysisLink = Link.findByName(this.page, { name: 'Analysis' });
    await analysisLink.clickAndWait();
    await waitWhileLoading(this.page);

    const analysisPage = new WorkspaceAnalysisPage(this.page);
    await analysisPage.waitForLoad();
    return analysisPage;
  }

  private async findNotebookIframe(): Promise<Frame> {
    const iframeElement = await this.page.waitForSelector('#notebook-frame');
    return iframeElement.contentFrame();
  }
}
