import {ElementHandle, Frame, Page} from 'puppeteer';
import {waitForDocumentTitle} from 'utils/waits-utils';
import {getPropValue} from 'utils/element-utils';
import AuthenticatedPage from './authenticated-page';
import CodeCell, {Selector as CodeCellSelector} from './notebook-code-cell';

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
        waitForDocumentTitle(this.page, this.documentTitle, 120000),
        frame.waitForSelector(CssSelector.runCellButton, {visible: true, timeout: 120000}),
        frame.waitForSelector(CodeCellSelector.codeCell, {visible: true, timeout: 120000}),
        frame.waitForSelector(CssSelector.kernelIcon, {visible: true, timeout: 120000})
      ]);
      return true;
    } catch (e) {
      console.log(`NotebookPage isLoaded() encountered ${e}`);
      return false;
    }
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
  async waitForKernelIdle(timeOut: number = 60000): Promise<boolean> {
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
    const value = await getPropValue(elemt, 'title');
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
    const value = await getPropValue(elemt, 'textContent');
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
    await Promise.all([
      this.waitForKernelIdle(opts.timeOut),
      this.codeCell.waitForOutput(cellIndex, opts.timeOut),
    ]);
    return this.codeCell.waitForOutput(cellIndex);
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
    await this.page.bringToFront();
    await this.page.keyboard.down('Shift');
    await this.page.keyboard.press('Enter', {delay: 20});
    await this.page.keyboard.up('Shift');
  }

  /**
   * Run (Execute) cells using Ctrl+Enter keys.
   * Shortcut explanation: Ctrl+Enter run selected cells.
   */
  async runCellCtrlEnter(): Promise<void> {
    await this.page.bringToFront();
    await this.page.keyboard.down('Control');
    await this.page.keyboard.press('Enter', {delay: 20});
    await this.page.keyboard.up('Control');
  }

  /**
   * Run (Execute) cells using Alt+Enter keys.
   * Shortcut explanation: Alt+Enter run cells and insert below.
   */
  async runCellAltEnter(): Promise<void> {
    await this.page.bringToFront();
    await this.page.keyboard.down('Alt');
    await this.page.keyboard.press('Enter', {delay: 20});
    await this.page.keyboard.up('Alt');
  }

  private async frame(): Promise<Frame> {
    const iframe = await this.page.waitForSelector('iframe[src*="notebooks"]');
    const frameContents = await iframe.contentFrame();
    return frameContents;
  }

}
