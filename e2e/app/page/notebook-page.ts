import {ElementHandle, Frame, Page} from 'puppeteer';
import {waitForDocumentTitle} from 'utils/waits-utils';
import {getPropValue} from 'utils/element-utils';
import {waitWhileLoading} from 'utils/test-utils';
import AuthenticatedPage from './authenticated-page';
import CodeCell, {Selector as CodeCellSelector} from './notebook-code-cell';
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

enum Mode {
  Command= 'command_mode',
  Edit = 'command_mode',
}

export enum KernelStatus {
  notRunning = 'Kernel is not running',
  Idle = 'Kernel Idle',
}

export default class NotebookPage extends AuthenticatedPage {
  private codeCell: CodeCell;

  constructor(page: Page, private readonly documentTitle) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      const frame = await this.frame();
      this.codeCell = new CodeCell(frame);
      // Wait up to 2 minutes.
      await Promise.all([
        waitForDocumentTitle(this.page, this.documentTitle),
        frame.waitForSelector(CssSelector.runCellButton, {visible: true}),
        frame.waitForSelector(CodeCellSelector.codeCell, {visible: true}),
        frame.waitForSelector(CssSelector.kernelIcon, {visible: true})
      ]);
      return true;
    } catch (e) {
      console.log(`NotebookPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  /**
   * Click "Notebook" link, goto Workspace Analysis tab. This function does not handle Unsaved Changes confirmation.
   */
  async goBackAnalysisPage(): Promise<WorkspaceAnalysisPage> {
    const navPromise = this.page.waitForNavigation({ waitUntil: ['load', 'domcontentloaded', 'networkidle0'] });
    await this.notebookLink().then( (link) => link.click());
    await navPromise;
    await waitWhileLoading(this.page);
    const analysisPage = new WorkspaceAnalysisPage(this.page);
    await analysisPage.waitForLoad();
    return analysisPage;
  }

  async notebookLink(): Promise<ElementHandle> {
    const selector = '//a[text()="Notebooks"]';
    return this.page.waitForXPath(selector, {visible: true});
  }

  /**
   * Click Run button in toolbar. It run focused cell and insert a new cell below.
   */
  async runButton(): Promise<void> {
    return this.frame()
      .then( (iframe) => iframe.waitForSelector(CssSelector.runCellButton, {visible: true}))
      .then( (butn) => butn.click() );
  }

  /**
   * Click Save button in toolbar.
   */
  async saveNotebook(): Promise<void> {
    return this.frame()
      .then( (iframe) => iframe.waitForSelector(CssSelector.saveNotebookButton, {visible: true}))
      .then( (butn) => butn.click() );
  }

  /**
   * Wait for notebook kernel becomes ready (idle).
   */
  async waitForKernelIdle(timeOut?: number): Promise<boolean> {
    const iconSelector = `${CssSelector.kernelIcon}.kernel_idle_icon`;
    const notifSelector = '#notification_kernel';
    const frame = await this.frame();
    try {
      await Promise.all([
        frame.waitForSelector(iconSelector, {visible: true, timeout: timeOut}),
        frame.waitForSelector(notifSelector, {hidden: true, timeout: timeOut}),
      ])
      return true;
    } catch (e) {
      return false;
    }
  }

  async kernelStatus(): Promise<KernelStatus | null> {
    const elemt = await this.frame().then( (frame) => frame.waitForSelector(CssSelector.kernelIcon, {visible: true}));
    const value = await getPropValue<string>(elemt, 'title');
    await elemt.dispose();
    Object.keys(KernelStatus).forEach(key => {
      if (KernelStatus[key] === value) {
        return key;
      }
    })
    return null;
  }

  async getKernelName(): Promise<string> {
    const frame = await this.frame();
    const elemt = await frame.waitForSelector(CssSelector.kernelName, {visible: true});
    const value = await getPropValue<string>(elemt, 'textContent');
    await elemt.dispose();
    return value;
  }

  /**
   * Click Run button in toolbar. It run focused cell and insert a new cell below.
   * @param {number} cellIndex Code Cell index. (index number starts from 1)
   * @param {string} code New code.
   * @return {string} Output string.
   */
  async runCodeCell(cellIndex: number, opts: { code?: string, timeOut?: number }): Promise<string> {
    await this.codeCell.selectCell(cellIndex, opts.code);
    await this.runButton();
    const [output] = await Promise.all([
      this.codeCell.findCellOutput(cellIndex, opts.timeOut),
      this.waitForKernelIdle(opts.timeOut),
    ]);
    return output;
  }

  async findCodeCellInput(cellIndex: number): Promise<string> {
    const codeCell = new CodeCell(await this.frame());
    const elemt = await codeCell.findCellInput(cellIndex);
    return getPropValue<string>(elemt, 'outerText');
  }

  async findCodeCellOutput(cellIndex: number): Promise<string> {
    const codeCell = new CodeCell(await this.frame());
    return codeCell.findCellOutput(cellIndex);
  }


  // ****************************************************************************

  /**
   * Active Notebook Command or Edit mode.
   * @param mode
   */
  async toggleMode(mode: Mode): Promise<void> {
    // Press Esc key to activate command mode
    if (mode === Mode.Command) {
      return this.page.keyboard.press('Escape');
    }
    // Press Enter key to activate edit mode
    return this.page.keyboard.press('Enter');
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

  private async frame(): Promise<Frame> {
    const iframe = await this.page.waitForSelector('iframe[src*="notebooks"]');
    return iframe.contentFrame();
  }

}
