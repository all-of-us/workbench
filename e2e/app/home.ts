import {ElementHandle} from 'puppeteer';
import * as xpathHandler from '../driver/xpath-handler';
import {findPlusCircleIcon} from "./elements/xpath-finder";
import AuthenticatedPage from './mixin/authenticatedpage';

export default class Home extends AuthenticatedPage {

  public selectors = {
    pageTitle: 'Homepage',
    header: '//h3[normalize-space(text())="Workspaces"]',
    seeAllWorkspacesLink: '//*[normalize-space()="See all Workspaces"]',
    createNewWorkspaceLink: 'Workspaces',
  };

  public async isLoaded(): Promise<void> {
    await super.isLoaded(this.selectors.pageTitle);
    await this.puppeteerPage.waitForXPath(this.selectors.seeAllWorkspacesLink, {visible: true});
    await this.puppeteerPage.waitForXPath(this.selectors.header, {visible: true});
  }

  public async waitForReady(): Promise<Home> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  /**
   * Go to Home page.
   */
  public async goto(): Promise<Home> {
    await this.navigation.toHome();
    return this;
  }

  get seeAllWorkspacesLink(): Promise<ElementHandle> {
    return xpathHandler.waitForVisible(this.puppeteerPage, this.selectors.seeAllWorkspacesLink);
  }

  public async getCreateNewWorkspaceLink(): Promise<ElementHandle> {
    return findPlusCircleIcon(this.puppeteerPage, this.selectors.createNewWorkspaceLink);
  }

  public async toAllWorkspaces() {
    const navigPromise = this.puppeteerPage.waitForNavigation({waitUntil: 'networkidle0'});
    await this.seeAllWorkspacesLink;
    await navigPromise;
  }

}
