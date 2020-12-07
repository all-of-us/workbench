import {Page} from 'puppeteer';

import ClrIcon from 'app/element/clr-icon-link';
import Button from 'app/element/button';
import {config} from 'resources/workbench-config';
import Modal from 'app/component/modal';
import {waitWhileLoading} from 'utils/waits-utils';

export default class CdrVersionUpgradeModal extends Modal {

    constructor(page: Page, xpath?: string) {
        super(page, xpath);
    }

    async isLoaded(): Promise<boolean> {
        const xpath = '//*[@data-test-id="cdr-version-upgrade-modal"]';
        await Promise.all([
            this.page.waitForXPath(xpath, {visible: true}),
            waitWhileLoading(this.page),
        ]);
        return true;
    }

    async getCancelButton(): Promise<ClrIcon> {
        return ClrIcon.findByName(this.page, {iconShape: 'times'}, this);
    }

    async getUpgradeButton(): Promise<Button> {
        return Button.findByName(this.page, {normalizeSpace: `Try ${config.defaultCdrVersionName}`}, this);
    }
}