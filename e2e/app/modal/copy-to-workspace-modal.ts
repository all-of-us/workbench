import Textbox from 'app/element/textbox';
import { LinkText } from 'app/text-labels';
import { ElementHandle, Page } from 'puppeteer';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import Modal from './modal';

const modalTitle = 'Copy to Workspace';

export default class CopyToWorkspaceModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalTitle, { xpath: this.getXpath() });
    return true;
  }

  async getDestinationTextbox(): Promise<Textbox> {
    return Textbox.findByName(this.page, { containsText: 'Destination' }, this);
  }

  async getNameTextbox(): Promise<Textbox> {
    return new Textbox(
      this.page,
      `${this.getXpath()}//*[contains(text(), "Name")]/ancestor::node()[1]/input[@type="text"]`
    );
  }

  /**
   *
   * @param {string} workspaceName Destination Workspace name.
   * @param {string} newName New name.
   */
  async beginCopyToAnotherWorkspace(workspaceName: string, newName?: string): Promise<void> {
    // Click dropdown trigger.
    const destinationInput = await this.getDestinationTextbox();
    await destinationInput.click();

    // Select Workspace in dropdown
    const selectOption = await this.waitForSelectOption(workspaceName);
    await selectOption.click();

    // Type new name.
    if (newName !== undefined) {
      const nameInput = await this.getNameTextbox();
      await nameInput.type(newName);
    }
  }

  /**
   *
   * @param {string} workspaceName Destination Workspace name.
   * @param {string} newName New name.
   */
  async copyToAnotherWorkspace(workspaceName: string, newName?: string): Promise<void> {
    await this.beginCopyToAnotherWorkspace(workspaceName, newName);

    await this.clickButton(LinkText.Copy);
    await waitWhileLoading(this.page);
  }

  async waitForSelectOption(workspaceName?: string): Promise<ElementHandle> {
    const xpathSubstr = 'starts-with(@id, "react-select-") and contains(@id, "-option-")';
    if (workspaceName === undefined) {
      // Without a workspace name, select the second (or any) option in dropdown.
      return this.page.waitForXPath(`${this.getXpath()}//*[${xpathSubstr} and text()]`, { visible: true });
    }
    return this.page.waitForXPath(`${this.getXpath()}//*[${xpathSubstr} and text()="${workspaceName}"]`, {
      visible: true
    });
  }
}
