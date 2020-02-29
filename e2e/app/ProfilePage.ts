import AuthenticatedPage from './mixin-pages/AuthenticatedPage';

const selectors = {
  pageTitle: 'Profile',
  header: '//*[normalize-space(text())="Profile"]',
};

export default class ProfilePage extends AuthenticatedPage {

  public async isLoaded(): Promise<boolean> {
    await super.isLoaded(selectors.pageTitle);
    await this.puppeteerPage.waitForXPath(selectors.header, {visible: true});
    return true;
  }

  public async waitForReady(): Promise<ProfilePage> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  /**
   * Go to Profile page.
   */
  public async goto(): Promise<ProfilePage> {
    await this.navigation.navToProfile();
    return this;
  }

}
