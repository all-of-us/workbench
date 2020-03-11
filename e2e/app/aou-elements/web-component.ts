import {Page} from 'puppeteer';
import Button from './button';
import Checkbox from './checkbox';
import Label from './label';
import Link from './link';
import RadioButton from './radiobutton';
import Select from './select';
import TextOptions from './text-options';
import Textarea from './textarea';
import Textbox from './textbox';

/**
 * WebComponent represents a AoU UI component visible in DOM.
 * For example, on workspace-edit page, a single label is associated with a checkbox and texbox or textarea.
 * If you create an instance of WebComponent, you do not need to create an instances of Checkbox and Textbox oTextarea separately.
 */
export default class WebComponent {

  constructor(private readonly page: Page, private readonly labelTextOptions?: TextOptions) {
    this.page = page;
    this.labelTextOptions = labelTextOptions || undefined;
  }

  async asCheckBox(): Promise<Checkbox> {
    const checkbox = new Checkbox(this.page);
    await checkbox.withLabel(this.labelTextOptions);
    return checkbox;
  }

  async asTextBox(): Promise<Textbox> {
    const textbox = new Textbox(this.page);
    await textbox.withLabel(this.labelTextOptions);
    return textbox;
  }

  async asTextArea(): Promise<Textarea> {
    const textarea = new Textarea(this.page);
    await textarea.withLabel(this.labelTextOptions);
    return textarea;
  }

  async asRadioButton(): Promise<RadioButton> {
    const radio = new RadioButton(this.page);
    await radio.withLabel(this.labelTextOptions);
    return radio;
  }

  async asButton(): Promise<Button> {
    const button = new Button(this.page);
    await button.withLabel(this.labelTextOptions);
    return button;
  }

  async asLabel(): Promise<Label> {
    const txt = new Label(this.page);
    await txt.withLabel(this.labelTextOptions);
    return txt;
  }

  async asSelect(): Promise<Select> {
    const select = new Select(this.page);
    await select.withLabel(this.labelTextOptions);
    return select;
  }

  async asLink(): Promise<Link> {
    const link = new Link(this.page);
    await link.withLabel(this.labelTextOptions.text);
    return link;
  }

}
