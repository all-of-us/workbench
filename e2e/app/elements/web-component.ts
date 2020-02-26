import {Page} from 'puppeteer';
import Button from './button';
import Checkbox from './checkbox';
import RadioButton from './radiobutton';
import Text from './text';
import TextArea from './textarea';
import Textbox from './textbox';

export default class WebComponent {

  private readonly label: string;
  private readonly page: Page;

  constructor(aPage: Page, label: string) {
    this.page = aPage;
    this.label = label;
  }

  public asCheckbox(): Checkbox {
    return new Checkbox(this.page, this.label);
  }

  public asTextbox(): Textbox {
    return new Textbox(this.page, this.label);
  }

  public asTextArea(): TextArea {
    return new TextArea(this.page, this.label);
  }

  public asRadioButton(): RadioButton {
    return new RadioButton(this.page, this.label);
  }

  public asButton(): Button {
    return new Button(this.page, this.label);
  }

  public asText(): Text {
    return new Text(this.page, this.label);
  }

}
