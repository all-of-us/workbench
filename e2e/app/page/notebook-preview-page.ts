import {ElementHandle, Frame, Page, WaitForSelectorOptions} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import AuthenticatedPage from './authenticated-page';

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
