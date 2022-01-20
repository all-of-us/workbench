import { Frame, Page } from 'puppeteer';
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
    await Promise.all([waitForDocumentTitle(this.page, PageTitle)]);
    await waitWhileLoading(this.page);
    return true;
  }

  // get the link in the header of the notebook preview page
  getNamespaceLink(): Link {
    const xpath = '//*[starts-with(text(), "Viewing")]/a';
    return new Link(this.page, xpath);
  }

  // click the namespace link to navigate back to the workspace admin page
  async clickNamespaceLink(): Promise<void> {
    const namespaceLink = this.getNamespaceLink();
    const navPromise = this.page.waitForNavigation({ waitUntil: ['load', 'networkidle0'] });
    await namespaceLink.click();
    await navPromise;
    await waitWhileLoading(this.page);
  }

  // extract only the workspace namescape
  async getNamespaceText(): Promise<string> {
    const link = this.getNamespaceLink();
    const allTextContent = await link.getProperty<string>('textContent');
    const workspaceNamespace = allTextContent.split(' ').slice(-1).join(' ');
    return workspaceNamespace;
  }

  async getFormattedCode(): Promise<string[]> {
    const css = '.jp-CodeMirrorEditor';
    const iframe = await this.findNotebookIframe();
    await iframe.waitForSelector(css, { visible: true });
    const elements = await iframe.$$(css);
    return Promise.all(elements.map(async (content) => await getPropValue<string>(content, 'textContent')));
  }

  private async findNotebookIframe(): Promise<Frame> {
    const iframeElement = await this.page.waitForSelector('#notebook-frame');
    return iframeElement.contentFrame();
  }
}
