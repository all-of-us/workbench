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

  asCheckBox(): Checkbox {
    return Checkbox.findByName(this.page, this.xpathOptions);
  }

  asTextBox(): Textbox {
    return Textbox.findByName(this.page, this.xpathOptions);
  }

  asTextArea(): Textarea {
    return Textarea.findByName(this.page, this.xpathOptions);
  }

  asRadioButton(): RadioButton {
    return RadioButton.findByName(this.page, this.xpathOptions);
  }

  asButton(): Button {
    return Button.findByName(this.page, this.xpathOptions);
  }

  asSelect(): Select {
    return Select.findByName(this.page, this.xpathOptions);
  }

  asLink(): Link {
    return Link.findByName(this.page, this.xpathOptions);
  }
}
