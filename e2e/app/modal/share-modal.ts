import { ElementHandle, Page } from 'puppeteer';
import { LinkText, WorkspaceAccessLevel } from 'app/text-labels';
import Textbox from 'app/element/textbox';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import Modal from './modal';
import Button from 'app/element/button';
import { ElementType } from 'app/xpath-options';
import ClrIconLink from 'app/element/clr-icon-link';
import { logger } from 'libs/logger';

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
    const timeout = 5000;
    const findCollaboratorAddIcon = (name: string) => {
      return ClrIconLink.findByName(
        this.page,
        { type: ElementType.Icon, iconShape: 'plus-circle', containsText: name, ancestorLevel: 2 },
        this
      );
    };
    const addIcon = findCollaboratorAddIcon(userName);

    const dropDownXpath = this.getXpath() + '//*[@data-test-id="drop-down"][.//clr-icon[@shape="plus-circle"]]';
    const existsDropDown = async (timeout: number): Promise<boolean> => {
      return this.page
        .waitForXPath(dropDownXpath, { visible: true, timeout })
        .then(() => {
          return true;
        })
        .catch(() => {
          return false;
        });
    };

    const waitForDropDownClose = async (): Promise<void> => {
      await waitWhileLoading(this.page);
      await this.page.waitForXPath(dropDownXpath, { hidden: true });
    };

    const pickUserRole = async (name: string): Promise<void> => {
      const roleInput = await this.waitForRoleSelectorForUser(name);
      await roleInput.click();
      const ownerOpt = await this.waitForRoleOption(level);
      await ownerOpt.click();
    };

    const typeAndAddUser = async (name: string): Promise<boolean> => {
      const nameWithoutDomain = name.split('@')[0];
      for (const char of nameWithoutDomain) {
        const input = await this.waitForSearchBox().asElementHandle();
        const waitForResponsePromise = this.page.waitForResponse((response) => {
          return response.url().includes('/userSearch/registered/') && response.request().method() === 'GET';
        });
        await input.type(char);
        // Wait for GET /userSearch request to finish. Sometimes it takes several seconds.
        await waitForResponsePromise;
        const foundDropdown = await existsDropDown(timeout);
        const foundAddIcon = await addIcon.exists(timeout);
        if (foundDropdown && foundAddIcon) {
          await addIcon.click();
          // Test playback runs fast. Wait until dropdown disappears so it is not interfering with next click.
          await waitForDropDownClose();
          return true;
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

  async existsUser(email: string): Promise<boolean> {
    const notFoundXpath =
      this.getXpath() + '//*[@data-test-id="drop-down"][not(.//*[text()="No results based on your search"])]';
    await this.waitForSearchBox().type(email, { delay: 20 });
    await waitWhileLoading(this.page);
    return await this.page
      .waitForXPath(notFoundXpath, { visible: true, timeout: 5000 })
      .then(() => {
        return true;
      })
      .catch(() => {
        return false;
      });
  }
}
