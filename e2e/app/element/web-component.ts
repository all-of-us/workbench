import {Page} from 'puppeteer';
import Button from './button';
import Checkbox from './checkbox';
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

  constructor(private readonly page: Page, private readonly labelTextOptions: TextOptions) {

  }

  async asCheckBox(): Promise<Checkbox> {
    return await Checkbox.forLabel({puppeteerPage: this.page}, this.labelTextOptions);
  }

  async asTextBox(): Promise<Textbox> {
    return await Textbox.forLabel({puppeteerPage: this.page}, this.labelTextOptions);
  }

  async asTextArea(): Promise<Textarea> {
    return await Textarea.forLabel({puppeteerPage: this.page}, this.labelTextOptions);
  }

  async asRadioButton(): Promise<RadioButton> {
    return await RadioButton.forLabel({puppeteerPage: this.page}, this.labelTextOptions);
  }

  async asButton(): Promise<Button> {
    return await Button.forLabel({puppeteerPage: this.page}, this.labelTextOptions);
  }

  async asSelect(): Promise<Select> {
    return await Select.forLabel({puppeteerPage: this.page}, this.labelTextOptions);
  }

  async asLink(): Promise<Link> {
    return await Link.forLabel({puppeteerPage: this.page}, this.labelTextOptions.text);
  }

}
