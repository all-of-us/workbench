import { Page } from 'puppeteer';
import Button from 'app/element/button';
import { ElementType } from 'app/xpath-options';
import { LinkText } from 'app/text-labels';
import Checkbox from 'app/element/checkbox';
import { waitForText } from 'utils/waits-utils';
import Modal from './modal';
import expect from 'expect';

const LabelAlias = {
  WILL_USE_OLD_CDR_VERSION: 'I will use this workspace to complete an existing study or replicate a previous study.',
  WILL_IDENTIFY_OLD_CDR_VERSION:
    'In the workspace description below, I will identify which study I am continuing or replicating.'
};

const FIELD = {
  willUseCheckbox: {
    textOption: { name: LabelAlias.WILL_USE_OLD_CDR_VERSION, type: ElementType.Checkbox }
  },
  willIdentifyCheckbox: {
    textOption: { name: LabelAlias.WILL_IDENTIFY_OLD_CDR_VERSION, type: ElementType.Checkbox }
  },
  continueButton: {
    textOption: { name: LinkText.Continue }
  },
  cancelButton: {
    textOption: { name: LinkText.Cancel }
  }
};

export default class OldCdrVersionModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    const xpath = '//*[@data-test-id="old-cdr-version-modal"]';
    await this.page.waitForXPath(xpath, { visible: true });
    const modalTitle = 'You have selected an older version of the dataset';
    await waitForText(this.page, modalTitle, { xpath: this.getXpath() });
    return true;
  }

  getWillUseCheckbox(): Checkbox {
    return Checkbox.findByName(this.page, FIELD.willUseCheckbox.textOption, this);
  }

  getWillIdentifyCheckbox(): Checkbox {
    return Checkbox.findByName(this.page, FIELD.willIdentifyCheckbox.textOption, this);
  }

  getContinueButton(): Button {
    return Button.findByName(this.page, FIELD.continueButton.textOption, this);
  }

  getCancelButton(): Button {
    return Button.findByName(this.page, FIELD.cancelButton.textOption, this);
  }

  /**
   * if the CDR Version is not the default, consent to the necessary restrictions in the modal which appears
   */
  async consentToOldCdrRestrictions(): Promise<void> {
    const modalTitle = await this.getTitle();
    expect(modalTitle).toMatch('You have selected an older version of the dataset.');

    // can't continue yet - user has not yet consented
    const continueButton: Button = this.getContinueButton();
    expect(await continueButton.isCursorNotAllowed()).toBe(true);

    const willUse: Checkbox = this.getWillUseCheckbox();
    expect(await willUse.isChecked()).toBe(false);
    await willUse.check();

    const willIdentify: Checkbox = this.getWillIdentifyCheckbox();
    expect(await willIdentify.isChecked()).toBe(false);
    await willIdentify.check();

    // can continue now
    await continueButton.waitUntilEnabled();
    await continueButton.click();
    await this.waitUntilClose();
  }
}
