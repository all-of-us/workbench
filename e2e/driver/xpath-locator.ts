
export class XPath {
  constructor(public selector: string) {}

  get pageFunc() {
    return (expression: string) =>
         document.evaluate(expression, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
            .singleNodeValue
  }
}