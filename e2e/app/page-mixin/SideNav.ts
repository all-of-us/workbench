import {Page} from 'puppeteer';
import {iconXpath} from '../aou-elements/xpath-defaults';
import {findIcon} from '../aou-elements/xpath-finder';

export interface PuppeteerPage {
  page: Page;
}

type Constructor<T> = new(...args: any[]) => T;

// @ts-ignore
export function SideNav<T extends Constructor<PuppeteerPage>>(Base: T) {

  return class extends Base {

    public angleIconXpath = iconXpath('', 'angle');

    constructor(...args: any[]) {
      super(...args);
    }

      /**
       * Go to application page.
       * @param page
       * @param app
       */
    public async goTo(app: string) {
      await this.openDropdown();
      const appLinkXpath = `//*[@role="button" and @tabindex="0"]//span[contains(., "${app}")]`;
      const [applink] = await this.page.$x(appLinkXpath);
      if (!applink) {
        const [username, admin] = await this.page.$x(this.angleIconXpath);
        if (app === 'Profile' || app === 'Sign Out') {
           // Open User submenu if needed
          if (!applink) {
            await username.click();
          }
        } else if (app === 'User Admin') {
           // Open Admin submenu if needed
          if (!applink) {
            await admin.click();
          }
        }
      }
      const link = await this.page.waitForXPath(appLinkXpath, {timeout: 2000});
      await link.click();
      await this.page.waitFor(1000);
    }

    public async isOpen(): Promise<boolean> {
      try {
        await findIcon(this.page, 'Home', 'home', {visible: true, timeout: 1000});
        return true;
      } catch(err) {
        return false;
      }
    }

      /**
       * Open dropdown.
       */
    public async openDropdown() {
      const is = await this.isOpen();
      if (!is) {
            // click bars icon to open dropdown
        const icon = await findIcon(this.page, '', 'bars');
        await icon.click();
        await this.page.waitForXPath(this.angleIconXpath, {timeout: 2000});
      }
    }

    public async getUserName(): Promise<unknown> {
      const xpath = `//*[child::clr-icon[@shape="angle"]/*[@role="img"]]`;
      const username = (await this.page.$x(xpath))[0];
      const p = await username.getProperty('innerText');
      const value = await p.jsonValue();
      console.log('p value = ' + value);
      return value;
    }

  }

}
