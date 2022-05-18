import AuthenticatedPage from './authenticated-page';
import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';

export const PageTitle = 'Access Renewal';

export const LabelAlias = {
  AccessExpired: 'Researcher workbench access has expired.'
};

// concerns the Data Access Requirements page in Access Renewal Mode
// which was formerly a separate page
export default class AccessRenewalMode extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitForDocumentTitle(this.page, PageTitle);
    await waitWhileLoading(this.page);
    return true;
  }

  // not a valid check as of 18 May 2022
  // async hasExpired(): Promise<boolean> {
  //   return await this.containsText(LabelAlias.AccessExpired);
  // }

  getReviewProfileButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Review });
  }
}
