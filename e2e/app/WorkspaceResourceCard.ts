import {ElementHandle, Page} from 'puppeteer';
import ClrIconLink from './aou-elements/ClrIconLink';
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
    console.log('all resource cards');
    console.log(resourceCards);
    return resourceCards;
  }

  public async getAnyResourceCard(): Promise<WorkspaceResourceCard> {
    const cards = await this.getAllCardsElements();
    const anyCard = _.shuffle(cards)[0];
    console.log('any resource card');
    console.log(anyCard);
    return anyCard;
  }

  public async getCardName(): Promise<unknown> {
    const cardNameElem = await this.element.$('[data-test-id="workspace-card-name"]');
    const cardName = await (await cardNameElem.getProperty('innerText')).jsonValue();
    console.log('cardName = ' + cardName);
    return cardName;
  }

  public async clickEllipsis() {
    const clrIcon = new ClrIconLink(this.puppeteerPage);
    console.log(clrIcon);

  }

  public asElementHandle(): ElementHandle {
    return this.element.asElement();
  }

}
