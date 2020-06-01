import Container from 'app/container';
import {ElementType, XPathOptions} from 'app/xpath-options';

export function iframeXpath(label: string) {
  return `//body[@id='body']//*[contains(@aria-label, '${label}')]//iframe`;
}

export function xPathOptionToXpath(xOpts: XPathOptions, container?: Container): string {

  const  { type, name, containsText, normalizeSpace, ancestorLevel = 1, iconShape} = xOpts;

  // optional function parameters check
  if (type === 'icon') {
    if (iconShape === undefined) {
      throw new Error(`Incorrect XPathOptions configuration for Icon: set "iconShape" value.`);
    }
  } else {
    if (name === undefined && normalizeSpace === undefined && containsText === undefined) {
      throw new Error(`Incorrect XPathOptions configuration for label name: 
      Cannot find a text parameter: set "name", "containsText" or "normalizeSpace" value.`);
    }
  }

  let str;
  if (name !== undefined) {
    str = `text()="${name}" or @aria-label="${name}" or @placeholder="${name}"`;
  } else if (containsText !== undefined) {
    str = `contains(text(),"${containsText}") or contains(@aria-label,"${containsText}") or contains(@placeholder,"${containsText}")`;
  } else if (normalizeSpace !== undefined) {
    str = `contains(normalize-space(),"${normalizeSpace}")`;
  }

  const containerXpath = (container === undefined) ? '' : container.getXpath();
  const textExpr = `(${containerXpath}//label | ${containerXpath}//*)[${str}]`;
  const nodeLevel = `ancestor::node()[${ancestorLevel}]`;

  let selector;
  switch (type) {
  case ElementType.Button:
    selector = `(${containerXpath}//button | ${containerXpath}//*[@role="button"])[${str}]`;
    break;
  case ElementType.Icon: // clickable icon
    const tag = (iconShape === undefined) ? '*' : `clr-icon[@shape="${iconShape}"]`;
    if (name === undefined && containsText === undefined && normalizeSpace === undefined) {
        // not tied to a specific label
      selector = `//${tag}[*[@role="img"]]`;
    } else {
      selector = `//*[${textExpr}]/${nodeLevel}//${tag}[*[@role="img"]]`;
    }
    break;
  case ElementType.Checkbox:
    selector = `${textExpr}/${nodeLevel}/input[@type="${type}"]`;
    break;
  case ElementType.RadioButton:
  case ElementType.Textbox:
    selector = `${textExpr}/${nodeLevel}//input[@type="${type}"]`;
    break;
  case ElementType.Link:
    selector = `(${containerXpath}//a | ${containerXpath}//span | ${containerXpath}//*[@role='button'])[${str}]`;
    break;
  case ElementType.Textarea:
  case ElementType.Select:
    selector = `${textExpr}/${nodeLevel}//${type}`;
    break;
  case ElementType.Dropdown:
    selector = `${textExpr}/${nodeLevel}//*[contains(concat(" ", normalize-space(@class), " ")," p-dropdown ")]`;
    break;
  default:
    console.debug(`Implement unhandled type: ${type}. 
    XPathOptions configuration: 
      "type": ${type}, 
      "name": ${name}, 
      "containsText": ${containsText}, 
      "normalizeSpace: ${normalizeSpace}, 
      "ancestorLevel": ${ancestorLevel}, 
      "iconShape": ${iconShape}`);
    throw new Error(`Implement unhandled type: ${type}`);
  }

  return this.xpath = selector;
}
