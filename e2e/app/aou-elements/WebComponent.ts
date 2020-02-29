import {Page} from 'puppeteer';
import Button from './Button';
import CheckBox from './CheckBox';
import RadioButton from './RadioButton';
import Select from './Select';
import Text from './Text';
import TextArea from './TextArea';
import TextBox from './TextBox';

export default class WebComponent {

  private readonly name: string;
  private readonly page: Page;

  constructor(aPage: Page, aName: string) {
    this.page = aPage;
    this.name = aName;
  }

  public async asCheckbox(): Promise<CheckBox> {
    const checkbox = new CheckBox(this.page);
    await checkbox.withLabel(this.name);
    return checkbox;
  }

  public async asTextbox(): Promise<TextBox> {
    const textbox = new TextBox(this.page);
    await textbox.withLabel(this.name);
    return textbox;
  }

  public async asTextArea(): Promise<TextArea> {
    const textarea = new TextArea(this.page);
    await textarea.withLabel(this.name);
    return textarea;
  }

  public async asRadioButton(): Promise<RadioButton> {
    const radio = new RadioButton(this.page);
    await radio.withLabel(this.name);
    return radio;
  }

  public async asButton(): Promise<Button> {
    const button = new Button(this.page);
    await button.withLabel(this.name);
    return button;
  }

  public async asText(): Promise<Text> {
    const txt = new Text(this.page);
    await txt.withLabel(this.name);
    return txt;
  }

  public async asSelect(): Promise<Select> {
    const select = new Select(this.page);
    await select.withLabel(this.name);
    return select;
  }

}
