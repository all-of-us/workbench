import Container from 'app/container';
import {ElementHandle, Page} from 'puppeteer';
import {WorkspaceAccessLevel} from 'app/page-identifiers';
import {ElementType} from 'app/xpath-options';
import Button from 'app/element/button';
import Textbox from 'app/element/textbox';
import ClrIcon from 'app/element/clr-icon-link';


export enum ButtonLabel {
  Cancel = 'Cancel',
  Save = 'Save'
}

const Selector = {
  defaultDialog: '//*[@role="dialog"]',
}

export default class ShareModal extends Container {

  constructor(page: Page, xpath: string = Selector.defaultDialog) {
    super(page, xpath);
  }

  async shareWithUser(username: string, level: WorkspaceAccessLevel): Promise<void> {
    const searchBox = await this.waitForSearchBox();
    await searchBox.type(username);

    const addCollab = await this.waitForAddCollaboratorIcon();
    await addCollab.click();

    // This is pretty limited - here we assume that the first collab with "Reader"
    // is the user we care about, which may not be the case. Ideally we'd have a
    // cleaner selection here.
    const roleInput = await this.waitForFirstReaderRoleDropdown();
    await roleInput.click();

    const ownerOpt = await this.waitForRoleOption(level);
    await ownerOpt.click();

    await this.clickButton(ButtonLabel.Save);
    await this.waitUntilDialogIsClosed();
  }

  async removeUser(username: string): Promise<void> {
    const rmCollab = await this.page.waitForXPath(
      `//*[contains(text(),"${username}")]/clr-icon[@shape="minus-circle"]`);
    await rmCollab.click();

    await this.clickButton(ButtonLabel.Save);
    await this.waitUntilDialogIsClosed();
    return;
  }

  async clickButton(buttonLabel: ButtonLabel): Promise<void> {
    const button = await this.waitForButton(buttonLabel);
    await button.waitUntilEnabled();
    return button.click();
  }

  async waitForButton(buttonLabel: ButtonLabel): Promise<Button> {
    return Button.findByName(this.page, {containsText: buttonLabel, type: ElementType.Button}, this);
  }

  async waitForSearchBox(): Promise<Textbox> {
    return Textbox.findByName(this.page, {name: 'Find Collaborators'}, this);
  }

  async waitForAddCollaboratorIcon(): Promise<ClrIcon> {
    return ClrIcon.findByName(this.page, {iconShape: 'plus-circle'}, this);
  }

  async waitForFirstReaderRoleDropdown(): Promise<Textbox> {
    return Textbox.findByName(this.page, {name: 'Reader'}, this);
  }

  async waitForRoleOption(level: WorkspaceAccessLevel): Promise<ElementHandle> {
    // The label in the select menu uses title case.
    const levelText =  level[0].toUpperCase() + level.substring(1).toLowerCase();
    return await this.page.waitForXPath(
      `//*[starts-with(@id,"react-select")][text()="${levelText}"]`);
  }

  async waitUntilDialogIsClosed(timeOut: number = 60000): Promise<void> {
    await this.page.waitForXPath(this.xpath, {hidden: true, timeout: timeOut});
  }

  async waitUntilVisible(): Promise<void> {
    await this.page.waitForXPath(this.xpath, {visible: true});
  }

  async exists(): Promise<boolean> {
    return (await this.page.$x(this.xpath)).length > 0;
  }

}
