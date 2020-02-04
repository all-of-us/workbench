
export default class XPath {
  constructor(public selector: string) {
  }

  get build(): any {
    return (expression: string) =>
         document.evaluate(expression, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)
            .singleNodeValue
  }

}