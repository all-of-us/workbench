import {ElementHandle, Frame, Page} from 'puppeteer';
import {getPropValue} from 'utils/element-utils';

export enum CellType {
  // To append to css selector
  Code = '.code_cell',
  Markdown = '.text_cell',
  Any = '',
}

/**
 * Notebook Cell represents the root element that contains both the code input and output cells.
 */
export default class NotebookCell {

  private readonly page: Page;
  private iframe: Frame; // Jupyter notebook iframe
  private cellIndex: number; // 1-based cell index because notebook cell prompt index starts from 1.
  private readonly cellType: CellType; // Markdown or Code cell.

  /**
   *
   * @param {Page} page Puppeteer Page.
   * @param {CellType} cellType Cell type: Code or Markdown cell. Default value is Code cell.
   * @param {number} cellIndex 1-based cell index. Default value is 1.
   */
  constructor(page: Page, cellType: CellType = CellType.Code, cellIndex: number = 1) {
    this.page = page;
    this.cellType = cellType;
    this.cellIndex = cellIndex;
  }

  async getLastCell(): Promise<NotebookCell | null> {
    const elements = await this.findAllCells();
    if (elements.length === 0) return null;
    this.cellIndex = elements.length;
    return this;
  }

  /**
   * Set focus in notebook cell input.
   * @returns ElementHandle to code input if exists.
   */
  async focus(): Promise<ElementHandle> {
    if (this.getCellIndex() === undefined) {
      throw new Error('cellIndex is required.');
    }
    const frame = await this.getIFrame();
    const selector = `${this.cellSelector(this.getCellIndex())} .CodeMirror-code`;
    const cell = await frame.waitForSelector(selector, {visible: true});
    await cell.click({delay: 10}); // focus
    return cell;
  }

  /**
   * Returns cell run result. Result is either code output or error texts if code fails to run.
   * @param {number} timeOut The timeout time in milliseconds.
   */
  async waitForOutput(timeOut: number = 30 * 1000): Promise<string> {
    return Promise.race([
      this.getOutputText(timeOut),
      this.getOutputError(timeOut),
    ]);
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

  /**
   * Find cell output_area element.
   * @param {number} timeOut The timeout in milliseconds.
   */
  async findOutputElementHandle(timeOut?: number): Promise<ElementHandle> {
    const selector = `${this.outputSelector(this.getCellIndex())}:not(.output_error)`;
    const iframe = await this.getIFrame();
    await iframe.waitForSelector(selector, {visible: true, timeout: timeOut});
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
    return iframe.waitForSelector(selector, {visible: true, timeout: timeOut});
  }

  private async getIFrame(): Promise<Frame> {
    if (this.iframe === undefined) {
      const frame = await this.page.waitForSelector('iframe[src*="notebooks"]');
      this.iframe = await frame.contentFrame();
    }
    return this.iframe;
  }

  private async findAllCells(): Promise<ElementHandle[]> {
    const frame = await this.getIFrame();
    const selector = `${this.cellSelector()} .CodeMirror-code`;
    return frame.$$(selector);
  }

  private getCellIndex(): number {
    return this.cellIndex;
  }

  private outputSelector(index?: number): string {
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
    return `${substr}:nth-child(${index})`;
  }

  /**
   * Returns css selector for an unused (empty) cell.
   * @param {number} indx Cell index.
   */
  // @ts-ignore
  private emptyCellSelector(indx?: number): string {
    const substr = `.cell.unrendered${this.cellType}`;
    if (indx === undefined) {
      return substr;
    }
    return `${substr}:nth-child(${indx})`;
  }

}
