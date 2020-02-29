/**
 * a TEXTAREA element with specified label.
 * @param name
 */
export function selectXpath(name: string) {
  return `${textXpath(name)}/ancestor::node()[2]//select`;
}

/**
 * a @role=button element with specified label.
 * @param name
 */
export function buttonXpath(name: string) {
  return `//*[(normalize-space(text())='${name}' or normalize-space(.)='${name}') and @role='button']`;
}

/**
 * a TEXTAREA element with specified label.
 * @param name
 */
export function textareaXpath(name: string) {
  return `${textXpath(name)}/ancestor::node()[2]//textarea`;
}

  /**
   * a textbox element with specified label.
   * @param name
   */
export function textboxXpath(name: string) {
  return `${inputXpath(name, 'text')}`;
}

/**
 * a IMAGE element with specified label.
 * @param name
 */
export function imageXpath(name: string) {
  return `//*[normalize-space(text())='${name}']//*[@role='img']`
}

/**
 * a CHECKBOX element with specified label.
 * @param name
 */
export function checkboxXpath(name: string) {
  return `${inputXpath(name, 'checkbox')}`;
}

/**
 * a RADIOBUTTON element with specified label.
 * @param name
 */
export function radioButtonXpath(name: string) {
  return `${inputXpath(name, 'radio')}`;
}

export function inputXpath(name: string, inputType?: string) {
  if (inputType !== undefined) {
    return `${textXpath(name)}/ancestor::node()[2]//input[@type='${inputType}']`;
  }
  // return all input nodes
  return `${textXpath(name)}/ancestor::node()[2]//input`;
}

/**
 * Texts or label. It can be partial or full string.
 * @param name
 */
export function textXpath(name: string) {
  return `//*[contains(normalize-space(text()),'${name}')  or @placeholder='${name}']`;
}

/**
 * Finds element that match xpath selector: //clr-icon[@shape="plus-circle"]
 * @param name
 */
export function plusCircleIconXpath(name: string) {
  return `//*[*[normalize-space(text())='${name}' or normalize-space(.)='${name}']]//clr-icon[@shape='plus-circle']/*[@role='img']`;
}

/**
 * Clickable element with label.
 * @param name
 */
export function clickableXpath(name: string) {
  return `(//a | //span | //*[@role='button'])[normalize-space()='${name}' or contains(@aria-label,'${name}')]`;
}

/**
 * A clr-icon element with specified label.
 * @param name
 */
export function clrIconXpath(name: string) {
  return `${textXpath(name)}/ancestor::node()[1]//clr-icon`;
}

