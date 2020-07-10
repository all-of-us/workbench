import Modal from 'app/component/modal';
import Textbox from 'app/element/textbox';
import {LinkText} from 'app/text-labels';
import {ElementHandle, Page} from 'puppeteer';


export default class ConceptsetCopyModal extends Modal {

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async getDestinationTextbox(): Promise<Textbox> {
    return Textbox.findByName(this.page, {containsText: 'Destination'}, this);
  }

  async getNameTextbox(): Promise<Textbox> {
    return new Textbox(this.page, `${this.getXpath()}//*[contains(text(), "Name")]/ancestor::node()[1]/input[@type="text"]`);
  }

 /**
  *
  * @param {string} workspaceName Workspace name.
  * @param {string} name Give a new name for the copy of Concept Set in new workspace.
  */
  async copyToAnotherWorkspace(workspaceName: string, name?: string): Promise<void> {
    // Click inside textbox to open dropdown.
    const destinationInput = await this.getDestinationTextbox();
    await destinationInput.click();
    // Select dropdown option
    const selectOption = await this.waitForSelectOption(workspaceName);
    await selectOption.click();
    // Type new Concept Set if parameter is not undefined.
    if (name !== undefined) {
      const nameInput = await this.getNameTextbox();
      await nameInput.type(name);
    }
    await this.clickButton(LinkText.Copy);
  }

  async waitForSelectOption(workspaceName?: string): Promise<ElementHandle> {
    const xpathSubstr = 'starts-with(@id, "react-select-") and contains(@id, "-option-")';
    if (workspaceName === undefined) {
      // Without a workspace name, select the second (or any) option in dropdown.
      return this.page.waitForXPath(`${this.getXpath()}//*[${xpathSubstr} and text()]`, {visible: true});
    }
    return this.page.waitForXPath(`${this.getXpath()}//*[${xpathSubstr} and text()="${workspaceName}"]`, {visible: true});
  }


}
