import { Frame, Page } from 'puppeteer';
import Link from 'app/element/link';
import { getPropValue } from 'utils/element-utils';
import AuthenticatedPage from './authenticated-page';
import NotebookPage from './notebook-page';
import WorkspaceAnalysisPage from './workspace-analysis-page';
import { waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { initializeRuntimeIfModalPresented } from 'utils/runtime-utils';

const Selector = {
  editLink: '//*[contains(normalize-space(text()), "Edit")]',
  runPlaygroundModeLink: '//*[contains(normalize-space(text()), "Run")]',
  previewLink: '//*[contains(normalize-space(text()), "Preview")]'
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
      this.page.waitForXPath(Selector.previewLink, { visible: true })
    ]);
    return true;
  }

  /**
   * Click "Edit" link to open notebook in Edit mode.
   */
  async openEditMode(notebookName: string): Promise<NotebookPage> {
    const editLink = await this.getEditLink();
    await editLink.waitUntilEnabled();
    await editLink.click();

    await initializeRuntimeIfModalPresented(this.page);

    // Restarting notebook server may take a while.
    await waitWhileLoading(this.page, 60 * 20 * 1000);

    const notebookPage = new NotebookPage(this.page, notebookName);
    await notebookPage.waitForLoad();
    return notebookPage;
  }

  async getFormattedCode(): Promise<string[]> {
    const css = '#notebook .code_cell.rendered pre';
    const iframe = await this.findNotebookIframe();
    await iframe.waitForSelector(css, { visible: true });
    const elements = await iframe.$$(css);
    return Promise.all(elements.map(async (content) => await getPropValue<string>(content, 'textContent')));
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

  private async findNotebookIframe(): Promise<Frame> {
    const iframeElement = await this.page.waitForSelector('#notebook-frame');
    return iframeElement.contentFrame();
  }
}
