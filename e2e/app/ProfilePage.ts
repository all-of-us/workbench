import {Page} from 'puppeteer';
import {waitUntilFindTexts} from '../driver/waitFuncs';
import TextBox from './aou-elements/TextBox';
import AuthenticatedPage from './page-mixin/AuthenticatedPage';
import {SideNav} from './page-mixin/SideNav';


export const FIELD_LABEL = {
  TITLE: 'Profile',
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

export default class Profile extends AuthenticatedPage {

  public page: Page;
  constructor(page: Page) {
    super(page);
    this.page = page;
  }

  public async isLoaded(): Promise<boolean> {
    await super.isLoaded(FIELD_LABEL.TITLE);
    await waitUntilFindTexts(this.puppeteerPage, FIELD_LABEL.TITLE);
    return true;
  }

  public async waitForReady(): Promise<Profile> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  public async getFirstName(): Promise<TextBox> {
    const textbox = new TextBox(this.puppeteerPage);
    await textbox.withLabel({text: FIELD_LABEL.FIRST_NAME});
    return textbox;
  }

  public async getLastName(): Promise<TextBox> {
    const textbox = new TextBox(this.puppeteerPage);
    await textbox.withLabel({text: FIELD_LABEL.LAST_NAME});
    return textbox;
  }

}

export const ProfilePage = SideNav(Profile);

