import * as fs from 'fs';
import {ElementHandle, Frame, Page} from 'puppeteer';
import {getPropValue} from 'utils/element-utils';
import {waitForDocumentTitle, waitWhileLoading} from 'utils/waits-utils';
import {ResourceCard} from 'app/text-labels';
import AuthenticatedPage from './authenticated-page';
import NotebookCell, {CellType} from './notebook-cell';
import NotebookDownloadModal from 'app/modal/notebook-download-modal';
import WorkspaceAnalysisPage from './workspace-analysis-page';

// CSS selectors
enum CssSelector {
  body = 'body.notebook_app',
  notebookContainer = '#notebook-container',

  toolbarContainer = '#maintoolbar-container',
  runCellButton = 'button[data-jupyter-action="jupyter-notebook:run-cell-and-select-next"]',
  saveNotebookButton = 'button[data-jupyter-action="jupyter-notebook:save-notebook"]',
  kernelIcon = '#kernel_indicator_icon',
  kernelName = '.kernel_indicator_name',
}

enum Xpath {
  fileMenuDropdown = '//a[text()="File"]',
  downloadMenuDropdown = '//a[text()="Download as"]',
  downloadIpynbButton = '//*[@id="download_script"]/a',
  downloadMarkdownButton = '//*[@id="download_markdown"]/a',
}

export enum Mode {
  Command= 'command_mode',
  Edit = 'edit_mode',
}

export enum KernelStatus {
  NotRunning = 'Kernel is not running',
  Idle = 'Kernel Idle',
}

export default class NotebookPage extends AuthenticatedPage {

