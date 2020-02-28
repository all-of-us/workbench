import {ElementHandle} from 'puppeteer';
import * as xpathHandler from '../driver/xpath-handler';
import {findPlusCircleIcon} from './aou-elements/xpath-finder';
import AuthenticatedPage from './mixin-pages/AuthenticatedPage';

export default class HomePage extends AuthenticatedPage {

  public static selectors = {
    pageTitle: 'Homepage',
    header: '//h3[normalize-space(text())="Workspaces"]',
    seeAllWorkspacesLink: '//*[normalize-space()="See all Workspaces"]',
    createNewWorkspaceLink: 'Workspaces',
  };

  public async isLoaded(): Promise<void> {
    await super.isLoaded(HomePage.selectors.pageTitle);
    await this.puppeteerPage.waitForXPath(HomePage.selectors.seeAllWorkspacesLink, {visible: true});
    await this.puppeteerPage.waitForXPath(HomePage.selectors.header, {visible: true});
  }

  public async waitForReady(): Promise<HomePage> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  /**
   * Go to Home page.
   */
  public async goto(): Promise<HomePage> {
    await this.navigation.toHome();
    return this;
  }

  get seeAllWorkspacesLink(): Promise<ElementHandle> {
    return xpathHandler.waitForVisible(this.puppeteerPage, HomePage.selectors.seeAllWorkspacesLink);
  }

  public async getCreateNewWorkspaceLink(): Promise<ElementHandle> {
    return findPlusCircleIcon(this.puppeteerPage, HomePage.selectors.createNewWorkspaceLink);
  }

  public async toAllWorkspaces() {
    const navigPromise = this.puppeteerPage.waitForNavigation({waitUntil: 'networkidle0'});
    await this.seeAllWorkspacesLink;
    await navigPromise;
  }

}
