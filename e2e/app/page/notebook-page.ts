import * as fs from 'fs';
import { ElementHandle, Page } from 'puppeteer';
import { getPropValue } from 'utils/element-utils';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import { LinkText, ResourceCard } from 'app/text-labels';
import RuntimePanel, { StartStopIconState } from 'app/component/runtime-panel';
import NotebookCell, { CellType } from './notebook-cell';
import NotebookDownloadModal from 'app/modal/notebook-download-modal';
import NotebookPreviewPage from './notebook-preview-page';
import WorkspaceAnalysisPage from './workspace-analysis-page';
import WorkspaceDataPage from './workspace-data-page';
import Link from 'app/element/link';
import NotebookFrame from './notebook-frame';

// CSS selectors
enum CssSelector {
  body = 'body.notebook_app',
  notebookContainer = '#notebook-container',
  toolbarContainer = '#maintoolbar-container',
  runCellButton = 'button[data-jupyter-action="jupyter-notebook:run-cell-and-select-next"]',
  saveNotebookButton = 'button[data-jupyter-action="jupyter-notebook:save-notebook"]',
  kernelIcon = '#kernel_indicator_icon',
  kernelName = '.kernel_indicator_name'
}

enum Xpath {
  fileMenuDropdown = './/a[text()="File"]',
  downloadMenuDropdown = './/a[text()="Download as"]',
  downloadIpynbButton = './/*[@id="download_script"]/a',
  downloadMarkdownButton = './/*[@id="download_markdown"]/a'
}

export enum Mode {
  Command = 'command_mode',
  Edit = 'edit_mode'
}

export enum KernelStatus {
  NotRunning = 'Kernel is not running',
  Idle = 'Kernel Idle'
}

export default class NotebookPage extends NotebookFrame {
  constructor(page: Page, private readonly documentTitle: string) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitForDocumentTitle(this.page, this.documentTitle);
    try {
      await this.findRunButton(120000);
    } catch (err) {
      console.warn(`Reloading "${this.documentTitle}" because cannot find the Run button`);
      await this.page.reload({ waitUntil: ['networkidle0', 'load'] });
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
    await this.page.waitForXPath(selector, { visible: true }).then((link) => link.click());
    await navPromise;
    await waitWhileLoading(this.page);
    const analysisPage = new WorkspaceAnalysisPage(this.page);
    await analysisPage.waitForLoad();
    return analysisPage;
  }

  /**
   * Click workspace name, goto Workspace Data page.
   * This function does not handle Unsaved Changes confirmation.
   */
  async goDataPage(): Promise<WorkspaceDataPage> {
    const navPromise = this.page.waitForNavigation({ waitUntil: ['load', 'domcontentloaded', 'networkidle0'] });
    await this.getWorkspaceLink().click();
    await navPromise;
    const dataPage = new WorkspaceDataPage(this.page);
    await dataPage.waitForLoad();
    return dataPage;
  }

  getWorkspaceLink(): Link {
    const selector = '//a[contains(@href, "/data")]';
    return new Link(this.page, selector);
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
    const saveButton = await frame.waitForSelector(CssSelector.saveNotebookButton, { visible: true });
    await saveButton.click();
    await saveButton.dispose();
  }

  private async downloadAs(formatXpath: string): Promise<NotebookDownloadModal> {
    const frame = await this.getIFrame();
    await frame.waitForXPath(Xpath.fileMenuDropdown, { visible: true }).then((element) => element.click());
    await frame.waitForXPath(Xpath.downloadMenuDropdown, { visible: true }).then((element) => element.hover());
    const menuOption = await frame.waitForXPath(formatXpath, { visible: true });
    await menuOption.hover();
    await menuOption.click();

    const modal = new NotebookDownloadModal(this.page);
    return modal.waitForLoad();
  }

  async downloadAsIpynb(): Promise<NotebookDownloadModal> {
    return this.downloadAs(Xpath.downloadIpynbButton);
  }

  async downloadAsMarkdown(): Promise<NotebookDownloadModal> {
    return this.downloadAs(Xpath.downloadMarkdownButton);
  }

