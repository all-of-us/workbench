import { ElementHandle, Page } from 'puppeteer';
import { LinkText, WorkspaceAccessLevel } from 'app/text-labels';
import Textbox from 'app/element/textbox';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import Modal from './modal';
import Button from 'app/element/button';
import { ElementType } from 'app/xpath-options';
import ClrIconLink from 'app/element/clr-icon-link';
import { logger } from 'libs/logger';
import { elementExists } from 'utils/element-utils';

const modalText = 'share this workspace';

export default class ShareModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalText, { container: this });
    await waitWhileLoading(this.page);
    return true;
  }

  async shareWithUser(userName: string, level: WorkspaceAccessLevel): Promise<void> {
    const timeout = 5000;
    const findCollaboratorAddIcon = (name: string) => {
      return ClrIconLink.findByName(
        this.page,
        { type: ElementType.Icon, iconShape: 'plus-circle', containsText: name, ancestorLevel: 2 },
        this
      );
    };
    const addIcon = findCollaboratorAddIcon(userName);

    const waitForDropDownClose = async (): Promise<void> => {
      await waitWhileLoading(this.page);
      await this.page.waitForXPath(this.getEmailsDropdownXpath(), { hidden: true, visible: false });
    };

    const pickUserRole = async (name: string): Promise<void> => {
      const roleInput = await this.waitForRoleSelectorForUser(name);
      await roleInput.click();
      const ownerOpt = await this.waitForRoleOption(level);
      await ownerOpt.click();
    };

    const typeAndAddUser = async (name: string): Promise<boolean> => {
      const nameWithoutDomain = name.split('@')[0];
      // Split string into segments of 5 characters. Type 5 chars at a time, much faster than type 1 char and check.
      const n = 5;
      for (let i = 0, charsLength = nameWithoutDomain.length; i < charsLength; i += n) {
        const chars = nameWithoutDomain.substring(i, i + n);
        const input = await this.waitForSearchBox().asElementHandle();
        const waitForResponsePromise = this.page.waitForResponse(
          (response) => {
            return response.url().includes('v1/userSearch/') && response.request().method() === 'GET';
          },
          { timeout: 60000 }
        );
        await input.type(chars, { delay: 0 });
        // Wait for GET /userSearch request to finish. Sometimes it takes several seconds.
        await waitForResponsePromise;
        if (await this.emailsDropdownExists(timeout)) {
          if (await addIcon.exists(1000)) {
            await addIcon.click();
            // Test playback runs fast. Wait until dropdown disappears so it is not interfering with next click.
            await waitForDropDownClose();
            return true;
          }
        }
      }
      return false;
    };

    const isSuccess = await typeAndAddUser(userName);
    if (!isSuccess) {
      const errMsg = `Failed sharing workspace with user ${userName}.`;
      logger.error(errMsg);
      throw new Error(errMsg);
    }
    await pickUserRole(userName);
    await this.clickButton(LinkText.Save, { waitForClose: true });
  }

  getSaveButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Save });
  }

  async removeUser(username: string): Promise<void> {
    const rmCollab = await this.page.waitForXPath(
      `${this.collaboratorRowXPath(username)}//clr-icon[@shape="minus-circle"]`,
      {
        visible: true
      }
    );
    await rmCollab.hover();
    await rmCollab.click();
    await this.clickButton(LinkText.Save, { waitForClose: true });
  }

  /**
   * Creates an xpath to a share "row" for a given user within the modal. This
   * can be combined with a child selector to pull out a control for a user.
   */
  private collaboratorRowXPath(username: string): string {
    // We dip into child contents to find the collaborator row element parent.
    // .//div filters by a relative path to the parent row.
    return (
      `${this.getXpath()}//*[@data-test-id="collab-user-row" and .//div[` +
      `@data-test-id="collab-user-email" and contains(text(),"${username}")]]`
    );
  }

  waitForSearchBox(): Textbox {
    return Textbox.findByName(this.page, { name: 'Find Collaborators' }, this);
  }

  async waitForRoleSelectorForUser(username: string): Promise<Textbox> {
    const box = new Textbox(this.page, `${this.collaboratorRowXPath(username)}//input[@type="text"]`);
    await box.waitForXPath({ visible: true });
    return box;
  }

  async waitForRoleOption(level: WorkspaceAccessLevel): Promise<ElementHandle> {
    // The label in the select menu uses title case.
    const levelText = level[0].toUpperCase() + level.substring(1).toLowerCase();
    return this.page.waitForXPath(`//*[starts-with(@id,"react-select") and text()="${levelText}"]`, { visible: true });
  }

  async userExists(email: string): Promise<boolean> {
    const timeout = 10000;
    const noSearchResultsXpath =
      this.getXpath() + '//*[@data-test-id="drop-down"]//*[text()="No results based on your search"]';
    const emailWithoutDomain = email.split('@')[0];
    await this.waitForSearchBox().type(emailWithoutDomain);
    await waitWhileLoading(this.page);
    await Promise.race([
      this.page.waitForXPath(noSearchResultsXpath, { visible: true, timeout }),
      this.emailsDropdownExists(timeout)
    ]);
    return elementExists(this.page, noSearchResultsXpath);
  }

  private async emailsDropdownExists(timeout = 2000): Promise<boolean> {
    return elementExists(this.page, this.getEmailsDropdownXpath(), { timeout });
  }

  private getEmailsDropdownXpath(): string {
    return `${this.getXpath()}//*[@data-test-id="drop-down"][.//clr-icon[@shape="plus-circle"]]`;
  }
}
