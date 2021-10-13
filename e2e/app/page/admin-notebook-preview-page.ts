

import { ElementHandle, Frame, Page, WaitForSelectorOptions } from 'puppeteer';
import Link from 'app/element/link';
import AuthenticatedPage from './authenticated-page';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import { getPropValue } from 'utils/element-utils';


const PageTitle = '.ipynb | All of Us Researcher Workbench';

export default class AdminNotebookPreviewPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitWhileLoading(this.page);
    await Promise.all([waitForDocumentTitle(this.page, PageTitle)]);
    return true;
  }

   getNamespaceLink(): Link {
    const selector = "//div/a[contains(@href, 'aou-rw-test-8c5cdbaf')]";
    return new Link(this.page, selector);
  }

  //click the namescape link to navigate back to workspace-namespace admin page
  async clickNamespaceLink(): Promise<void> {
    const button = this.getNamespaceLink();
    const navPromise = this.page.waitForNavigation({ waitUntil: ['load', 'networkidle0'] });
    await button.click();
    await navPromise;
    await waitWhileLoading(this.page);
  }

  async getFormattedCode(): Promise<string[]> {
    const css = '#notebook-container pre';
    await this.waitForCssSelector(css);
    const textContents = await this.findNotebookIframe().then((frame) => {
      return frame.$$(css);
    });
    return Promise.all(textContents.map(async (content) => await getPropValue<string>(content, 'textContent')));
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
