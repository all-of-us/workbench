import {ElementHandle, Page} from 'puppeteer-core';

const selectors = {
  openMenuLink: '//div[contains(@style,"transform: rotate(0deg)")]',
  closeMenuLink: '//div[contains(@style,"transform: rotate(90deg)")]',
  menuItems: '//div[div[contains(@style,"transform: rotate(90deg)")]]//*[@role="button"]',
  subMenuExpandLink: '//div[div[contains(@style,"transform: rotate(90deg)")]]//*[@role="button"]//clr-icon[@shape="angle"]'
};

export default class NavigationMenu {
  public puppeteerPage: Page;

  constructor(page: Page) {
    this.puppeteerPage = page;
  }

   /**
    * Open dropdown.
    */
  public async open() {
    const icon = await this.puppeteerPage.waitForXPath(selectors.openMenuLink, {visible: true});
    await icon.click(); // open dropdown
  }

   /**
    * Close dropdown.
    */
  public async close() {
    const icon = await this.puppeteerPage.waitForXPath(selectors.closeMenuLink, {visible: true});
    await icon.click(); // open dropdown
  }

   /**
    * Open User submenu.
    */
  public async openUserSubmenu() {
    await this.open();
    const link = await this.userSubmenuLink;
    await link.click();
  }

  get userSubmenuLink(): Promise<ElementHandle> {
    return this.puppeteerPage.waitForXPath(selectors.subMenuExpandLink, {visible: true});
  }

   /**
    * Find the "Home" element in dropdown menu.
    */
  get homeLink(): Promise<ElementHandle> {
    return this.puppeteerPage.waitForXPath(this.menuItem('Home'), {visible: true});
  }

   /**
    * Find the "Your Workspaces" element in dropdown menu.
    */
  get allWorkspacesLink(): Promise<ElementHandle> {
    return this.puppeteerPage.waitForXPath(this.menuItem('Your Workspaces'), {visible: true});
  }

   /**
    * Find the "Sign Out" element in dropdown menu.
    */
  get signOutLink(): Promise<ElementHandle> {
    return this.puppeteerPage.waitForXPath(this.menuItem('Sign Out'), {visible: true});
  }

   /**
    * Find the "Profile" element in dropdown menu.
    */
  get profileLink(): Promise<ElementHandle> {
    return this.puppeteerPage.waitForXPath(this.menuItem('Profile'), {visible: true});
  }

   /**
    * <pre>
    * Click "Home" link in dropdown.
    * Precondition: hamburger icon is visible and dropdown is not open.
    * Steps:
    * 1. find hamburger icon and click it
    * 2. find "Home" link and click it
    * </pre>
    */
  public async toHome() {
    await this.open();
    const link = await this.homeLink;
    await this.clickAndWait(link);
  }

   /**
    * <pre>
    * Click "Your Workspaces" link in dropdown.
    * Precondition: hamburger icon is visible and dropdown is not open.
    * Steps:
    * 1. find hamburger icon and click it
    * 2. find "Your Workspaces" link and click it
    * </pre>
    */
  public async toAllWorkspaces() {
    await this.open();
    const link = await this.allWorkspacesLink;
    await this.clickAndWait(link);
  }

   /**
    * <pre>
    * Click Log Out link in dropdown.
    * Precondition: hamburger icon is visible and dropdown is not open.
    * Steps:
    * 1. find hamburger icon and click it
    * 2. find User submenu icon and click it
    * 3. find "Sign Out" link and click it
    * </pre>
    */
  public async signOut() {
    await this.openUserSubmenu();
    const link = await this.signOutLink;
    await this.clickAndWait(link);
  }

   /**
    * <pre>
    * Click "Profile" link in dropdown.
    * Precondition: hamburger icon is visible and dropdown is not open.
    * Steps:
    * 1. find hamburger icon and click it
    * 2. find User submenu icon and click it
    * 3. find "Profile" link and click it
    * </pre>
    */
  public async toProfile() {
    await this.openUserSubmenu();
    const link = await this.profileLink;
    await this.clickAndWait(link);
  }

   /**
    * constructs xpath selector for a specified menu link
    * @param name
    */
  public menuItem(name: string): string {
    return selectors.menuItems + `//*[normalize-space(text())='${name}']`
  };

  public async clickAndWait(element: ElementHandle) {
    const naviPromise = this.puppeteerPage.waitForNavigation();
    await element.click();
    await naviPromise;
  }

}
