import { Page } from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import Textbox from 'app/element/textbox';
import { getPropValue } from 'utils/element-utils';
import Table from 'app/component/table';
import BaseElement from 'app/element/base-element';
import DeleteRuntimeModal from 'app/modal/delete-runtime.modal';

const PageTitle = 'Workspace Admin | All of Us Researcher Workbench';

export default class WorkspaceAdminPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle)]);
    return true;
  }

  getWorkspaceNamespaceInput(): Textbox {
    const selector = "//label[text()='Workspace namespace']/following-sibling::input[@type='text']";
    return new Textbox(this.page, selector);
  }

  getLoadWorkspaceButton(): Button {
    return Button.findByName(this.page, { name: LinkText.LoadWorkspace });
  }

  async clickLoadWorkspaceButton(): Promise<void> {
    const button = this.getLoadWorkspaceButton();
    const navPromise = this.page.waitForNavigation({ waitUntil: ['load', 'networkidle0'] });
    await button.click();
    await navPromise;
    await waitWhileLoading(this.page);
  }

  // get the h2 page header (workspace)
  async getWorkspaceHeader(): Promise<string> {
    const h2 = await this.page.waitForXPath('//h2', { visible: true });
    return getPropValue<string>(h2, 'textContent');
  }

  // get all the h3 headers
  async getAllHeadings3(): Promise<string[]> {
    await this.page.waitForXPath('//h3', { visible: true });
    const headings = await page.$$eval('h3', (headers) => {
      return headers.map((header) => header.textContent);
    });
    // get only the first-5 headings(h3)
    const namespaceHeadings = headings.slice(0, 5);
    return namespaceHeadings;
  }

  getCloudStorageTable(): Table {
    const selector = '//table[@class="p-datatable-scrollable-header-table"]';
    return new Table(this.page, selector);
  }

  // get the column names of the cloud storage table
  getcloudStorageColNames(): Promise<string[]> {
    const cloudStorageTable = this.getCloudStorageTable();
    const cloudStorageColNames = cloudStorageTable.getColumnNames();
    return cloudStorageColNames;
  }

  async getAllHeadings2(): Promise<string[]> {
    await this.page.waitForXPath('//h2', { visible: true });
    const headings = await page.$$eval('h2', (headers) => {
      return headers.map((header) => header.textContent);
    });
    // get only the 2nd and 3rd headings(h2)
    const namespaceHeadings = headings.slice(1, 3);
    return namespaceHeadings;
  }

  // get the Notebook preview button
  getNotebookPreviewButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Preview });
  }

  // click the Notebook preview button when it is enabled
  async clickNotebookPreviewButton(): Promise<void> {
    const button = this.getNotebookPreviewButton();
    const navPromise = this.page.waitForNavigation({ waitUntil: ['load', 'networkidle0'] });
    await button.click();
    await navPromise;
    await waitWhileLoading(this.page);
  }

  // get the text area to input the reason to access
  getAccessReasonInput(): Textbox {
    const selector =
      '//label[contains(text(), "To preview notebooks, enter Access Reason (for auditing purposes")]' +
      '/following-sibling::textarea';
    return new Textbox(this.page, selector);
  }

  // get the No Active Runtime Text to verify that no runtime is active
  async getNoActiveRuntimeText(): Promise<string> {
    const xpath = '//h2[contains(text(),"Runtimes")]/following-sibling::p';
    const element = BaseElement.asBaseElement(this.page, await this.page.waitForXPath(xpath, { visible: true }));
    const textContent = await element.getTextContent();
    return textContent;
  }

  // get the Runtime Delete button
  getRuntimeDeleteButton(): Button {
    return Button.findByName(this.page, { name: LinkText.Delete });
  }

  async clickRuntimeDeleteButton(): Promise<DeleteRuntimeModal> {
    const button = this.getRuntimeDeleteButton();
    await button.click();
    const modal = new DeleteRuntimeModal(this.page);
    await modal.waitForLoad();
    return modal;
  }

  // get the runtime status in the Status col
  async getRuntimeStatus(): Promise<string> {
    const xpath = '//div[text()="Delete" and @role="button"]/preceding-sibling::div[1]';
    const element = BaseElement.asBaseElement(this.page, await this.page.waitForXPath(xpath, { visible: true }));
    const textContent = await element.getTextContent();
    return textContent;
  }

  // get the runtime status (delete) in the Status col
  async getRuntimeDeleteStatus(): Promise<string> {
    const xpath = '//div[text()="Delete" and @role="button"]/preceding-sibling::div[1][contains(text(),"Deleting")]';
    const element = BaseElement.asBaseElement(this.page, await this.page.waitForXPath(xpath, { visible: true }));
    const textContent = await element.getTextContent();
    return textContent;
  }
}
