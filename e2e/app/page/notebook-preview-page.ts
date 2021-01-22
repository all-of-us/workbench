import {ElementHandle, Frame, Page, WaitForSelectorOptions} from 'puppeteer';
import Link from 'app/element/link';
import {getPropValue} from 'utils/element-utils';
import AuthenticatedPage from './authenticated-page';
import NotebookPage from './notebook-page';
import WorkspaceAnalysisPage from './workspace-analysis-page';
import {waitWhileLoading} from 'utils/waits-utils';

const Selector = {
  editButton: '//div[normalize-space(text())="Edit"]',
  playgroundButton: '//div[normalize-space(text())="Run (Playground Mode)"]',
}

export default class NotebookPreviewPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitWhileLoading(this.page);
    await Promise.all([
      this.page.waitForXPath(Selector.editButton, {visible: true}),
      this.page.waitForXPath(Selector.playgroundButton, {visible: true})
    ]);
    return true;
  }

  /**
   * Click "Edit" link to open notebook in Edit mode.
   */
  async openEditMode(notebookName: string): Promise<NotebookPage> {
    const link = new Link(this.page, Selector.editButton);
    await link.click();
    // Restarting notebook server may take a while.
    await waitWhileLoading(this.page, 60 * 15 * 1000);

    const notebookPage = new NotebookPage(this.page, notebookName);
    await notebookPage.waitForLoad();
    return notebookPage;
  }

  async getFormattedCode(): Promise<string> {
    const codeContent = await this.waitForCssSelector('#notebook-container pre');
    return getPropValue<string>(codeContent, 'textContent');
  }

  /**
   * Click "Notebook" link, goto Workspace Analysis page.
   * This function does not handle Unsaved Changes confirmation.
   */
  async goAnalysisPage(): Promise<WorkspaceAnalysisPage> {
    const notebooksLink = await Link.findByName(this.page, {name: 'Notebooks'});
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