  constructor(page: Page, private readonly documentTitle) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitForDocumentTitle(this.page, this.documentTitle);
    try {
      await this.findRunButton(120000);
    } catch (err) {
      console.warn(`Reloading "${this.documentTitle}" because cannot find the Run button`);
      await this.page.reload({waitUntil: ['networkidle0', 'load']});
    }
    await this.waitForKernelIdle(10 * 60 * 1000); // 10 minutes
    return true;
  }

  /**
   * Click "Notebook" link, goto Workspace Analysis page.
   * This function does not handle Unsaved Changes confirmation.
   */
  async goAnalysisPage(): Promise<WorkspaceAnalysisPage> {
    const selector = '//a[text()="Notebooks"]';
    const navPromise = this.page.waitForNavigation({ waitUntil: ['load', 'domcontentloaded', 'networkidle0'] });
    await this.page.waitForXPath(selector, {visible: true}).then( (link) => link.click());
    await navPromise;
    await waitWhileLoading(this.page);
    const analysisPage = new WorkspaceAnalysisPage(this.page);
    await analysisPage.waitForLoad();
    return analysisPage;
  }

  /**
   * Run focused cell and insert a new cell below. Click Run button in toolbar.
   */
  async run(): Promise<void> {
    const runButton = await this.findRunButton();
    await runButton.click();
    await this.page.waitForTimeout(1000);
    await runButton.dispose();
  }

  /**
   * Save notebook. Click Save button in toolbar.
   */
  async save(): Promise<void> {
    const frame = await this.getIFrame();
    const saveButton = await frame.waitForSelector(CssSelector.saveNotebookButton, {visible: true});
    await saveButton.click();
    await saveButton.dispose();
  }

  private async downloadAs(formatXpath: string): Promise<NotebookDownloadModal> {
    const frame = await this.getIFrame();

    await (await frame.waitForXPath(Xpath.fileMenuDropdown, {visible: true})).click();
    await (await frame.waitForXPath(Xpath.downloadMenuDropdown, {visible: true})).hover();
    await (await frame.waitForXPath(formatXpath, {visible: true})).click();

    const modal = new NotebookDownloadModal(this.page, frame);
    return await modal.waitForLoad();
  }

  async downloadAsIpynb(): Promise<NotebookDownloadModal> {
    return this.downloadAs(Xpath.downloadIpynbButton)
  }

  async downloadAsMarkdown(): Promise<NotebookDownloadModal> {
    return this.downloadAs(Xpath.downloadMarkdownButton);
  }

  /**
   * Wait for notebook kernel becomes ready (idle).
   */
  async waitForKernelIdle(timeOut?: number): Promise<void> {
    const idleIconSelector = `${CssSelector.kernelIcon}.kernel_idle_icon`;
    const notifSelector = '#notification_kernel';
    const frame = await this.getIFrame();
    try {
      await Promise.all([
        frame.waitForSelector(idleIconSelector, {visible: true, timeout: timeOut}),
        frame.waitForSelector(notifSelector, {hidden: true, timeout: timeOut}),
      ]);
    } catch (e) {
      throw new Error(`Notebook kernel is ${await this.getKernelStatus()}. waitForKernelIdle() encountered ${e}`);
    }
  }

  async getKernelStatus(): Promise<KernelStatus | string> {
    const frame = await this.getIFrame();
    const elemt = await frame.waitForSelector(CssSelector.kernelIcon, {visible: true});
    const value = await getPropValue<string>(elemt, 'title');
    await elemt.dispose();
    Object.keys(KernelStatus).forEach(key => {
      if (KernelStatus[key] === value) {
        return key;
      }
    })
    return value;
  }

  async getKernelName(): Promise<string> {
    const frame = await this.getIFrame();
    const elemt = await frame.waitForSelector(CssSelector.kernelName, {visible: true});
    const value = await getPropValue<string>(elemt, 'textContent');
    await elemt.dispose();
    return value.trim();
  }

  /**
   *
   * @param {number} cellIndex Code Cell index. (first index is 1)
   * @param {CellType} cellType: Code or Markdown cell. Default value is Code cell.
   */
  async findCell(cellIndex: number, cellType: CellType = CellType.Code): Promise<NotebookCell> {
    const cell = new NotebookCell(this.page, cellType, cellIndex);
    return cell;
  }

  /**
   * Find the last cell.
   * @param {CellType} cellType: Code or Markdown cell. Default value is Code cell.
   */
  async findLastCell(cellType: CellType = CellType.Code): Promise<NotebookCell | null> {
    const cell = new NotebookCell(this.page, cellType);
    await cell.getLastCell();
    return cell;
  }

  /**
   * Click Run button in toolbar. Run focused code cell and insert a new code cell below.
   *
   * @param {number} cellIndex Code Cell index. (first index is 1). Use -1 to find last cell.
   * @param opts
   *  {string} code The code to run.
   *  {string} codeFile The full path to a file that contains the code to run.
   *  {number} timeOut The timeout time in milliseconds (default 120 sec).
   *  {boolean} markdownWorkaround Convert to Markdown before typing (default false)
   */
  async runCodeCell(
      cellIndex: number,
      opts: { code?: string, codeFile?: string, timeOut?: number, markdownWorkaround?: boolean } = {}): Promise<string> {
    const cell = cellIndex === -1 ? await this.findLastCell() : await this.findCell(cellIndex);
    const inputCell = await cell.focus();

    const {code, codeFile, timeOut = 2 * 60 * 1000, markdownWorkaround = false} = opts;

    let codeToRun;
    if (code !== undefined) {
      codeToRun = code;
    } else if (codeFile !== undefined) {
      codeToRun = fs.readFileSync(codeFile, 'ascii');
    }

    // autoCloseBrackets is true by default for R code cells.
    // Puppeteer types in every character of code, resulting in extra brackets.
    // Workaround: Type code in Markdown cell, then change to Code cell to run.
    if (markdownWorkaround) {
      await this.changeToMarkdownCell();
      const markdownCell = await this.findCell(cellIndex, CellType.Markdown);
      const markdownCellInput = await markdownCell.focus();
      await markdownCellInput.type(codeToRun);
      await this.changeToCodeCell();
    } else {
      await inputCell.type(codeToRun);
    }

    await inputCell.dispose();
    await this.run();
    await this.waitForKernelIdle(timeOut);
    const [output] = await Promise.all([
      cell.waitForOutput(timeOut),
      this.waitForKernelIdle(timeOut), // Wait for kernel idle again because sometimes kernel turns unexpectely.
    ]);
    return output;
  }

  /**
   * Returns cell input and output texts in an array. Not waiting for output rendered.
   * @param {number} cellIndex Code Cell index. (first index is 1)
   * @param {CellType} cellType: Markdown or Code. Default value is Code cell.
   */
  async getCellInputOutput(cellIndex: number, cellType: CellType = CellType.Code): Promise<[string, string]> {
    const cell = await this.findCell(cellIndex, cellType);
    const code = await cell.getInputText();
    const output = await cell.waitForOutput(1000);
    return [code, output];
  }

  /**
   * Delete notebook
   */
  async deleteNotebook(notebookName: string): Promise<void> {
    const analysisPage = await this.goAnalysisPage();
    await analysisPage.deleteResource(notebookName, ResourceCard.Notebook);
  }

  private async getIFrame(): Promise<Frame> {
    const frame = await this.page.waitForSelector('iframe[src*="notebooks"]');
    return frame.contentFrame();
  }

  private async findRunButton(timeout?: number): Promise<ElementHandle> {
    const frame = await this.getIFrame();
    return frame.waitForSelector(CssSelector.runCellButton, {visible: true, timeout});
  }

  // ****************************************************************************

  /**
   * Change selected cell type to Markdown.
   */
  async changeToMarkdownCell(): Promise<void> {
    await this.toggleMode(Mode.Command);
    await this.page.keyboard.press('M');
  }

  /**
   * Change selected cell type to Code.
   */
  async changeToCodeCell(): Promise<void> {
    await this.toggleMode(Mode.Command);
    await this.page.keyboard.press('Y');
  }

  /**
   * Active Notebook Command or Edit mode.
   * @param mode
   */
  async toggleMode(mode: Mode): Promise<void> {
    // Press Esc key to activate command mode
    if (mode === Mode.Command) {
      await this.page.keyboard.press('Escape');
      await this.getIFrame().then(frame => frame.waitForSelector('body.notebook_app.command_mode'));
      return;
    }
    // Press Enter key to activate edit mode
    await this.page.keyboard.press('Enter');
    await this.getIFrame().then(frame => frame.waitForSelector('body.notebook_app.edit_mode'));
    return;
  }

  /**
   * Run (Execute) cells using Shift+Enter keys.
   * Shortcut explanation: Shift+Enter run cells and select below.
   */
  async runCellShiftEnter(): Promise<void> {
    return this.runCommand('Shift');
  }

  /**
   * Run (Execute) cells using Ctrl+Enter keys.
   * Shortcut explanation: Ctrl+Enter run selected cells.
   */
  async runCellCtrlEnter(): Promise<void> {
    return this.runCommand('Control');
  }

  /**
   * Run (Execute) cells using Alt+Enter keys.
   * Shortcut explanation: Alt+Enter run cells and insert below.
   */
  async runCellAltEnter(): Promise<void> {
    return this.runCommand('Alt');
  }

  private async runCommand(keyboardCommand: string): Promise<void> {
    await this.page.bringToFront();
    await this.page.keyboard.down(keyboardCommand);
    await this.page.keyboard.press('Enter', {delay: 20});
    await this.page.keyboard.up(keyboardCommand);
  }

}
