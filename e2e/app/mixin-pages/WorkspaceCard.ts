import {ElementHandle, Page} from 'puppeteer';
import AuthenticatedPage from './AuthenticatedPage';

export default class WorkspaceResourceCard extends AuthenticatedPage {

  private xpath = '//*[*[@data-test-id="workspace-card"]]';

  constructor(aPage: Page) {
    super(aPage);
  }

  public async getAllCards(): Promise<ElementHandle[]> {
    const cards = await this.puppeteerPage.$x(this.xpath);
    return cards;
  }

}
