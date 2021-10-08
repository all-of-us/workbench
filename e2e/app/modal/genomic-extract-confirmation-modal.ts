import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';

const modalTitle = 'Would you like to extract genomic variant data as VCF files?';

export default class GenomicsVariantExtractConfirmationModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    await waitForText(this.page, modalTitle, { container: this });
    await waitWhileLoading(this.page);
    return true;
  }

  getContinueButton(): Button {
    return Button.findByName(this.page, { name: LinkText.ExtractAndContinue }, this);
  }
}
