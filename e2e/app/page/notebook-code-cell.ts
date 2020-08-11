import {ElementHandle, Frame} from 'puppeteer';
import {getPropValue} from 'utils/element-utils';

// CSS selectors
export enum Selector {
  codeCell = '.cell.code_cell',
}

export default class CodeCell {

  constructor(private readonly page: Frame) {}

  async selectCell(index: number = 1, code?: string): Promise<void> {
    const cell = await this.findCellInput(index);
    await cell.click({delay: 20}); // focus
    if (code !== undefined) await cell.type(code);
    console.log(`code cell [${index}]: \n ${code}`);
    await cell.dispose();
  }

  /**
   * The Output area can contain multiple outputs:
   * - Standard output or Rich output (an HTML table and an image)
   * - Error output
   * @param {number} index Code cell index.
   * @param {number} timeOut
   */
  async findCellOutput(index: number, timeOut?: number): Promise<string> {
    const outputText = await Promise.race([
      this.getOutputTexts(index, timeOut),
      this.getOutputError(index, timeOut),
    ]);
    console.log(`Code cell [${index}] output texts: \n ${outputText}`);
    return outputText;
  }

  /**
   * Find code cell input textfield. The index of the first cell is 1.
   * Warning: This is not the number shown in cell_prompt: "In [1]" or "Out [1]".
   * @param {number} index Cell index.
   */
  async findCellInput(index: number): Promise<ElementHandle> {
    const selector = `${this.getCellSelector(index)} .CodeMirror-code`;
    return this.page.waitForSelector(selector, {visible: true});
  }

  /**
   * Find the last cell. Cell element that contains both the input or output.
   */
  async findLastCell(): Promise<ElementHandle> {
    const cells = await this.findAllCells();
    const cellSize = cells.length;
    return cells[cellSize - 1];
  }

  /**
   * Find all visible cells.
   */
  async findAllCells(): Promise<ElementHandle[]> {
    await this.page.waitForSelector(this.getCellSelector(), {visible: true});
    return this.page.$$(this.getCellSelector());
  }

  /**
   * Gets cell output. Output is asynchronous.
   * @param {number} index Code cell index.
   */
  private async findOutputArea(index: number, timeOut?: number): Promise<ElementHandle> {
    const selector = `${this.getCellSelector(index)} .output_subarea:not(.output_error)`;
    return this.page.waitForSelector(selector, {visible: true, timeout: timeOut});
  }

  /**
   * Gets cell output error. Output is asynchronous.
   * @param {number} index Code cell index.
   */
  private async findOutputAreaError(index: number, timeOut: number): Promise<ElementHandle> {
    const selector = `${this.getCellSelector(index)} .output_subarea.output_error`;
    return this.page.waitForSelector(selector, {visible: true, timeout: timeOut});
  }

  /**
   * Gets code cell output text contents.
   * @param {number} index Cell index.
   */
  private async getOutputTexts(index: number, timeOut: number): Promise<string> {
    const outputElement = await this.findOutputArea(index, timeOut);
    const value = await getPropValue<string>(outputElement, 'innerHTML');
    await outputElement.dispose();
    return value;
  }

  /**
   * Gets code cell output error.
   * @param {number} index Cell index.
   */
  private async getOutputError(index: number, timeOut: number): Promise<string> {
    const outputElement = await this.findOutputAreaError(index, timeOut);
    const value = await getPropValue<string>(outputElement, 'innerHTML');
    await outputElement.dispose();
    return value;
  }

  // The index of the first cell is 1.
  private getCellSelector(index?: number): string {
    if (index === undefined) {
      return Selector.codeCell;
    }
    return `${Selector.codeCell}:nth-child(${index})`;
  }

}
