import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitWhileLoading } from 'utils/waits-utils';
import { getPropValue } from 'utils/element-utils';
import WorkspaceBase from './workspace-base';
import Button from 'app/element/button';
import ShareModal from 'app/modal/share-modal';
import { config } from 'resources/workbench-config';

export const PageTitle = 'View Workspace Details';

export default class WorkspaceAboutPage extends WorkspaceBase {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForDocumentTitle(this.page, PageTitle), waitWhileLoading(this.page)]);
    return true;
  }

  // Returns map of key: AccessRole, value: Array[Emails].
  async findUsersInCollaboratorList(): Promise<Map<string, Array<string>>> {
    await waitWhileLoading(this.page);
    // At least one collaborator should eventually render, i.e. the current user.
    const xPath = './/*[starts-with(@data-test-id,"workspaceUser-") and text()]';
    await this.page.waitForXPath(xPath, { visible: true });

    const users = new Map<string, Array<string>>();
    const collaborators = await this.page.$x(xPath);
    for (const collaborator of collaborators) {
      const texts = await getPropValue<string>(collaborator, 'textContent');
      const [splitPart1, splitPart2] = texts.split(' : ');
      const role = splitPart1.trim();
      const email = splitPart2.trim();
      if (users.has(role)) {
        users.get(role).push(email);
      } else {
        users.set(role, new Array(email));
      }
    }
    return users;
  }

  async openShareModal(): Promise<ShareModal> {
    const share = Button.findByName(this.page, { containsText: 'Share' });
    await share.click();
    const modal = new ShareModal(this.page);
    await modal.waitForLoad();
    return modal;
  }

  async getCdrVersion(): Promise<string> {
    const cdrVerXpath = '//*[@data-test-id="cdrVersion"]';
    const cdr = await this.page.waitForXPath(cdrVerXpath, { visible: true });
    return getPropValue<string>(cdr, 'innerText');
  }

  async getCreationDate(): Promise<string> {
    const creationDateXpath = '//*[@data-test-id="creationDate"]/*[last()]';
    const creationDate = await this.page.waitForXPath(creationDateXpath, { visible: true });
    return getPropValue<string>(creationDate, 'innerText');
  }

  async getLastUpdatedDate(): Promise<string> {
    const lastUpdatedDateXpath = '//*[@data-test-id="lastUpdated"]/*[last()]';
    const lastUpdatedDate = await this.page.waitForXPath(lastUpdatedDateXpath, { visible: true });
    return getPropValue<string>(lastUpdatedDate, 'innerText');
  }

  // if the collaborator is already on this workspace, just remove them before continuing.
  async removeCollaborator(name = config.COLLABORATOR_USER): Promise<void> {
    const modal = await this.openShareModal();
    await modal.removeUser(name);
    await waitWhileLoading(this.page);
  }
}
