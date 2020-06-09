import {Page} from 'puppeteer';
import Dialog, {ButtonLabel} from 'app/component/dialog';
import {makeRandomName} from 'utils/str-utils';
import {waitWhileLoading} from 'utils/test-utils';
import RadioButton from 'app/element/radiobutton';
import Textbox from 'app/element/textbox';

export default class DatasetSaveModal extends Dialog {

  constructor(page: Page) {
    super(page);
  }

  /**
   * Handle Save or Update dialog.
   * @param {boolean} isExportToNotebook If True, select Export To Notebook checkbox.
   * @param {boolean} isUpdate If true, click Update button. If False, click Save button.
   * @param {string} notebookName New notebook name to be created or select an existing notebook.
   * @param {String} language Notebook programming language.
   */
  async saveDataset(opts:
                       {isExportToNotebook?: boolean, isUpdate?: boolean, notebookName?: string, language?: string} = {}): Promise<string> {
    const {isExportToNotebook = false, isUpdate = false, notebookName, language = 'Python'} = opts;
    const newDatasetName = makeRandomName();

    const nameTextbox = await this.waitForTextbox('Dataset Name');
    await nameTextbox.type(newDatasetName);

    // Export to Notebook checkbox is checked by default
    const exportCheckbox = await this.waitForCheckbox('Export to notebook');
      // Not Export to Notebook
    if (isExportToNotebook) {
      const notebookNameTextbox = new Textbox(this.page, '//*[@data-test-id="notebook-name-input"]');
      await notebookNameTextbox.type(notebookName);
      const radioBtn = await RadioButton.findByName(this.page, {name: language});
      await radioBtn.select();
    } else {
      await exportCheckbox.unCheck();
    }
    await waitWhileLoading(this.page);
    if (isUpdate) {
      await this.waitForButton(ButtonLabel.Update).then(btn => btn.clickAndWait());
    } else {
      await this.waitForButton(ButtonLabel.Save).then(btn => btn.clickAndWait());
    }
    await waitWhileLoading(this.page);
    return newDatasetName;
  }

}
