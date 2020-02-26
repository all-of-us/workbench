
/**
 * a BUTTON element with specified label.
 * @param label
 */
export function buttonXpath(label: string) {
  return `//*[@role='button' and contains(normalize-space(text()),'${label}')]`;
}

/**
 * a TEXTAREA element with specified label.
 * @param label
 */
export function textareaXpath(label: string) {
  return `${inputXpath(label, 'textarea')}`;
}

  /**
   * a textbox element with specified label.
   * @param label
   */
export function textboxXpath(label: string) {
  return `${inputXpath(label, 'text')}`;
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
export function checkboxXpath(label: string) {
  return `${inputXpath(label, 'checkbox')}`;
}

/**
 * a RADIOBUTTON element with specified label.
 * @param label
 */
export function radioButtonXpath(label: string) {
  return `${inputXpath(label, 'radio')}`;
}

export function inputXpath(label: string, inputType?: string) {
  if (!!inputType) {
    return `${textXpath(label)}/ancestor::node()[1]/input[@type=${inputType}] | /ancestor::node()[2]//input[@type=${inputType}]`;
  }
  // return all input nodes
  return `${textXpath(label)}/ancestor::node()[1]/input | /ancestor::node()[2]//input`;
}

/**
 * Texts or label. It can be partial or full string.
 * @param labelText
 */
export function textXpath(label: string) {
  return `//*[contains(normalize-space(text()),"${label}")]`;
}

/**
 * Clickable element with label.
 * @param label
 */
export function clickableXpath(label: string) {
  return `(//a | //*[@role='button'])[normalize-space()='${label}' or contains(@aria-label,'${label}')]`;
}
