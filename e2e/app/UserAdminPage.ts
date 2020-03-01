import AuthenticatedPage from './page-mixin/AuthenticatedPage';

export const FIELD_LABEL = {
  TITLE: 'User Admin Table',
};

export default class UserAdminPage extends AuthenticatedPage {

  public async isLoaded(): Promise<boolean> {
    await super.isLoaded(FIELD_LABEL.TITLE);
    return true;
  }

  public async waitForReady(): Promise<UserAdminPage> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }
  

}
