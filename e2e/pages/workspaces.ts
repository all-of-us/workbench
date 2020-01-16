import {ElementHandle, JSHandle} from 'puppeteer-core';
import {waitUntilNetworkIdle} from '../services/page-wait';
import AuthenticatedPage from './authenticatedpage';

const configs = require('../config/config');

const selectors = {
  pageTitle: 'View Workspaces',
  header: '//h3[normalize-space(text())="Workspaces"]',
  container: '//*[h3[normalize-space(text())="Workspaces"]]',
  createNewWorkspaceButton: '//*[@role="button" and normalize-space(.)="Create a New Workspace"]',
  cards: '*[data-test-id="workspace-card-name"]',
  workspaceLink (workspaceName: string) {
    return `//*[@role='button'][./*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]`
  },
  accessLevel (workspaceName: string) {
    return `"//*[.//*[@data-test-id='workspace-card-name' and normalize-space(text())='${workspaceName}']]/*[@data-test-id='workspace-access-level']"`;
  }
};

export default class Workspaces extends AuthenticatedPage {

  public async isLoaded() {
    await super.isLoaded(selectors.pageTitle);
    await this.puppeteerPage.waitForXPath(selectors.createNewWorkspaceButton, {visible: true});
    await this.puppeteerPage.waitForXPath(selectors.header, {visible: true});
  }

  public async waitForReady(): Promise<Workspaces> {
    await this.isLoaded();
    await this.waitForSpinner();
    return this;
  }

  /**
   * goto the URL of the Workspaces page
   */
  public async goto(): Promise<Workspaces> {
    await this.puppeteerPage.goto(configs.uiBaseUrl + configs.workspacesUrlPath);
    await waitUntilNetworkIdle(this.puppeteerPage);
    // await this.navigation.toAllWorkspaces();
    return this;
  }

  /**
   * Find all visible Workspace names.
   */
  public async getAllWorkspaceNames(): Promise<any[]> {
    return await this.puppeteerPage.evaluate(() => {
      return Array.from(document.querySelectorAll(`${selectors.cards}`)).map(a =>a.textContent)
    })
  }

   /**
    * Find workspace access level.
    * @param workspaceName
    */
  public async getWorkspaceAccessLevel(workspaceName: string) : Promise<JSHandle<string>> {
    const element = await this.puppeteerPage.waitForXPath(selectors.accessLevel(workspaceName), {visible: true});
    return await element.getProperty('innerText');
  }

  /**
   * Find "Create a New Workspace" element on the page.
   */
  public async getCreateWorkspaceButton(): Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.createNewWorkspaceButton, {visible: true});
  }

   /**
    * Find element with specified workspace name on the page.
    * @param {string} workspaceName
    */
  public async getWorkspaceLink(workspaceName: string) : Promise<ElementHandle> {
    return await this.puppeteerPage.waitForXPath(selectors.workspaceLink(workspaceName));
  }

}
