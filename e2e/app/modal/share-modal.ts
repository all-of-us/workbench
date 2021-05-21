import { ElementHandle, Page } from 'puppeteer';
import { LinkText, WorkspaceAccessLevel } from 'app/text-labels';
import Textbox from 'app/element/textbox';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import Modal from './modal';
import { logger } from 'libs/logger';
import Button from 'app/element/button';
import { ElementType } from 'app/xpath-options';
import ClrIconLink from 'app/element/clr-icon-link';

const modalText = 'share this workspace';

export default class ShareModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalText, { xpath: this.getXpath() });
    return true;
  }

  async shareWithUser(userName: string, level: WorkspaceAccessLevel): Promise<void> {
    const dropDownXpath = this.getXpath() + '//*[@data-test-id="drop-down"][.//clr-icon[@shape="plus-circle"]]';
    const waitForDropDown = async () => {
      await waitWhileLoading(this.page);
      await this.page.waitForXPath(dropDownXpath, { visible: true });
    };

    const waitForClose = async () => {
      await waitWhileLoading(this.page);
      await this.page.waitForXPath(dropDownXpath, { hidden: true });
    };

    const findCollaboratorAddIcon = () => {
      return ClrIconLink.findByName(
        this.page,
        { type: ElementType.Icon, iconShape: 'plus-circle', containsText: userName, ancestorLevel: 2 },
        this
      );
    };

    const pickUserRole = async () => {
      const roleInput = await this.waitForRoleSelectorForUser(userName);
      await roleInput.click();

      const ownerOpt = await this.waitForRoleOption(level);
      await ownerOpt.click();
    };

    let maxAttempts = 2;
    // Try 2 times: enter email and find add icon.
    const typeAndAddUser = async () => {
      await this.waitForSearchBox().type(userName, { delay: 50 });
      await waitForDropDown();
      try {
        const addIcon = findCollaboratorAddIcon();
        await addIcon.click();
        await waitForClose();
        return;
      } catch (err) {
        // Click failed.
      }
      if (maxAttempts <= 0) {
        return;
      }
      maxAttempts--;
      await this.page.waitForTimeout(1000).then(() => {
        return typeAndAddUser();
      });
    };

    await typeAndAddUser();
    await pickUserRole();
    await this.clickButton(LinkText.Save, { waitForClose: true });
    logger.info(`Shared workspace to ${userName} with role ${level}`);
  }

  getSaveButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Save });
  }

  async removeUser(username: string): Promise<void> {
    const rmCollab = await this.page.waitForXPath(`${this.collabRowXPath(username)}//clr-icon[@shape="minus-circle"]`, {
      visible: true
    });
    await rmCollab.hover();
    await rmCollab.click();
    await this.clickButton(LinkText.Save, { waitForClose: true });
  }

  /**
   * Creates an xpath to a share "row" for a given user within the modal. This
   * can be combined with a child selector to pull out a control for a user.
   */
  private collabRowXPath(username: string): string {
    // We dip into child contents to find the collab user row element parent.
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
    const box = new Textbox(this.page, `${this.collabRowXPath(username)}//input[@type="text"]`);
    await box.waitForXPath({ visible: true });
    return box;
  }

  async waitForRoleOption(level: WorkspaceAccessLevel): Promise<ElementHandle> {
    // The label in the select menu uses title case.
    const levelText = level[0].toUpperCase() + level.substring(1).toLowerCase();
    return this.page.waitForXPath(`//*[starts-with(@id,"react-select") and text()="${levelText}"]`, { visible: true });
  }

  async notFindUser(email: string): Promise<boolean> {
    const notFoundXpath =
      this.getXpath() + '//*[@data-test-id="drop-down"][not(.//*[text()="No results based on your search"])]';
    await this.waitForSearchBox().type(email, { delay: 20 });
    await waitWhileLoading(this.page);
    return await this.page
      .waitForXPath(notFoundXpath, { visible: true })
      .then(() => {
        return true;
      })
      .catch(() => {
        return false;
      });
  }
}