  /**
   * Wait for notebook kernel becomes ready (idle).
   */
  async waitForKernelIdle(timeOut?: number): Promise<void> {
    const frame = await this.getIFrame();
    const idleIconSelector = `${CssSelector.kernelIcon}.kernel_idle_icon`;
    const notificationSelector = '#notification_kernel';
    const isIdle = async (timeout): Promise<boolean> => {
      return Promise.all([
        frame.waitForSelector(idleIconSelector, { visible: true, timeout }),
        frame.waitForSelector(notificationSelector, { hidden: true, timeout })
      ])
        .then(() => {
          return true;
        })
        .catch(() => {
          return false;
        });
    };
    // Check kernel status twice with a pause between two checks because kernel status can suddenly become not ready.
    let ready = false;
    const startTime = Date.now();
    while (Date.now() - startTime <= timeOut) {
      const idle = await isIdle(30000);
      if (ready && idle) {
        break;
      }
      ready = idle;
      await this.page.waitForTimeout(5000);
    }
    // Throws exception if not ready.
    try {
      await Promise.all([
        frame.waitForSelector(idleIconSelector, { visible: true, timeout: 1000 }),
        frame.waitForSelector(notificationSelector, { hidden: true, timeout: 1000 })
      ]);
    } catch (e) {
      throw new Error(`Notebook kernel is ${await this.getKernelStatus()}. waitForKernelIdle() encountered ${e}`);
    }
  }

  async getKernelStatus(): Promise<KernelStatus | string> {
    const frame = await this.getIFrame();
    const elemt = await frame.waitForSelector(CssSelector.kernelIcon, { visible: true });
    const value = await getPropValue<string>(elemt, 'title');
    await elemt.dispose();
    Object.keys(KernelStatus).forEach((key) => {
      if (KernelStatus[key] === value) {
        return key;
      }
    });
    return value;
  }

  async getKernelName(): Promise<string> {
    const frame = await this.getIFrame();
    const elemt = await frame.waitForSelector(CssSelector.kernelName, { visible: true });
    const value = await getPropValue<string>(elemt, 'textContent');
    await elemt.dispose();
    return value.trim();
  }

  /**
   *
   * @param {number} cellIndex Code Cell index. (first index is 1)
   * @param {CellType} cellType: Code or Markdown cell. Default value is Code cell.
   */
  findCell(cellIndex: number, cellType: CellType = CellType.Code): NotebookCell {
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
    opts: { code?: string; codeFile?: string; timeOut?: number; markdownWorkaround?: boolean } = {}
  ): Promise<string> {
    const cell = cellIndex === -1 ? await this.findLastCell() : this.findCell(cellIndex);
    const inputCell = await cell.focus();

    const { code, codeFile, timeOut = 2 * 60 * 1000, markdownWorkaround = false } = opts;

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
      const markdownCell = this.findCell(cellIndex, CellType.Markdown);
      const markdownCellInput = await markdownCell.focus();
      await markdownCellInput.type(codeToRun);
      await this.changeToCodeCell();
    } else {
      if (codeToRun) {
        await inputCell.type(codeToRun);
      }
    }

    await inputCell.dispose();
    await this.run();
    await this.waitForKernelIdle(timeOut);
    const [output] = await Promise.all([
      cell.waitForOutput(timeOut),
      this.waitForKernelIdle(timeOut) // Wait for kernel idle again because sometimes kernel turns unexpectedly.
    ]);
    return output;
  }

  /**
   * Returns cell input and output texts in an array. Not waiting for output rendered.
   * @param {number} cellIndex Code Cell index. (first index is 1)
   * @param {CellType} cellType: Markdown or Code. Default value is Code cell.
   */
  async getCellInputOutput(cellIndex: number, cellType: CellType = CellType.Code): Promise<[string, string]> {
    const cell = this.findCell(cellIndex, cellType);
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

  /**
   * Delete runtime
   */
  async deleteRuntime(): Promise<void> {
    // Open runtime panel
    const runtimePanel = new RuntimePanel(this.page);
    await runtimePanel.open();

    // Click 'delete environment' then Delete buttons.
    await runtimePanel.clickButton(LinkText.DeleteEnvironment);
    await runtimePanel.clickButton(LinkText.Delete);

    const notebookPreviewPage = new NotebookPreviewPage(this.page);
    await notebookPreviewPage.waitForLoad();

    // Wait until runtime status indicats None.
    await runtimePanel.open();
    await runtimePanel.waitForStartStopIconState(StartStopIconState.Stopping);
    await runtimePanel.waitForStartStopIconState(StartStopIconState.None);
    await runtimePanel.close();
  }

  private async findRunButton(timeout?: number): Promise<ElementHandle> {
    const frame = await this.getIFrame();
    return frame.waitForSelector(CssSelector.runCellButton, { visible: true, timeout });
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
      await this.getIFrame().then((frame) => frame.waitForSelector('body.notebook_app.command_mode'));
      return;
    }
    // Press Enter key to activate edit mode
    await this.page.keyboard.press('Enter');
    await this.getIFrame().then((frame) => frame.waitForSelector('body.notebook_app.edit_mode'));
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
    await this.page.keyboard.press('Enter', { delay: 20 });
    await this.page.keyboard.up(keyboardCommand);
  }
}
