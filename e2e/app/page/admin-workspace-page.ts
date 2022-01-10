import { Page } from 'puppeteer';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import { LinkText } from 'app/text-labels';
import Textbox from 'app/element/textbox';
import Textarea from 'app/element/textarea';
import Table from 'app/component/table';
import BaseElement from 'app/element/base-element';
import DeleteRuntimeModal from 'app/modal/delete-runtime.modal';
import LockWorkspaceModal from 'app/modal/lock-workspace-modal';

const PageTitle = 'Workspace Admin | All of Us Researcher Workbench';

export const LabelAlias = {
  WorkspaceNamespace: 'Workspace namespace'
};

export default class WorkspaceAdminPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle)]);
    await waitWhileLoading(this.page);
    return true;
  }

  getWorkspaceNamespaceInput(): Textbox {
    return Textbox.findByName(this.page, { name: LabelAlias.WorkspaceNamespace });
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

  getLockWorkspaceButton(): Button {
    return Button.findByName(this.page, { normalizeSpace: LinkText.LockWorkspace });
  }

  //click "Lock Workspace" button
  async clickLockWorkspaceButton(): Promise<LockWorkspaceModal> {
    const button = this.getLockWorkspaceButton();
    await button.click();
    const modal = new LockWorkspaceModal(this.page);
    await modal.waitForLoad();
    return modal;
  }

  // extract only the Workspace Namespace text for verification
  async getWorkspaceNamespaceText(): Promise<string> {
    const xpath = '//label[contains(normalize-space(),"Workspace Namespace")]//following-sibling::div';
    const element = BaseElement.asBaseElement(this.page, await this.page.waitForXPath(xpath, { visible: true }));
    const textContent = element.getTextContent();
    return textContent;
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
  getAccessReasonTextArea(): Textarea {
    const selector =
      '//label[contains(text(), "To preview notebooks, enter Access Reason (for auditing purposes)")]' +
      '/following-sibling::textarea';
    return new Textarea(this.page, selector);
  }

  // get the No Active Runtime Text to verify that no runtime is active
  async getNoActiveRuntimeText(): Promise<string> {
    const xpath = '//h2[contains(text(),"Runtimes")]/following-sibling::p';
    const element = BaseElement.asBaseElement(this.page, await this.page.waitForXPath(xpath, { visible: true }));
    return await element.getTextContent();
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
    const xpath = '//div[text()="Delete" and @role="button"]/preceding-sibling::*[1]';
    const element = BaseElement.asBaseElement(
      this.page,
      await this.page.waitForXPath(xpath, { visible: true, timeout: 60000 })
    );
    return element.getTextContent();
  }
}
