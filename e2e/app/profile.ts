import AuthenticatedPage from './mixin/authenticatedpage';

const selectors = {
  pageTitle: 'Profile',
  header: '//*[normalize-space(text())="Profile"]',
};

export default class Profile extends AuthenticatedPage {

  public async isLoaded(): Promise<void> {
    await super.isLoaded(selectors.pageTitle);
    await this.puppeteerPage.waitForXPath(selectors.header, {visible: true});
  }

  public async waitForReady(): Promise<Profile> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  /**
   * Go to Profile page.
   */
  public async goto(): Promise<Profile> {
    await this.navigation.toProfile();
    return this;
  }

}
