import {Page} from 'puppeteer';
import Textbox from 'app/element/textbox';
import AuthenticatedPage from 'app/page/authenticated-page';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle, waitForUrl} from 'utils/waits-utils';

export const PageTitle = 'Profile';

export const LabelAlias = {
  FirstName: 'First Name',
  LastName: 'Last Name',
  ContactEmail: 'Contact Email',
  CurrentPosition: 'Your Current Position',
  Organization: 'Your Organization',
  CurrentResearchWork: 'Current Research Work',
  AboutYou: 'About You',
  Institution: 'Institution',
  Role: 'Role',
  DiscardChanges: 'Discard Changes',
  SaveProfile: 'Save Profile',
};

export default class ProfilePage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForUrl(this.page, '/profile'),
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (err) {
      console.log(`ProfilePage isLoaded() encountered ${err}`);
      return false;
    }
  }

  async getFirstName(): Promise<Textbox> {
    return await Textbox.findByName(this.page, {name: LabelAlias.FirstName});
  }

  async getLastName(): Promise<Textbox> {
    return await Textbox.findByName(this.page, {name: LabelAlias.LastName});
  }

}
