import {Page} from 'puppeteer';
import Dialog, {ButtonLabel} from 'app/component/dialog';
import {makeRandomName} from 'utils/str-utils';
import {waitWhileLoading} from 'utils/test-utils';
import RadioButton from 'app/element/radiobutton';
import Textbox from 'app/element/textbox';

export enum Language {
  Python = 'Python',
  R = 'R',
}

export default class DatasetSaveModal extends Dialog {

  constructor(page: Page) {
    super(page);
  }

  /**
   * Handle Save or Update dialog.
   * @param notebookOpts {}
   * @param {boolean} isUpdate If true, click Update button. If False, click Save button.
   *
   * <pre>
   * {boolean} isExportToNotebook If True, select Export To Notebook checkbox.
   * {string} notebookName New notebook name to be created or select an existing notebook.
   * {String} language Notebook programming language.
   * </pre>
   */
  async saveDataset(notebookOpts: { isExportToNotebook?: boolean, notebookName?: string, language?: Language} = {},
                    isUpdate: boolean  = false): Promise<string> {

    const {isExportToNotebook = false, notebookName, language = Language.Python} = notebookOpts;
    const newDatasetName = makeRandomName();

    const nameTextbox = await this.waitForTextbox('Dataset Name');
    await nameTextbox.type(newDatasetName);

    // Export to Notebook checkbox is checked by default
    const exportCheckbox = await this.waitForCheckbox('Export to notebook');

    if (isExportToNotebook) {
      // Export to notebook
      const notebookNameTextbox = new Textbox(this.page, '//*[@data-test-id="notebook-name-input"]');
      await notebookNameTextbox.type(notebookName);
      const radioBtn = await RadioButton.findByName(this.page, {name: language});
      await radioBtn.select();
    } else {
      // Not export to notebook
      await exportCheckbox.unCheck();
    }
    await waitWhileLoading(this.page);
    if (isUpdate) {
      await this.waitForButton(ButtonLabel.Update).then(btn => btn.clickAndWait());
    } else {
      await this.waitForButton(ButtonLabel.Save).then(btn => btn.clickAndWait());
    }
    await waitWhileLoading(this.page);
    console.log(`Created Dataset "${newDatasetName}"`);
    if (isExportToNotebook) {
      console.log(`Created Notebook "${notebookName}"`);
    }
    return newDatasetName;
  }

}
