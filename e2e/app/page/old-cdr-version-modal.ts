import {Page} from 'puppeteer';

import Button from 'app/element/button';
import Modal from 'app/component/modal';
import {waitWhileLoading} from 'utils/waits-utils';
import {ElementType} from 'app/xpath-options';
import {LinkText} from 'app/text-labels';
import Checkbox from 'app/element/checkbox';

const LabelAlias = {
    WILL_USE_OLD_CDR_VERSION: 'I will use this workspace to complete an existing study or replicate a previous study.',
    WILL_IDENTIFY_OLD_CDR_VERSION: 'In the workspace description below, I will identify which study I am continuing or replicating.',
}

const FIELD = {
    willUseCheckbox: {
        textOption: {name: LabelAlias.WILL_USE_OLD_CDR_VERSION, type: ElementType.Checkbox}
    },
    willIdentifyCheckbox: {
        textOption: {name: LabelAlias.WILL_IDENTIFY_OLD_CDR_VERSION, type: ElementType.Checkbox}
    },
    continueButton: {
        textOption: {name: LinkText.Continue}
    },
}

export default class OldCdrVersionModal extends Modal {

    constructor(page: Page, xpath?: string) {
        super(page, xpath);
    }

    async isLoaded(): Promise<boolean> {
      const xpath = '//*[@data-test-id="old-cdr-version-modal"]';
      await this.page.waitForXPath(xpath, {visible: true});
      await waitWhileLoading(this.page);
      return true;
    }

    async getWillUseCheckbox(): Promise<Checkbox> {
        return Checkbox.findByName(this.page, FIELD.willUseCheckbox.textOption, this);
    }

    async getWillIdentifyCheckbox(): Promise<Checkbox> {
        return Checkbox.findByName(this.page, FIELD.willIdentifyCheckbox.textOption, this);
    }

    async getContinueButton(): Promise<Button> {
        return Button.findByName(this.page, FIELD.continueButton.textOption, this);
    }

    /**
     * if the CDR Version is not the default, consent to the necessary restrictions in the modal which appears
     */
    async consentToOldCdrRestrictions() {
        await this.isLoaded()

        // can't continue yet - user has not yet consented
        const continueButton: Button = await this.getContinueButton();
        expect(await continueButton.isCursorNotAllowed()).toBe(true);

        const willUse: Checkbox = await this.getWillUseCheckbox();
        expect(await willUse.isChecked()).toBe(false)
        await willUse.check();

        const willIdentify: Checkbox = await this.getWillIdentifyCheckbox();
        expect(await willIdentify.isChecked()).toBe(false)
        await willIdentify.check();

        // can continue now
        await continueButton.waitUntilEnabled();
        await continueButton.click();
    }
}