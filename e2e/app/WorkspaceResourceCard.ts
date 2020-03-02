import {ElementHandle, Page} from 'puppeteer';
import AuthenticatedPage from './page-mixin/AuthenticatedPage';
const _ = require('lodash');

export default class WorkspaceResourceCard extends AuthenticatedPage {

  // @ts-ignore
  private element: ElementHandle;

  /**
   *
   * Work in progress
   */


  constructor(aPage: Page, elementHandle?: ElementHandle) {
    super(aPage);
    this.element = elementHandle;
  }

  public async getAllCardsElements(): Promise<WorkspaceResourceCard[]> {
    const xpath = '//*[*[@data-test-id="workspace-card"]]';
    const cards = await this.puppeteerPage.$x(xpath);
    const resourceCards = cards.map(card => new WorkspaceResourceCard(this.puppeteerPage, card));
    return resourceCards;
  }

  public async getAnyResourceCard(): Promise<WorkspaceResourceCard> {
    const cards = await this.getAllCardsElements();
    const anyCard = _.shuffle(cards)[0];
    return anyCard;
  }

  public async getCardName(): Promise<unknown> {
    const cardNameElem = await this.element.$('[data-test-id="workspace-card-name"]');
    const cardName = await (await cardNameElem.getProperty('innerText')).jsonValue();
    return cardName;
  }

  public asElementHandle(): ElementHandle {
    return this.element.asElement();
  }

}
