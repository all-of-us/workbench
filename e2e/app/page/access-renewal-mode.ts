import AuthenticatedPage from './authenticated-page';
import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import { assertNoNotification, DataTestIds, getNotificationText, Texts } from 'app/component/notification';

export const PageTitle = 'Access Renewal';

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

  async hasExpired(): Promise<boolean> {
    const notificationText = await getNotificationText(page, DataTestIds.AccessRenewal);
    return notificationText.includes(Texts.AccessExpired);
  }

  async hasNotExpired(): Promise<boolean> {
    await assertNoNotification(page, DataTestIds.AccessRenewal);
    return true;
  }

  getReviewProfileButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Review });
  }
}
