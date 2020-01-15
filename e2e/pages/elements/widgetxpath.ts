
  /**
   * Clickable element with label.
   * @param label
   */
export function clickable(label: string) {
  return `(//a | //*[@role='button'])[normalize-space()='${label}' or contains(@aria-label,'${label}')]`;
}

  /**
   * a BUTTON element with label.
   * @param label
   */
export function button(label: string) {
  return `//*[@role='button' and normalize-space(text())='${label}']`;
}

  /**
   * a TEXTAREA element with label.
   * @param label
   */
export function textarea(label: string) {
  return `//*[normalize-space(text())='${label}']/ancestor::*/textarea`;
      // return `//*[..//*[contains(normalize-space(text()),'${label}')]]/textarea`;
}

  /**
   * a text INPUT element with label.
   * @param label
   */
export function textInput(label: string) {
  return `//*[normalize-space(text())='${label}']/ancestor::*/input[@type='text']`;
      // return `//*[./*[normalize-space(text())='${label}']]/input[@type='text']`;
}

  /**
   * a IMAGE element with label.
   * @param label
   */
export function image(label: string) {
  return `//*[normalize-space(text())='${label}']//*[@role='img']`
      // return `//*[./*[normalize-space(text())='${label}']]//*[@role='img']`;
}

  /**
   * a CHECKBOX element with label.
   * @param label
   */
export function checkbox(label: string) {
  return `//*[contains(normalize-space(.),'${label}')]/ancestor::*/input[@type='checkbox']`;
}

  /**
   * Visible text element.
   * @param text
   */
export function textString(text: string) {
  return `//*[contains(normalize-space(text()),'${text}')]`;
}
