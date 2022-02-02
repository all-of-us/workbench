import AuthenticatedPage from './authenticated-page';
import { Frame, Page } from 'puppeteer';

export default abstract class NotebookFrame extends AuthenticatedPage {
  protected constructor(page: Page) {
    super(page);
  }

  async getIFrame(timeout?: number): Promise<Frame> {
    const frame = await this.page.waitForSelector('iframe[src*="notebooks"]', { visible: true, timeout });
    return frame.contentFrame();
  }
}
