import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import { LinkText } from 'app/text-labels';

const modalTitle = 'Please review Research Purpose for Workspace';

export default class WorkspaceReviewResearchPurposeModal extends Modal {
  constructor(page: Page, xpath?: string) {
    super(page, xpath);
  }

  async isLoaded(): Promise<boolean> {
    return await waitForText(this.page, modalTitle, { xpath: this.getXpath() }).catch(() => {
      return false;
    });
  }

  async clickReviewNowButton(): Promise<void> {
    await this.clickButton(LinkText.ReviewNow);
  }

  async clickReviewLaterButton(): Promise<void> {
    await this.clickButton(LinkText.ReviewLater);
  }
}
