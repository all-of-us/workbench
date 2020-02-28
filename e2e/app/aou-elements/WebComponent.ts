import {Page} from 'puppeteer';
import Button from './Button';
import CheckBox from './CheckBox';
import RadioButton from './RadioButton';
import Select from './Select';
import Text from './Text';
import TextArea from './TextArea';
import TextBox from './TextBox';

export default class WebComponent {

  private readonly label: string;
  private readonly page: Page;

  constructor(aPage: Page, label: string) {
    this.page = aPage;
    this.label = label;
  }

  public async asCheckbox(): Promise<CheckBox> {
    const checkbox = new CheckBox(this.page);
    await checkbox.withLabel(this.label);
    return checkbox;
  }

  public async asTextbox(): Promise<TextBox> {
    const textbox = new TextBox(this.page);
    await textbox.withLabel(this.label);
    return textbox;
  }

  public async asTextArea(): Promise<TextArea> {
    const textarea = new TextArea(this.page);
    await textarea.withLabel(this.label);
    return textarea;
  }

  public async asRadioButton(): Promise<RadioButton> {
    const radio = new RadioButton(this.page);
    await radio.withLabel(this.label);
    return radio;
  }

  public async asButton(): Promise<Button> {
    const button = new Button(this.page);
    await button.withLabel(this.label);
    return button;
  }

  public async asText(): Promise<Text> {
    const txt = new Text(this.page);
    await txt.withLabel(this.label);
    return txt;
  }

  public async asSelect(): Promise<Select> {
    const select = new Select(this.page);
    await select.withLabel(this.label);
    return select;
  }

}
