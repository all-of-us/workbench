import TextOptions from './TextOptions';


/**
 * a SELECT element with specified label.
 * @param label
 */
export function selectXpath(textOptions?: TextOptions) {
  return `${textXpath(textOptions)}/ancestor::node()[2]//select`;
}

/**
 * a TEXTAREA element with specified label.
 * @param label
 */
export function textAreaXpath(textOptions?: TextOptions) {
  if (textOptions.ancestorNodeLevel === undefined) {
    textOptions.ancestorNodeLevel = 2;
  }
  return `${textXpath(textOptions)}/ancestor::node()[${textOptions.ancestorNodeLevel}]//textarea`;
}

  /**
   * a textbox element with specified label.
   * @param label
   */
export function textBoxXpath(textOptions?: TextOptions) {
  return `${inputXpath(textOptions, 'text')}`;
}

/**
 * a IMAGE element with specified label.
 * @param label
 */
export function imageXpath(label: string) {
  return `//*[normalize-space(text())='${label}']//*[@role='img']`
}

/**
 * a CHECKBOX element with specified label.
 * @param label
 */
export function checkBoxXpath(textOptions?: TextOptions) {
  return `${inputXpath(textOptions, 'checkbox')}`;
}

/**
 * a RADIOBUTTON element with specified label.
 * @param label
 */
export function radioButtonXpath(options?: TextOptions) {
  return `${inputXpath(options, 'radio')}`;
}

export function inputXpath(options?: TextOptions, inputType?: string) {
  if (options.ancestorNodeLevel === undefined) {
    options.ancestorNodeLevel = 1;
  }

  if (inputType !== undefined) {
    return `${textXpath(options)}/ancestor::node()[${options.ancestorNodeLevel}]//input[@type='${inputType}']`;
  }
  // return all input nodes
  return `${textXpath(options)}/ancestor::node()[${options.ancestorNodeLevel}]//input`;
}

/**
 * Texts or label. It can be partial or full string.
 * @param label
 */
export function textXpath(options?: TextOptions) {
  if (options.text) {
    return `//*[text()='${options.text}' or @placeholder='${options.text}']`;
  } else if (options.textContains) {
    return `//*[contains(text(),'${options.textContains}') or contains(@aria-label,'${options.textContains}') or contains(@placeholder,'${options.textContains}')]`;
  } else if (options.normalizeSpace) {
    return `//*[contains(normalize-space(),'${options.normalizeSpace}')]`;
  }
}

/**
 * Label. It can be partial or full string.
 * @param label
 */
export function labelXpath(options?: TextOptions) {
  if (options.text) {
    return `//label[text()='${options.text}']`;
  } else if (options.textContains) {
    return `//label[(contains(text(),'${options.textContains}') or contains(@aria-label,'${options.textContains}'))]`;
  } else if (options.normalizeSpace) {
    return `//label[contains(normalize-space(),'${options.normalizeSpace}')]`;
  }
}

/**
 * any [@role=button] element with specified label.
 * @param label
 */
export function buttonXpath(textOptions?: TextOptions) {
  if (textOptions.text) {
    return `(//button[text()='${textOptions.text}'] | //*[text()='${textOptions.text}' and @role='button'])`;
  } else if (textOptions.normalizeSpace) {
    return `(//button[contains(normalize-space(text()),'${textOptions.normalizeSpace}')] | //*[normalize-space()='${textOptions.normalizeSpace}' and @role='button'])`;
  } else if (textOptions.textContains) {
    return `(//button[contains(text(), '${textOptions.textContains}')] | //*[contains(text(),'${textOptions.textContains}') and @role='button'])`;
  }
}

/**
 * Clickable element with label. It can be a link or button.
 * @param label
 */
export function clickableXpath(label: string) {
  return `(//a | //span | //*[@role='button'])[normalize-space(text())='${label}' or contains(@aria-label,'${label}')]`;
}

/**
 * clr-icon element with specified label.
 * @param label:
 * @param shapeValue:
 */
export function iconXpath(label: string, shapeValue: string) {
  if (label === '') {
    return `//clr-icon[@shape='${shapeValue}'][*[@role='img']]`; // anywhere on page
  }
  // next to a label
  return `//*[normalize-space()='${label}']/ancestor::node()[1]//clr-icon[@shape='${shapeValue}'][*[@role='img']]`;
}
