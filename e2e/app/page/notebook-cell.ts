import { ElementHandle, Page } from 'puppeteer';
import { getPropValue } from 'utils/element-utils';
import NotebookFrame from './notebook-frame';

export enum CellType {
  // To append to css selector
  Code = '.code_cell',
  Markdown = '.text_cell',
  Any = ''
}

/**
 * Notebook Cell represents the root element that contains both the code input and output cells.
 */
export default class NotebookCell extends NotebookFrame {
  isLoaded(): Promise<boolean> {
    throw new Error('Method not implemented.');
  }

  /**
   *
   * @param {Page} page Puppeteer Page.
   * @param {CellType} cellType: Code or Markdown cell. Default value is Code cell.
   * @param {number} cellIndex Cell index. (first index is 1)
   */
  constructor(page: Page, private readonly cellType: CellType = CellType.Code, private cellIndex: number = 1) {
    super(page);
  }

  async findCell(cellIndx?: number): Promise<ElementHandle> {
    const iframe = await this.getIFrame();
    cellIndx = cellIndx || this.getCellIndex();
    const selector = this.cellSelector(cellIndx);
    return iframe.waitForSelector(`${selector} .CodeMirror-code`, { visible: true });
  }

  async getLastCell(): Promise<NotebookCell | null> {
    const elements = await this.findAllCells();
    if (elements.length === 0) return null;
    this.cellIndex = elements.length;
    return this;
  }

  async isSelected(cellIndx?: number): Promise<boolean> {
    const iframe = await this.getIFrame();
    cellIndx = cellIndx || this.getCellIndex();
    const selector = this.cellSelector(cellIndx);
    return iframe
      .waitForSelector(`${selector}.selected`, { visible: true })
      .then(() => {
        return true;
      })
      .catch(() => {
        return false;
      });
  }

  /**
   * Set focus to (select) a notebook cell input. Retry up to 3 times if focus fails.
   * @returns ElementHandle to code input if exists.
   */
  async focus(maxAttempts = 3): Promise<ElementHandle> {
    const clickAndCheck = async (): Promise<ElementHandle> => {
      maxAttempts--;
      const cell = await this.findCell();
      await cell.click({ delay: 100 });
      // Click in a notebook cell editor area enables cell Edit mode.
      // When a cell is in Edit mode, user can enter code to run.
      const iframe = await this.getIFrame();
      const [element] = await iframe.$$('body.notebook_app.edit_mode');
      const selected = await this.isSelected();
      if (element && selected) {
        return cell;
      }
      if (maxAttempts <= 0) {
        console.warn(`Notebook cell[${this.getCellIndex()}] is not in edit_mode or selected.`);
        return cell;
      }
      await this.page.waitForTimeout(2000); // Pause 2 seconds then retry
      return clickAndCheck();
    };

    return clickAndCheck();
  }

  /**
   * Returns cell run result. Result is either code output or error texts if code fails to run.
   * @param {number} timeOut The timeout time in milliseconds.
   */
  async waitForOutput(timeOut: number = 30 * 1000): Promise<string> {
    return Promise.race([this.getOutputText(timeOut), this.getOutputError(timeOut), this.getOutputStdError(timeOut)]);
  }

  /**
   * Returns code input texts.
   */
  async getInputText(): Promise<string> {
    const inputCell = await this.focus();
    const value = await getPropValue<string>(inputCell, 'innerText');
    await inputCell.dispose();
    return value.trim();
  }

  /**
   * Returns code output texts.
   * @param {number} timeOut The timeout time in milliseconds.
   */
  async getOutputText(timeOut?: number): Promise<string> {
    const outputCell = await this.findOutputElementHandle(timeOut);
    const value = await getPropValue<string>(outputCell, 'innerText');
    await outputCell.dispose();
    return value.trim();
  }

  /**
   * Gets output error texts.
   * @param {number} timeOut The timeout time in milliseconds.
   */
  async getOutputError(timeOut?: number): Promise<string> {
    const element = await this.findOutputErrorElementHandle(timeOut);
    const value = await getPropValue<string>(element, 'innerText');
    await element.dispose();
    console.error(`Run cell output error: \n${value}`);
    return value.trim();
  }

  async getOutputStdError(timeOut?: number): Promise<string> {
    const element = await this.findOutputStdErrorElementHandle(timeOut);
    const value = await getPropValue<string>(element, 'innerText');
    await element.dispose();
    console.error(`Run cell output error: \n${value}`);
    return value.trim();
  }

  /**
   * Find cell output_area element.
   * @param {number} timeOut The timeout in milliseconds.
   */
  async findOutputElementHandle(timeOut?: number): Promise<ElementHandle> {
    const selector = `${this.outputSelector(this.getCellIndex())}:not(.output_error):not(.output_stderr)`;
    const iframe = await this.getIFrame();
    await iframe.waitForSelector(selector, { visible: true, timeout: timeOut });
    const elements = await iframe.$$(selector);
    return elements[elements.length - 1];
  }

  /**
   * Find cell output_area error element.
   * @param {number} timeOut The timeout in milliseconds.
   */
  async findOutputErrorElementHandle(timeOut?: number): Promise<ElementHandle> {
    const selector = `${this.outputSelector(this.getCellIndex())}.output_error`;
    const iframe = await this.getIFrame();
    return iframe.waitForSelector(selector, { visible: true, timeout: timeOut });
  }

  async findOutputStdErrorElementHandle(timeOut?: number): Promise<ElementHandle> {
    const selector = `${this.outputSelector(this.getCellIndex())}.output_stderr`;
    const iframe = await this.getIFrame();
    return iframe.waitForSelector(selector, { visible: true, timeout: timeOut });
  }

  private async findAllCells(): Promise<ElementHandle[]> {
    const frame = await this.getIFrame();
    const selector = `${this.cellSelector()} .CodeMirror-code`;
    return frame.$$(selector);
  }

  private getCellIndex(): number {
    return this.cellIndex;
  }

  outputSelector(index?: number): string {
    return `${this.cellSelector(index)} .output_subarea`;
  }

  /**
   * Returns css selector for cell.
   * Cell contains both the code input and output cells.
   * @param {number} index Cell index. Returns selector for all cells if index is undefined.
   */
  private cellSelector(index?: number): string {
    const substr = `.cell${this.cellType}`;
    if (index === undefined) {
      return substr;
    }
    return `${substr}:nth-child(${index})`; // the index of the first child is 1 in :nth-child() selector
  }

  async waitForPropertyContains(cssSelector: string, propertyName: string, propertyValue: string): Promise<boolean> {
    const iframe = await this.getIFrame();
    const jsHandle = await iframe.waitForFunction(
      (css, prop, value) => {
        const element = document.querySelector(css);
        return element && element[prop].includes(value);
      },
      {},
      cssSelector,
      propertyName,
      propertyValue
    );
    return (await jsHandle.jsonValue()) as boolean;
  }
}
