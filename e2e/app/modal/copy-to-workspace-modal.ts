import Textbox from 'app/element/textbox';
import { LinkText } from 'app/text-labels';
import { Page } from 'puppeteer';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import Modal from './modal';
import ReactSelect from 'app/element/react-select';

const modalTitle = 'Copy to Workspace';

export default class CopyToWorkspaceModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalTitle, { container: this });
    return true;
  }

  getDestinationTextbox(): Textbox {
    return Textbox.findByName(this.page, { containsText: 'Destination' }, this);
  }

  getNotebookNameTextbox(): Textbox {
    return new Textbox(
      this.page,
      `${this.getXpath()}//*[contains(text(), "Name")]/ancestor::node()[1]/input[@type="text"]`
    );
  }

  /**
   *
   * @param {string} workspaceName Destination Workspace name.
   * @param {string} newNotebookName New name.
   */
  async beginCopyToAnotherWorkspace(workspaceName: string, newNotebookName?: string): Promise<void> {
    await this.selectDestinationWorkspace(workspaceName);
    // Type notebook name.
    if (newNotebookName !== undefined) {
      const nameInput = this.getNotebookNameTextbox();
      await nameInput.type(newNotebookName);
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

  async selectDestinationWorkspace(workspaceName: string): Promise<void> {
    const selectMenu = new ReactSelect(this.page, { name: 'Destination *' });
    await selectMenu.selectOption(workspaceName);
  }
}
