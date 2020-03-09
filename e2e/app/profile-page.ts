import {Page} from 'puppeteer';
import Textbox from './aou-elements/textbox';
import AuthenticatedPage from './authenticated-page';


export const PAGE = {
  TITLE: 'Profile',
};

export const FIELD_LABEL = {
  FIRST_NAME: 'First Name',
  LAST_NAME: 'Last Name',
  CONTACT_EMAIL: 'Contact Email',
  CURRENT_POSITION: 'Your Current Position',
  ORGANIZATION: 'Your Organization',
  CURRENT_RESEARCH_WORK: 'Current Research Work',
  ABOUT_YOU: 'About You',
  INSTITUTION: 'Institution',
  ROLE: 'Role',
  DISCARD_CHANGES: 'Discard Changes',
  SAVE_PROFILE: 'Save Profile',
};

export default class ProfilePage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await this.waitForTextExists(PAGE.TITLE);
      return true;
    } catch (e) {
      return false;
    }
  }

  async getFirstName(): Promise<Textbox> {
    const textbox = new Textbox(this.page);
    await textbox.withLabel({text: FIELD_LABEL.FIRST_NAME});
    return textbox;
  }

  async getLastName(): Promise<Textbox> {
    const textbox = new Textbox(this.page);
    await textbox.withLabel({text: FIELD_LABEL.LAST_NAME});
    return textbox;
  }

}
