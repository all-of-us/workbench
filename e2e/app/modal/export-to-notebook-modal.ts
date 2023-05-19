import RadioButton from 'app/element/radiobutton';
import Textbox from 'app/element/textbox';
import { Page } from 'puppeteer';
import { AnalysisTool, Language, LinkText } from 'app/text-labels';
import Modal from './modal';
import { waitForText } from 'utils/waits-utils';
import NotebookPreviewPage from 'app/page/notebook-preview-page';
import { getFormattedPreviewCode } from 'utils/notebook-preview-utils';

const title = 'Export Dataset';

export default class ExportToNotebookModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, title, { container: this });
    await this.getNotebookNameInput().asElementHandle();
    return true;
  }

  getNotebookNameInput(): Textbox {
    const selector = `${this.getXpath()}//*[@data-test-id="notebook-name-input"]`;
    return new Textbox(this.page, selector);
  }

  getPythonRadioButton(): RadioButton {
    const selector = this.getRadioButtonXpath(Language.Python);
    return new RadioButton(this.page, selector);
  }

  getRRadioButton(): RadioButton {
    const selector = this.getRadioButtonXpath(Language.R);
    return new RadioButton(this.page, selector);
  }

  async enterNotebookName(notebookName: string): Promise<Textbox> {
    const notebookNameInput = this.getNotebookNameInput();
    return notebookNameInput.type(notebookName);
  }

  async pickLanguage(language: Language = Language.Python): Promise<void> {
    const radio = language === Language.Python ? this.getPythonRadioButton() : this.getRRadioButton();
    return radio.select();
  }

  async pickAnalysisTool(analysisTool: AnalysisTool = AnalysisTool.Hail): Promise<void> {
    const radioButtonXpath = this.getRadioButtonXpath(analysisTool);
    const radioButton = new RadioButton(this.page, radioButtonXpath);
    await radioButton.select();
  }

  async showCodePreview(): Promise<string[]> {
    await this.clickButton(LinkText.SeeCodePreview);
    const previewFrame = await this.page.waitForSelector('#popup-root iframe#export-preview-frame', { visible: true });
    return getFormattedPreviewCode(await previewFrame.contentFrame());
  }

  async clickExportButton(): Promise<NotebookPreviewPage> {
    await this.clickButton(LinkText.Export, { waitForClose: true });
    const notebookPreviewPage = new NotebookPreviewPage(this.page);
    await notebookPreviewPage.waitForLoad();
    return notebookPreviewPage;
  }

  /**
   * Fill out Export Notebook modal to create a new notebook:
   * - Type notebook name.
   * - Select notebook language.
   * - Click "Export and Open" button.
   * @param {string} notebookName Notebook name.
   * @param {Language} language Notebook programming language. Default value is Python.
   */
  async fillInModal(notebookName: string, language: Language = Language.Python): Promise<NotebookPreviewPage> {
    await this.enterNotebookName(notebookName);
    await this.pickLanguage(language);
    return this.clickExportButton();
  }

  private getRadioButtonXpath(name: string): string {
    return `${this.getXpath()}//label[contains(normalize-space(),"${name}")]//input[@type="radio"]`;
  }
}
