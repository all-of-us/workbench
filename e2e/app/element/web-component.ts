import { Page } from 'puppeteer';
import { XPathOptions } from 'app/xpath-options';
import Button from './button';
import Checkbox from './checkbox';
import Link from './link';
import RadioButton from './radiobutton';
import Select from './select';
import Textarea from './textarea';
import Textbox from './textbox';

/**
 * WebComponent represents a AoU UI component visible in DOM.
 * For example, on workspace-edit page, a single label is associated with a checkbox and texbox or textarea.
 * If you create an instance of WebComponent, you do not need to create an instances of Checkbox and Textbox oTextarea separately.
 */
export default class WebComponent {
  constructor(private readonly page: Page, private readonly xpathOptions: XPathOptions) {}

  async asCheckBox(): Promise<Checkbox> {
    return await Checkbox.findByName(this.page, this.xpathOptions);
  }

  async asTextBox(): Promise<Textbox> {
    return await Textbox.findByName(this.page, this.xpathOptions);
  }

  async asTextArea(): Promise<Textarea> {
    return await Textarea.findByName(this.page, this.xpathOptions);
  }

  async asRadioButton(): Promise<RadioButton> {
    return await RadioButton.findByName(this.page, this.xpathOptions);
  }

  async asButton(): Promise<Button> {
    return await Button.findByName(this.page, this.xpathOptions);
  }

  async asSelect(): Promise<Select> {
    return await Select.findByName(this.page, this.xpathOptions);
  }

  async asLink(): Promise<Link> {
    return await Link.findByName(this.page, this.xpathOptions);
  }
}
