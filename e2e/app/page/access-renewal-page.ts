import AuthenticatedPage from './authenticated-page';
import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';

export const PageTitle = 'Access Renewal';

export const LabelAlias = {
  AccessExpired: 'Researcher workbench access has expired.',
};

export default class AccessRenewalPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitForDocumentTitle(this.page, PageTitle);
    await waitWhileLoading(this.page);
    return true;
  }

  hasExpired(): Promise<boolean> {
    return this.containsText(LabelAlias.AccessExpired);
  }

  getReviewProfileButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Review });
  }
}
