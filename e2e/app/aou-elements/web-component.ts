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

export default class WebComponent {

  private readonly label: TextOptions;
  private readonly page: Page;

  constructor(aPage: Page, textOptions?: TextOptions) {
    this.page = aPage;
    this.label = textOptions;
  }

  async asCheckBox(): Promise<Checkbox> {
    const checkbox = new Checkbox(this.page);
    await checkbox.withLabel(this.label);
    return checkbox;
  }

  async asTextBox(): Promise<Textbox> {
    const textbox = new Textbox(this.page);
    await textbox.withLabel(this.label);
    return textbox;
  }

  async asTextArea(): Promise<Textarea> {
    const textarea = new Textarea(this.page);
    await textarea.withLabel(this.label);
    return textarea;
  }

  async asRadioButton(): Promise<RadioButton> {
    const radio = new RadioButton(this.page);
    await radio.withLabel(this.label);
    return radio;
  }

  async asButton(): Promise<Button> {
    const button = new Button(this.page);
    await button.withLabel(this.label);
    return button;
  }

  async asLabel(): Promise<Label> {
    const txt = new Label(this.page);
    await txt.withLabel(this.label);
    return txt;
  }

  async asSelect(): Promise<Select> {
    const select = new Select(this.page);
    await select.withLabel(this.label);
    return select;
  }

  async asLink(): Promise<Link> {
    const link = new Link(this.page);
    await link.withLabel(this.label.text);
    return link;
  }

}
