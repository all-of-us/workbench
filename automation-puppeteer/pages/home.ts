import {ElementHandle} from 'puppeteer-core';
import * as xpathHandler from '../services/xpath-handler';
import AuthenticatedPage from './authenticatedpage';

const selectors = {
  pageTitle: 'Homepage',
  header: '//h3[normalize-space(text())="Workspaces"]',
  seeAllWorkspacesLink: '//*[normalize-space()="See all Workspaces"]',
  createNewWorkspaceLink: '//*[normalize-space(.)="Workspaces"]//*[@role="img"]'
};

export default class Home extends AuthenticatedPage {

  public async isLoaded(): Promise<void> {
    await super.isLoaded(selectors.pageTitle);
    await this.puppeteerPage.waitForXPath(selectors.seeAllWorkspacesLink, {visible: true});
    await this.puppeteerPage.waitForXPath(selectors.header, {visible: true});
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
    return xpathHandler.waitForVisible(this.puppeteerPage, selectors.seeAllWorkspacesLink);
  }

  public async getCreateNewWorkspaceLink(): Promise<ElementHandle> {
    return await xpathHandler.waitForVisible(this.puppeteerPage, selectors.createNewWorkspaceLink);
  }

  public async toAllWorkspaces() {
    const navigPromise = this.puppeteerPage.waitForNavigation({waitUntil: 'networkidle0'});
    await this.seeAllWorkspacesLink;
    await navigPromise;
  }

}
