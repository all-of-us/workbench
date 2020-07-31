import {Page} from 'puppeteer';
import Modal from 'app/component/modal';
import {makeRandomName} from 'utils/str-utils';
import {waitWhileLoading} from 'utils/test-utils';
import RadioButton from 'app/element/radiobutton';
import Textbox from 'app/element/textbox';
import {Language, LinkText} from 'app/text-labels';
import Button from 'app/element/button';

export default class DatasetSaveModal extends Modal {

  constructor(page: Page) {
    super(page);
  }

  /**
   * Handle Save or Update dialog.
   * @param notebookOpts {}
   * @param {boolean} isUpdate If true, click Update button. If False, click Save button.
   *
   * <pre>
   * {boolean} exportToNotebook If True, select Export To Notebook checkbox.
   * {string} notebookName New notebook name to be created or select an existing notebook.
   * {String} lang Notebook programming language.
   * </pre>
   */
  async saveDataset(notebookOpts: {exportToNotebook?: boolean, notebookName?: string, lang?: Language} = {},
                    isUpdate: boolean = false): Promise<string> {

    const {exportToNotebook = false, notebookName, lang = Language.Python} = notebookOpts;
    const newDatasetName = makeRandomName();

    const nameTextbox = await this.waitForTextbox('Dataset Name');
    await nameTextbox.clearTextInput();
    await nameTextbox.type(newDatasetName);

    // Export to Notebook checkbox is checked by default
    const exportCheckbox = await this.waitForCheckbox('Export to notebook');

    if (exportToNotebook) {
      // Export to notebook
      const notebookNameTextbox = new Textbox(this.page, `${this.getXpath()}//*[@data-test-id="notebook-name-input"]`);
      await notebookNameTextbox.type(notebookName);
      console.log(`Notebook language: ` + lang);
      const radioBtn = await RadioButton.findByName(this.page, {name: lang, ancestorLevel: 0}, this);
      await radioBtn.select();
    } else {
      // Not export to notebook
      await exportCheckbox.unCheck();
    }
    await waitWhileLoading(this.page);

    let finishButton: Button;
    if (isUpdate) {
      finishButton = await this.waitForButton(LinkText.Update);
    } else {
      finishButton = await this.waitForButton(LinkText.Save);
    }
    await finishButton.waitUntilEnabled();
    await finishButton.clickAndWait();
    await waitWhileLoading(this.page);

    if (isUpdate) {
      console.log(`Updated Dataset "${newDatasetName}"`);
    } else {
      console.log(`Created Dataset "${newDatasetName}"`);
    }
    if (exportToNotebook) {
      console.log(`Created Notebook "${notebookName}"`);
    }
    return newDatasetName;
  }

}
