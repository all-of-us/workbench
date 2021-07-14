import { ElementHandle, Frame, Page } from 'puppeteer';
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

  async getLastCell(): Promise<NotebookCell | null> {
    const elements = await this.findAllCells();
    if (elements.length === 0) return null;
    this.cellIndex = elements.length;
    return this;
  }

  /**
   * Set focus to (select) a notebook cell input. Retry up to 3 times if focus fails.
   * @returns ElementHandle to code input if exists.
   */
  async focus(maxAttempts = 3): Promise<ElementHandle> {
    const clickInCell = async (iframe: Frame): Promise<ElementHandle> => {
      const selector = this.cellSelector(this.getCellIndex());
      const cell = await iframe.waitForSelector(`${selector} .CodeMirror-code`, { visible: true });
      await cell.click({ delay: 10 }); // focus
      return cell;
    };

    const clickAndCheck = async (iframe: Frame): Promise<ElementHandle> => {
      maxAttempts--;
      const cell = await clickInCell(iframe);
      const [element] = await iframe.$$('body.notebook_app.edit_mode');
      if (element) {
        return cell;
      }
      if (maxAttempts <= 0) {
        console.warn('Notebook body is not in edit_mode.');
        return cell;
      }
      await this.page.waitForTimeout(3000); // Pause 3 seconds then retry
      return clickAndCheck(iframe);
    };

    const frame = await this.getIFrame();
    return clickAndCheck(frame);
  }

  /**
   * Returns cell run result. Result is either code output or error texts if code fails to run.
   * @param {number} timeOut The timeout time in milliseconds.
   */
  async waitForOutput(timeOut: number = 30 * 1000): Promise<string> {
    return Promise.race([this.getOutputText(timeOut), this.getOutputError(timeOut)]);
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
