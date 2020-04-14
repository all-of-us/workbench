import TextOptions from './text-options';

function textXpathHelper(opts: TextOptions) {
  let findText;
  if (opts.text) {
    findText = opts.text;
    return `text()='${findText}' or @aria-label='${findText}' or @placeholder='${findText}'`;
  } else if (opts.textContains) {
    findText = opts.textContains;
    return `contains(text(), '${findText}') or contains(@aria-label, '${findText}') or contains(@placeholder, '${findText}')`;
  } else if (opts.normalizeSpace) {
    findText = opts.normalizeSpace;
    return `contains(normalize-space(), '${findText}')`;
  }
}

/**
 * Label. It can be partial or full string.
 * @param label
 */
export function labelXpath(opts: TextOptions) {
  return `(//label | //*)[${textXpathHelper(opts)}]`;
}

/**
 * any [@role=button] element with specified label.
 * @param label
 */
export function buttonXpath(opts: TextOptions) {
  const role = `@role='button'`;
  const txt = textXpathHelper(opts);
  return `(//button[${txt}] | //*[${txt} and ${role}])`;
}

export function inputXpath(opts: TextOptions) {
  const numSlashes = opts.inputType === 'checkbox' ? '/' : '//';
  const nodeLevel = `ancestor::node()[${opts.ancestorNodeLevel}]`;
  if (opts.inputType !== undefined) {
    return `${labelXpath(opts)}/${nodeLevel}${numSlashes}input[@type='${opts.inputType}']`;
  }
  // return all input nodes
  return `${labelXpath(opts)}/${nodeLevel}//input`;
}

/**
 * a IMAGE element with specified label.
 * @param label
 */
export function imageXpath(label: string) {
  return `//*[normalize-space(text())='${label}']//*[@role='img']`
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
export function clrIconXpath(opts: TextOptions, shapeValue: string) {
  if (Object.keys(opts).length === 0) {
    return `//clr-icon[@shape='${shapeValue}'][*[@role='img']]`; // anywhere on page
  }
  // next to a label
  const nodeLevel = opts.ancestorNodeLevel || 1;
  return `//*[${textXpathHelper(opts)}]/ancestor::node()[${nodeLevel}]//clr-icon[@shape='${shapeValue}'][*[@role='img']]`;
}
