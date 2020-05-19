import Container from './container';
import TextOptions from './text-options';

function textXpathHelper(opts: TextOptions): string {
  let findText;
  if (opts.text) {
    // matching text exactly
    findText = opts.text;
    return `text()="${findText}" or @aria-label="${findText}" or @placeholder="${findText}"`;
  } else if (opts.contains) {
    findText = opts.contains;
    return `contains(text(), "${findText}") or contains(@aria-label, "${findText}") or contains(@placeholder, "${findText}")`;
  } else if (opts.normalizeSpace) {
    // text displayed with hidden line breaks
    findText = opts.normalizeSpace;
    return `contains(normalize-space(), "${findText}")`;
  }
}

/**
 * Label. It can be partial or full string.
 * @param {TextOptions} opts
 */
export function labelXpath(opts: TextOptions, container?: Container): string {
  const containerXpath = (container === undefined) ? '' : container.getXpath();
  return `(${containerXpath}//label | ${containerXpath}//*)[${textXpathHelper(opts)}]`;
}

/**
 * Find an Button matching label.
 * @param {TextOptions} opts
 * @param {Container} container
 */
export function buttonXpath(opts: TextOptions, container?: Container): string {
  const buttonName = opts.text || opts.contains || opts.normalizeSpace;
  const containerXpath = (container === undefined) ? '' : container.getXpath();
  return `(${containerXpath}//button | ${containerXpath}//*[@role="button"])[contains(normalize-space(), "${buttonName}") 
    or contains(normalize-space(@placeholder), "${buttonName}")]`;
}

export function inputXpath(opts: TextOptions, container?: Container): string {
  const nodeLevel = `ancestor::node()[${opts.ancestorNodeLevel}]`;
  if (opts.inputType === undefined) {
    // xpath that finds all input elements
    return `${labelXpath(opts, container)}/${nodeLevel}//input`;
  }
  if (opts.inputType === 'checkbox') {
    return `${labelXpath(opts, container)}/${nodeLevel}/input[@type="${opts.inputType}"]`;
  }
  return `${labelXpath(opts, container)}/${nodeLevel}//input[@type="${opts.inputType}"]`;
}

/**
 * a IMAGE element with specified label.
 * @param label
 */
export function imageXpath(label: string): string {
  return `//*[normalize-space(text())="${label}"]//*[@role="img"]`
}

/**
 * Clickable element with label. It can be a link or button.
 * @param label
 */
export function clickableXpath(label: string, container?: Container): string {
  const containerXpath = (container === undefined) ? '' : container.getXpath();
  return `(${containerXpath}//a | ${containerXpath}//span | ${containerXpath}//*[@role="button"])[normalize-space(text())="${label}" or contains(@aria-label,"${label}")]`;
}

/**
 * clr-icon element with specified label.
 * @param label:
 * @param shapeValue:
 */
export function clrIconXpath(opts: TextOptions, shapeValue: string, container?: Container): string {
  const containerXpath = (container === undefined) ? '' : container.getXpath();
  if (Object.keys(opts).length === 0) {
    return `${containerXpath}//clr-icon[@shape="${shapeValue}"][*[@role="img"]]`; // does not care about text label
  }
  // near a text label
  const nodeLevel = opts.ancestorNodeLevel || 1;
  return `${containerXpath}//*[${textXpathHelper(opts)}]/ancestor::node()[${nodeLevel}]//clr-icon[@shape="${shapeValue}"][*[@role="img"]]`;
}

export function iframeXpath(label: string) {
  return `//body[@id='body']//*[contains(@aria-label, '${label}')]//iframe`
}