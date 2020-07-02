import Dialog from 'app/component/dialog';
import Textbox from 'app/element/textbox';
import {LinkText} from 'app/text-labels';
import {ElementHandle, Page} from 'puppeteer';


export default class ConceptsetCopyModal extends Dialog {

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async getDestinationTextbox(): Promise<Textbox> {
    return Textbox.findByName(this.page, {containsText: 'Destination'}, this);
  }

  async getNameTextbox(): Promise<Textbox> {
    return Textbox.findByName(this.page, {containsText: 'Name'}, this);
  }

   /**
    *
    * @param {string} workspaceName Workspace name.
    * @param {string} name Give a new name for the copy of Concept Set in new workspace.
    */
  async copyToAnotherWorkspace(workspaceName: string, name?: string): Promise<void> {
    const destinationInput = await this.getDestinationTextbox();
    await destinationInput.click();

    const selectOption = await this.waitForSelectOption(workspaceName);
    await selectOption.click();

    if (name !== undefined) {
      const nameInput = await this.getNameTextbox();
      await nameInput.type(name);
    }

    await this.clickButton(LinkText.Copy);
      // wait for dialog to change (disappear).
    await this.page.waitForFunction(() => !document.querySelector('[role="dialog"]'), {polling: 'mutation'});
  }

  async waitForSelectOption(workspaceName?: string): Promise<ElementHandle> {
    const xpathSubstr = 'starts-with(@id, "react-select-") and contains(@id, "-option-1")';
    if (workspaceName === undefined) {
         // Without a workspace name, select the second (or any) option in dropdown.
      return this.page.waitForXPath(`//*[${xpathSubstr} and text()]`, {visible: true});
    }
    return this.page.waitForXPath(`//*[${xpathSubstr} and text()="${workspaceName}"]`, {visible: true});
  }


}
