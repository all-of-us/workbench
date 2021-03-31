import Container from 'app/container';
import Button from 'app/element/button';
import { LinkText, SideBarLink } from 'app/text-labels';
import * as fp from 'lodash/fp';
import { Page } from 'puppeteer';
import { getPropValue } from 'utils/element-utils';
import { waitWhileLoading } from 'utils/waits-utils';

const enum Selectors {
  rootXpath = '//*[@id="help-sidebar"]',
  // "margin-right: 0px;" is used to deterimine visibility
  contentXpath = '//*[@data-test-id="sidebar-content" and contains(normalize-space(@style), "margin-right: 0px;")]',
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
   * Click a button inside the sidebar.
   * @param {string} buttonLabel The button text label.
   * @param waitOptions Wait for navigation or/and modal close after click button.
   */
  async clickButton(buttonLabel: LinkText, waitOptions: { waitForClose?: boolean } = {}): Promise<void> {
    const { waitForClose = false } = waitOptions;
    const button = await Button.findByName(this.page, { normalizeSpace: buttonLabel }, this);
    await button.waitUntilEnabled();
    await Promise.all(
      fp.flow(
        fp.filter<{ shouldWait: boolean; waitFn: () => Promise<void> }>('shouldWait'),
        fp.map((item) => item.waitFn()),
        fp.concat([button.click()])
      )([{ shouldWait: waitForClose, waitFn: () => this.waitUntilClose() }])
    );
  }

  /**
   * Click X icon to close the Sidebar.
   */
  async close(): Promise<void> {
    const sidePanelTitle = await this.getTitle();
    const closeButton = new Button(this.page, this.deleteIconXpath);
    await closeButton.waitUntilEnabled();
    await closeButton.click();
    await waitWhileLoading(this.page);
    await this.waitUntilClose();
    console.log(`Closed "${sidePanelTitle}" sidebar panel`);
  }

  async isVisible(): Promise<boolean> {
    if (!(await super.isVisible())) return false;
    try {
      await Promise.all([
        this.page.waitForXPath(this.getXpath(), { visible: true, timeout: 1000 }),
        this.page.waitForXPath(this.deleteIconXpath, { visible: true, timeout: 1000 })
      ]);
      return true;
    } catch (err) {
      return false;
    }
  }

  async waitUntilClose(): Promise<void> {
    await super.waitUntilClose();
    await this.page.waitForXPath(this.deleteIconXpath, { hidden: true });
    await this.page.waitForTimeout(1000);
  }
}
