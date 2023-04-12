import AuthenticatedPage from './authenticated-page';
import { Frame, Page } from 'puppeteer';

export default abstract class NotebookFrame extends AuthenticatedPage {
  protected constructor(page: Page) {
    super(page);
  }

  async getIFrame(timeout?: number): Promise<Frame> {
    const handle = await this.page.waitForSelector('iframe[src*="notebooks"]', { visible: true, timeout });
    const frame = await handle.contentFrame();
    if (frame === null) {
      throw new Error('contentFrame() returned null - this seems to be a frequent issue when running in debug mode');
    }
    return frame;
  }
}
