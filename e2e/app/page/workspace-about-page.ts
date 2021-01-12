import {Page} from 'puppeteer';
import {waitForDocumentTitle, waitWhileLoading} from 'utils/waits-utils';
import {LinkText, WorkspaceAccessLevel} from 'app/text-labels';
import {getPropValue} from 'utils/element-utils';
import WorkspaceBase from './workspace-base';
import Button from 'app/element/button';
import ShareModal from 'app/component/share-modal';
import {config} from 'resources/workbench-config';
// import WorkspaceCard from 'app/component/workspace-card';

export const PageTitle = 'View Workspace Details';

export default class WorkspaceAboutPage extends WorkspaceBase {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      waitForDocumentTitle(this.page, PageTitle),
      waitWhileLoading(this.page)
    ]);
    return true;
  }

  async findUserInCollaboratorList(username: string): Promise<WorkspaceAccessLevel> {
    // At least one collab should eventually render, i.e. the current user.
    const collabXPath = `//*[starts-with(@data-test-id,"workspaceUser-")]`;
    await this.page.waitForXPath(collabXPath, {visible: true});

    // Fetch all of the collabs so we can string match and parse text content.
    const collabs = await this.page.$x(collabXPath);
    for (const c of collabs) {
      let collabLine = await getPropValue<string>(c, 'textContent');
      collabLine = collabLine.toLowerCase().trim();
      if (collabLine.includes(username.toLowerCase())) {
        for (const level of [
          WorkspaceAccessLevel.Reader,
          WorkspaceAccessLevel.Writer,
          WorkspaceAccessLevel.Owner
        ]) {
          if (collabLine.startsWith(level.toLowerCase())) {
            return level;
          }
        }
      }
    }

    return null;
  }

  async openShareModal(): Promise<ShareModal> {
    const share = await Button.findByName(this.page, {containsText: 'Share'});
    await share.click();
    const modal = new ShareModal(this.page);
    await modal.waitUntilVisible();
    return modal;
  }

  async getCdrVersion(): Promise<string> {
    const cdrVerXpath = '//*[@data-test-id="cdrVersion"]';
    const cdr = await this.page.waitForXPath(cdrVerXpath, {visible: true});
    return getPropValue<string>(cdr, 'innerText');
  }

  async getCreationDate(): Promise<string> {
    const creationDateXpath = '//*[@data-test-id="creationDate"]/*[last()]';
    const creationDate = await this.page.waitForXPath(creationDateXpath, {visible: true});
    return getPropValue<string>(creationDate, 'innerText');
  }

  async getLastUpdatedDate(): Promise<string> {
    const lastUpdatedDateXpath = '//*[@data-test-id="lastUpdated"]/*[last()]';
    const lastUpdatedDate = await this.page.waitForXPath(lastUpdatedDateXpath, {visible: true});
    return getPropValue<string>(lastUpdatedDate, 'innerText');
  }

  // if the collaborator is already on this workspace, just remove them before continuing.
  async removeCollab(): Promise<void> {  
    const accessLevel = await this.findUserInCollaboratorList(config.collaboratorUsername);
    if (accessLevel !== null) {
      await (await (this.openShareModal())).removeUser(config.collaboratorUsername);
      await waitWhileLoading(this.page);
    }
  }

  // to verify if the search input field is disabled for Writer/reader & enabled for Owner
  async verifyCollabInputField(): Promise<void> {
    const accessLevel = await this.findUserInCollaboratorList(config.collaboratorUsername);
    const modal = await this.openShareModal();
    const searchInput = await modal.waitForSearchBox();
    if (accessLevel !== WorkspaceAccessLevel.Owner){
      expect(await searchInput.isDisabled()).toBe(true);
       } else if (accessLevel === WorkspaceAccessLevel.Owner) {
      expect(await searchInput.isDisabled()).toBe(false);
   }
     await modal.clickButton(LinkText.Cancel);
 }

}
