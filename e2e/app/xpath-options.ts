export interface XPathOptions {
  type?: ElementType;
  name?: string;
  containsText?: string;
  normalizeSpace?: string;
  ancestorLevel?: number;
  iconShape?: string;
  startsWith?: string;
  dataTestId?: string;
  id?: string;
}

export enum ElementType {
  Button = 'button',
  Icon = 'icon',
  Checkbox = 'checkbox',
  RadioButton = 'radio',
  Textbox = 'text',
  Textarea = 'textarea',
  Link = 'link',
  Select = 'select',
  Dropdown = 'dropdown',
  Tab = 'Tab',
  Number = 'number'
}
