import {Page} from 'puppeteer';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils'
import {LinkText} from 'app/text-labels';
import Button from 'app/element/button';
import Modal from './modal';
import Textbox from 'app/element/textbox';
import ReactSelect from 'app/element/react-select';
import {waitWhileLoading, waitForText} from 'utils/waits-utils';

const modalTitle = 'Create a Review-Wide Annotation Field';

export enum AnnotationType {
    FreeText = 'Free Text',
    DropdownList = 'Dropdown List',
    Date = 'Date',
    TrueFalseCheckBox ='True/False CheckBox',
    NumericField = 'Numeric Field'
  }

export default class AnnotationFieldModal extends Modal {

  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async waitForLoad(): Promise<this> {
    await super.waitForLoad();
    try {
      await waitForText(this.page, modalTitle, {xpath: this.getXpath()});
    } catch (e) {
      await savePageToFile(this.page);
      await takeScreenshot(this.page);
      throw new Error(`AnnotationFieldModal waitForLoad() encountered ${e}`);
    }
    return this;
  }

  /**
   * select annotation type.
   * @param {AnnotationType}  option
   */
   async selectAnnotationType(option: AnnotationType): Promise<string> {
    const selectMenu = new ReactSelect(this.page, {name: 'Type:'});
    await selectMenu.selectOption(option);
    await waitWhileLoading(this.page);
    return selectMenu.getSelectedOption();
  }

  async getAnnotationFieldNameTextBox():  Promise<Textbox> {
    return new Textbox(this.page, `${this.getXpath()}//*[contains(text(), "Name")]/ancestor::node()[1]/input[@type="text"]`);
  }

  /**
   * @param {string} newName New name.
   */
  async beginCreateNewAnnotationName(newName?: string): Promise<void> {
    // Type new name.
      const nameInput = await this.getAnnotationFieldNameTextBox();
      await nameInput.type(newName);
  }

  /**
   * @param {string} newName New name.
   */
  // create a Review-Wide Annotation Field name
  async createNewAnnotationName(newName?: string): Promise<void> {
    await this.beginCreateNewAnnotationName(newName);
    await this.clickButton(LinkText.Create, {waitForClose: true});
    await waitWhileLoading(this.page);
    console.log(`created annotation field: "${newName}"`);
  }

  // click cancel button of the anootation field modal
  async cancelAnnotationButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LinkText.Cancel});
  }


}
