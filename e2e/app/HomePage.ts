import {ElementHandle} from 'puppeteer';
import * as xpathHandler from '../driver/xpath-handler';
import {findPlusCircleIcon} from './aou-elements/xpath-finder';
import AuthenticatedPage from './mixin-pages/AuthenticatedPage';

const configs = require('../resources/workbench-config.js');

export default class HomePage extends AuthenticatedPage {

  public static selectors = {
    pageTitle: 'Homepage',
    header: '//h3[normalize-space(text())="Workspaces"]',
    seeAllWorkspacesLink: '//*[normalize-space()="See all Workspaces"]',
    createNewWorkspaceLink: 'Workspaces',
  };

  public async isLoaded(): Promise<boolean> {
    await super.isLoaded(HomePage.selectors.pageTitle);
    await this.puppeteerPage.waitForXPath(HomePage.selectors.seeAllWorkspacesLink, {visible: true});
    await this.puppeteerPage.waitForXPath(HomePage.selectors.header, {visible: true});
    return true;
  }

  public async waitForReady(): Promise<HomePage> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  /**
   * navigate to Home page URL
   */
  public async navigateToURL(): Promise<void> {
    const pageUrl = configs.uiBaseUrl;
    await this.puppeteerPage.goto(pageUrl, {waitUntil: ['domcontentloaded','networkidle0']});
    await this.waitForReady();
  }


  /**
   * Go to Home page.
   */
  public async navigateToHome(): Promise<HomePage> {
    await this.navigation.navToHome();
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
