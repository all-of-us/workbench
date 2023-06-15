import { Page } from 'puppeteer';
import { SideBarLink } from 'app/text-labels';
import BaseEnvironmentPanel from './base-environment-panel';
import Button from '../element/button';

const defaultXpath = '//*[@id="confirm-delete-environment-with-pd-panel"]';

export default class ConfirmDeleteEnvironmentWithPdPanel extends BaseEnvironmentPanel {
  // this panel is used in the context of multiple sidebar "icons", so we manually pass the relevant one here
  constructor(page: Page, sideBarLink: SideBarLink) {
    super(page, defaultXpath, sideBarLink);
  }

  async confirmDeleteGkeAppWithDisk(): Promise<void> {
    await this.isVisible();

    const includePdButtonPath = `${this.getXpath()}//*[@data-test-id="delete-environment-and-pd"]`;
    const includePdButton = new Button(page, includePdButtonPath);
    expect(await includePdButton.exists()).toBeTruthy();
    await includePdButton.click();

    const confirmButtonPath = `${this.getXpath()}//*[@aria-label="Delete"]`;
    const confirmButton = new Button(page, confirmButtonPath);
    expect(await confirmButton.exists()).toBeTruthy();
    await confirmButton.click();

    return;
  }
}
