import Modal from './modal';
import { Page } from 'puppeteer';
import { waitForText } from 'utils/waits-utils';
import { LinkText } from 'app/text-labels';

const modalTitle = 'Please review Research Purpose for Workspace';

export default class WorkspaceReviewResearchPurposeModal extends Modal {
  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
  }

  async isLoaded(): Promise<boolean> {
    return await waitForText(this.page, modalTitle, { container: this })
      .then(() => {
        return true;
      })
      .catch(() => {
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
