import {Frame, Page} from 'puppeteer';
import {getPropValue} from 'utils/element-utils';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import AuthenticatedPage from './authenticated-page';
import NotebookCell, {CellType} from './notebook-cell';
import WorkspaceAnalysisPage from './workspace-analysis-page';
import * as fs from 'fs';

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

export enum Mode {
  Command= 'command_mode',
  Edit = 'edit_mode',
}

export enum KernelStatus {
  notRunning = 'Kernel is not running',
  Idle = 'Kernel Idle',
}

export default class NotebookPage extends AuthenticatedPage {

  constructor(page: Page, private readonly documentTitle) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await waitForDocumentTitle(this.page, this.documentTitle);
      await this.waitForKernelIdle();
      return true;
    } catch (e) {
      console.error(`NotebookPage isLoaded() encountered ${e}`);
      return false;
    }
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
    const frame = await this.getIFrame();
    const runButton = await frame.waitForSelector(CssSelector.runCellButton, {visible: true});
    await runButton.click();
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
      console.error(`Notebook kernel is: ${await this.kernelStatus()}`);
      throw new Error(`waitForKernelIdle encountered ${e}`);
    }
  }

  async kernelStatus(): Promise<KernelStatus | string> {
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
   * @param {number} cellIndex Code Cell index. (first index is 1)
   * @param {string} code The code to run.
   * @param {string} codeFile The full path to file that contains code to run.
   * @param {number} timeOut The timeout time in milliseconds.
   * @returns {string} Run output.
   */
  async runCodeCell(cellIndex: number, opts: { code?: string, codeFile?: string, timeOut?: number } = {}): Promise<string> {
    const cell = await this.findCell(cellIndex)
    const cellInput = await cell.focus();
    if (opts.code !== undefined) {
      await cellInput.type(opts.code);
    } else if (opts.codeFile !== undefined) {
      const code = fs.readFileSync(opts.codeFile, 'utf8');
      await cellInput.type(code);
    }
    await cellInput.dispose();
    await this.run();
    await this.waitForKernelIdle(opts.timeOut);
    const [output] = await Promise.all([
      cell.waitForOutput(opts.timeOut),
      this.waitForKernelIdle(opts.timeOut), // Wait for kernel idle again because sometimes kernel turns unexpectely.
    ]);
    return output;
  }

  /**
   * Returnss cell input and output texts in an array. Not waiting for output rendered.
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
    await analysisPage.deleteNotebook(notebookName);
  }

  private async getIFrame(): Promise<Frame> {
    const frame = await this.page.waitForSelector('iframe[src*="notebooks"]');
    return frame.contentFrame();
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
      console.log('Notebook in command mode');
      return;
    }
    // Press Enter key to activate edit mode
    await this.page.keyboard.press('Enter');
    await this.getIFrame().then(frame => frame.waitForSelector('body.notebook_app.edit_mode'));
    console.log('Notebook in edit mode');
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
