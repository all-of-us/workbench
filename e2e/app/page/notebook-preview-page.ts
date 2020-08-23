import {ElementHandle, Frame, Page, WaitForSelectorOptions} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import Link from 'app/element/link';
import {getPropValue} from 'utils/element-utils';
import AuthenticatedPage from './authenticated-page';
import NotebookPage from './notebook-page';

const Selector = {
  editButton: '//div[normalize-space(text())="Edit"]',
  playgroundButton: '//div[normalize-space(text())="Run (Playground Mode)"]',
}

export default class NotebookPreviewPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        this.page.waitForXPath(Selector.editButton),
        this.page.waitForXPath(Selector.playgroundButton),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (e) {
      console.error(`NotebookPreviewPage isLoaded() encountered ${e}`);
      throw new Error(e);
    }
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
    await notebookPage.waitForKernelIdle();
    return notebookPage;
  }

  async getFormattedCode(): Promise<string> {
    const codeContent = await this.waitForCssSelector('#notebook-container pre');
    return getPropValue<string>(codeContent, 'textContent');
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
