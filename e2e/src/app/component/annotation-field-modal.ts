import {Page} from 'puppeteer';
import {savePageToFile, takeScreenshot} from 'utils/save-file-utils'
import {LinkText} from 'src/app/text-labels';
import Button from 'src/app/element/button';
import Modal from './modal';
import ReactSelect from 'src/app/element/react-select';
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


  async createAnnotationButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LinkText.Create});
  }


  async cancelAnnotationButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LinkText.Cancel});
  }


}
