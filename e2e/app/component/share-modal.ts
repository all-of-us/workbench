import {ElementHandle, Page} from 'puppeteer';
import {LinkText, WorkspaceAccessLevel} from 'app/text-labels';
import Dialog from 'app/component/dialog';
import Textbox from 'app/element/textbox';
import ClrIcon from 'app/element/clr-icon-link';


export default class ShareModal extends Dialog {

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async shareWithUser(username: string, level: WorkspaceAccessLevel): Promise<void> {
    /*
    if (!this.page.viewport()) {
      throw Error(
        'testing of the share modal requires a viewport to be set on account ' +
          'on odd rendering issues with the react-select component');
    }
    */

    const searchBox = await this.waitForSearchBox();
    await searchBox.type(username);

    const addCollab = await this.waitForAddCollaboratorIcon();
    await addCollab.click();

    const roleInput = await this.waitForRoleSelectorForUser(username);
    await roleInput.click();

    const ownerOpt = await this.waitForRoleOption(level);
    await ownerOpt.click();

    await this.clickButton(LinkText.Save);
    await this.waitUntilDialogIsClosed();
  }

  async removeUser(username: string): Promise<void> {
    const rmCollab = await this.page.waitForXPath(
      `${this.collabRowXPath(username)}//clr-icon[@shape="minus-circle"]`, {visible: true});
    await rmCollab.click();

    await this.clickButton(LinkText.Save);
    return this.waitUntilDialogIsClosed();
  }

  /**
   * Creates an xpath to a share "row" for a given user within the modal. This
   * can be combined with a child selector to pull out a control for a user.
   */
  private collabRowXPath(username: string): string {
    // We dip into child contents to find the collab user row element parent.
    // .//div filters by a relative path to the parent row.
    return `${this.getXpath()}//*[@data-test-id="collab-user-row" and .//div[` +
        `@data-test-id="collab-user-email" and contains(text(),"${username}")]]`;
  }

  async waitForSearchBox(): Promise<Textbox> {
    return Textbox.findByName(this.page, {name: 'Find Collaborators'}, this);
  }

  async waitForAddCollaboratorIcon(): Promise<ClrIcon> {
    return ClrIcon.findByName(this.page, {iconShape: 'plus-circle'}, this);
  }

  async waitForRoleSelectorForUser(username: string): Promise<Textbox> {
    const box = new Textbox(
      this.page, `${this.collabRowXPath(username)}//input[@type="text"]`);
    await box.waitForXPath({visible: true});
    return box;
  }

  async waitForRoleOption(level: WorkspaceAccessLevel): Promise<ElementHandle> {
    // The label in the select menu uses title case.
    const levelText =  level[0].toUpperCase() + level.substring(1).toLowerCase();
    return this.page.waitForXPath(
      `//*[starts-with(@id,"react-select") and text()="${levelText}"]`,
      {visible: true});
  }
}
