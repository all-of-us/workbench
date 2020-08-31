import {ElementHandle, Page} from 'puppeteer';
import {EllipsisMenuAction} from 'app/text-labels';
import EllipsisMenu from './ellipsis-menu';


export default abstract class CardBase {

  ellipsisXpath = './/clr-icon[@shape="ellipsis-vertical"]';
  protected cardElement: ElementHandle;

  protected constructor(protected readonly page: Page) {
  }

  asElementHandle(): ElementHandle {
    return this.cardElement.asElement();
  }

  getEllipsis(): EllipsisMenu {
    return new EllipsisMenu(this.page, this.ellipsisXpath, this.asElementHandle());
  }

  async clickEllipsisAction(action: EllipsisMenuAction, opt: { waitForNav?: boolean } = {}): Promise<void> {
    return this.getEllipsis().clickAction(action, opt);
  }


}
