import AuthenticatedPage from 'app/page/authenticated-page';
import { Page } from 'puppeteer';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import StaticText from 'app/element/staticText';
import SelectMenu from 'app/component/select-menu';
import { ElementType } from 'app/xpath-options';

export abstract class BaseAdminPage extends AuthenticatedPage {
  protected constructor(page: Page) {
    super(page);
  }

  getSaveButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Save });
  }

  getCancelButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Cancel });
  }

  getSelectMenu(dataTestId: string): SelectMenu {
    return SelectMenu.findByName(this.page, {
      type: ElementType.Dropdown,
      dataTestId,
      ancestorLevel: 0
    });
  }

  async getStaticText(name: string): Promise<string> {
    const element = StaticText.findByName(this.page, { name });
    const text = await element.getText();
    return text.split('\n')[1];
  }
}
