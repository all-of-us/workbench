import { Page } from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';

const LabelAlias = {
  Save: 'Save'
};
export const PageTitle = 'User Profile Information Page';

export default class UserProfileInfo extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle), waitWhileLoading(this.page)]);
    return true;
  }

  async waitForSaveButton(isActive: boolean): Promise<Button> {
    const button = this.getSaveUserProfileButton();
    const isCursorEnabled = !(await button.isCursorNotAllowed());
    expect(isCursorEnabled).toBe<boolean>(isActive);
    return button;
  }

  getSaveUserProfileButton(): Button {
    return Button.findByName(this.page, { name: LabelAlias.Save });
  }
}
