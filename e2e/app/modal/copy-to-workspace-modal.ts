import Textbox from 'app/element/textbox';
import { LinkText } from 'app/text-labels';
import { Page } from 'puppeteer';
import { waitWhileLoading } from 'utils/waits-utils';
import Modal from './modal';
import ReactSelect from 'app/element/react-select';

const modalTitle = 'Copy to Workspace';

export default class CopyToWorkspaceModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    const timeout = 30 * 1000;
    return Promise.all([
      this.page.waitForXPath(`${this.getXpath()}//*[text()="${modalTitle}"]`, { visible: true, timeout }),
      await this.getDestinationTextbox().exists(timeout),
      await this.getNotebookNameTextbox().exists(timeout)
    ])
      .then(() => {
        return true;
      })
      .catch(() => {
        return false;
      });
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
    await this.clickButton(LinkText.Copy, { waitForClose: true });
    await waitWhileLoading(this.page);
  }

  async selectDestinationWorkspace(workspaceName: string): Promise<void> {
    const selectMenu = new ReactSelect(this.page, { name: 'Destination *' });

    const input = selectMenu.waitForInput();
    const inputElementHandle = await input.asElementHandle();
    await inputElementHandle.type(workspaceName);

    const option = await selectMenu.waitForOption(workspaceName);
    await option.click({ delay: 20 });
  }
}
