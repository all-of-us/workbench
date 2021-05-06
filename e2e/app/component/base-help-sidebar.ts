import Container from 'app/container';
import Button from 'app/element/button';
import { SideBarLink } from 'app/text-labels';
import { Page } from 'puppeteer';
import { getPropValue } from 'utils/element-utils';
import { logger } from 'libs/logger';

const enum Selectors {
  rootXpath = '//*[@id="help-sidebar"]',
  // not(contains(normalize-space(@style), "width: 0px;")) is used to determine visibility
  contentXpath = '//*[not(contains(normalize-space(@style), "width: 0px;"))]/*[@data-test-id="sidebar-content"]',
  closeIconXpath = '//*[@role="button"][./*[@alt="Close"]]'
}

export default abstract class BaseHelpSidebar extends Container {
  deleteIconXpath: string;

  protected constructor(page: Page, xpath = `${Selectors.rootXpath}${Selectors.contentXpath}`) {
    super(page, xpath);
    this.deleteIconXpath = `${Selectors.rootXpath}${Selectors.contentXpath}${Selectors.closeIconXpath}`;
  }

  abstract open(): Promise<void>;

  async getTitle(): Promise<string> {
    const xpath = `(${this.getXpath()}//h3)[1]`;
    const h3 = await this.page.waitForXPath(xpath, { visible: true });
    return getPropValue<string>(h3, 'innerText');
  }

  async clickIcon(sidebarLink: SideBarLink): Promise<void> {
    let xpath;
    switch (sidebarLink) {
      case SideBarLink.ComputeConfiguration:
        xpath = `${Selectors.rootXpath}//*[@data-test-id="help-sidebar-icon-runtime"]`;
        break;
      case SideBarLink.DataDictionary:
        xpath = `${Selectors.rootXpath}//*[@data-test-id="help-sidebar-icon-dataDictionary"]`;
        break;
      case SideBarLink.HelpTips:
        xpath = `${Selectors.rootXpath}//*[@data-test-id="help-sidebar-icon-help"]`;
        break;
      case SideBarLink.WorkspaceMenu:
        xpath = `${Selectors.rootXpath}//*[@data-test-id="workspace-menu-button"]`;
        break;
      case SideBarLink.EditAnnotations:
        xpath = `${Selectors.rootXpath}//*[@data-test-id="help-sidebar-icon-annotations"]`;
        break;
    }
    await this.page.waitForXPath(xpath, { visible: true }).then((link) => link.click());
  }

  /**
   * Click X icon to close the Sidebar.
   */
  async close(): Promise<void> {
    const sidePanelTitle = await this.getTitle();
    const closeButton = new Button(this.page, this.deleteIconXpath);
    await closeButton.waitUntilEnabled();
    await closeButton.focus();
    await closeButton.click();
    await this.waitUntilClose();
    logger.info(`Closed "${sidePanelTitle}" sidebar panel`);
  }

  async isVisible(timeout = 1000): Promise<boolean> {
    if (!(await super.isVisible())) return false;
    try {
      await Promise.all([
        this.page.waitForXPath(this.getXpath(), { visible: true, timeout }),
        this.page.waitForXPath(this.deleteIconXpath, { visible: true, timeout })
      ]);
      return true;
    } catch (err) {
      return false;
    }
  }

  async waitUntilClose(): Promise<void> {
    await Promise.all([
      super.waitUntilClose(),
      this.page.waitForXPath(this.deleteIconXpath, { hidden: true }),
      this.page.waitForFunction(
        (selector) => {
          const node = document.evaluate(selector, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
            .singleNodeValue;
          return node === null;
        },
        { polling: 'mutation' },
        this.deleteIconXpath
      )
    ]).catch((err) => {
      logger.error('waitUntilClose() failed');
      logger.error(err);
      throw new Error(err);
    });
  }
}
