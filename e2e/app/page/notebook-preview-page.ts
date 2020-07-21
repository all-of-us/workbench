import {ElementHandle, Frame, Page, WaitForSelectorOptions} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForNumberElements} from 'utils/waits-utils';
import AuthenticatedPage from './authenticated-page';
import NotebookPage from './notebook-page';

export default class NotebookPreviewPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        this.waitForCssSelector('#notebook'),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (e) {
      console.error(`NotebookPreviewPage isLoaded() encountered ${e}`);
      throw new Error(e);
    }
  }

  /**
   * Click Edit link.
   */
  async edit(notebookName: string): Promise<NotebookPage> {
    const selector = '//div[normalize-space(text())="Edit"]';
    const link = await this.page.waitForXPath(selector, {visible: true});
    await link.click();
    // wait for Connecting to Notebook Server icon.
    await waitForNumberElements(this.page, 'clr-icon[shape="sync"]', 1);
    // wait until Connecting to Notebook Server icon disappear.
    await waitForNumberElements(this.page, 'clr-icon[shape="sync"]', 0, 120000);
    const notebookPage = new NotebookPage(this.page, notebookName);
    await notebookPage.waitForLoad();
    return notebookPage;
  }

  async getFormattedCode(): Promise<string> {
    const codeContent = await this.waitForCssSelector('#notebook-container pre');
    const textContentProperty = await codeContent.getProperty('textContent');
    return (await textContentProperty.jsonValue()).toString();
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
