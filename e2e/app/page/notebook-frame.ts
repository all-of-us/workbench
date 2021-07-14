import AuthenticatedPage from './authenticated-page';
import { Frame, Page } from 'puppeteer';

export default abstract class NotebookFrame extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async getIFrame(): Promise<Frame> {
    const frame = await this.page.waitForSelector('iframe[src*="notebooks"]');
    return frame.contentFrame();
  }
}
