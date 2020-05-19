/**
 * The Container class is used for finding elements in a Dialog or Modal window.
 * Modal and Dialog classes extends Container.
 */
export default class Container {

  protected readonly xpath: string;

  constructor(selector: {xpath?: string, testId?: string}) {
    if (selector.testId !== undefined) {
      this.xpath = this.testIdToXPath(selector.testId);
    } else if (selector.xpath !== undefined) {
      this.xpath = selector.xpath;
    } else {
      throw new Error('Require selector xpath or testId.');
    }
  }

  testIdToXPath(testId: string): string {
    return `//*[@data-test-id="${testId}"]`;
  }

  getXpath(): string {
    return this.xpath;
  }

}
