import { ElementHandle, Frame, Page, WaitForSelectorOptions } from 'puppeteer';
import Link from 'app/element/link';
import { getPropValue } from 'utils/element-utils';
import AuthenticatedPage from './authenticated-page';
import NotebookPage from './notebook-page';
import WorkspaceAnalysisPage from './workspace-analysis-page';
import { waitWhileLoading } from 'utils/waits-utils';

const Selector = {
  editLink: '//div[normalize-space(text())="Edit"]',
  runPlaygroundModeLink: '//div[normalize-space(text())="Run (Playground Mode)"]'
};

export default class NotebookPreviewPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitWhileLoading(this.page);
    await Promise.all([
      this.page.waitForXPath(Selector.editLink, { visible: true }),
      this.page.waitForXPath(Selector.runPlaygroundModeLink, { visible: true }),
      this.page.waitForXPath('//*[text()="Preview (Read-Only)"]', { visible: true })
    ]);
    return true;
  }

  /**
   * Click "Edit" link to open notebook in Edit mode.
   */
  async openEditMode(notebookName: string): Promise<NotebookPage> {
    await this.getEditLink().click();
    // Restarting notebook server may take a while.
    await waitWhileLoading(this.page, 60 * 20 * 1000);

    const notebookPage = new NotebookPage(this.page, notebookName);
    await notebookPage.waitForLoad();
    return notebookPage;
  }

  async getFormattedCode(): Promise<string[]> {
    const css = '#notebook-container pre';
    await this.waitForCssSelector(css);
    const textContents = await this.findNotebookIframe().then((frame) => {
      return frame.$$(css);
    });
    return Promise.all(textContents.map(async (content) => await getPropValue<string>(content, 'textContent')));
  }

  getEditLink(): Link {
    return new Link(this.page, Selector.editLink);
  }

  getRunPlaygroundModeLink(): Link {
    return new Link(this.page, Selector.runPlaygroundModeLink);
  }

  /**
   * Click "Notebook" link, goto Workspace Analysis page.
   * This function does not handle Unsaved Changes confirmation.
   */
  async goAnalysisPage(): Promise<WorkspaceAnalysisPage> {
    const notebooksLink = Link.findByName(this.page, { name: 'Notebooks' });
    await notebooksLink.clickAndWait();
    await waitWhileLoading(this.page);

    const analysisPage = new WorkspaceAnalysisPage(this.page);
    await analysisPage.waitForLoad();
    return analysisPage;
  }

  private async waitForCssSelector(selector: string, options?: WaitForSelectorOptions): Promise<ElementHandle> {
    const notebookIframe = await this.findNotebookIframe();
    return notebookIframe.waitForSelector(selector, options);
  }

  private async findNotebookIframe(): Promise<Frame> {
    const iframeElement = await this.page.waitForSelector('#notebook-frame');
    return iframeElement.contentFrame();
  }
}
