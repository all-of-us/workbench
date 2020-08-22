import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import EllipsisMenu from 'app/component/ellipsis-menu';
import {buildXPath} from 'app/xpath-builders';
import {ElementType} from 'app/xpath-options';
import {EllipsisMenuAction} from 'app/text-labels';
import Button from 'app/element/button';
import Textbox from 'app/element/textbox';
import {getPropValue} from 'utils/element-utils';
import AuthenticatedPage from './authenticated-page';
import CopyModal from 'app/component/copy-modal';


const PageTitle = 'Concept Set';

export default class ConceptsetPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (e) {
      console.log(`ConceptsetPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  async openCopyToWorkspaceModal(conceptName: string): Promise<CopyModal> {
    const ellipsis = this.getEllipsisMenu(conceptName);
    await ellipsis.clickAction(EllipsisMenuAction.CopyToAnotherWorkspace, {waitForNav: false});
    return new CopyModal(this.page);
  }

  // Get Concept Ellipsis dropdown menu
  getEllipsisMenu(conceptName: string): EllipsisMenu {
    const ellipsisXpath = buildXPath( {name: conceptName, ancestorLevel: 2, type: ElementType.Icon, iconShape: 'ellipsis-vertical'});
    return new EllipsisMenu(this.page, ellipsisXpath);
  }

  async getConceptName(): Promise<string> {
    const xpath = `//*[@data-test-id="concept-set-title"]`;
    const title = await this.page.waitForXPath(xpath, {visible: true});
    return getPropValue<string>(title, 'innerText');
  }

  async edit(newConceptName?: string, newDescription?: string): Promise<void> {
    await this.getEditButton().then(butn => butn.click());
    // edit name
    if (newConceptName !== undefined) {
      const nameInputXpath = '//*[@data-test-id="edit-name"]';
      const nameInput = new Textbox(this.page, nameInputXpath);
      await nameInput.type(newConceptName);
    }
    // edit description
    if (newDescription !== undefined) {
      const descInputXpath = '//*[@data-test-id="edit-description"]';
      const descInput = new Textbox(this.page, descInputXpath);
      await descInput.paste(newDescription);
    }
    const saveButton = await Button.findByName(this.page, {name: 'Save', ancestorLevel: 0});
    await saveButton.click();
    await this.getEditButton().then(butn => butn.waitUntilEnabled());
  }

  /**
   * Find the Edit (pencil) button.
   */
  async getEditButton(): Promise<Button> {
    const xpath = '//*[@role="button"]/*[normalize-space()="Edit"]';
    return new Button(this.page, xpath);
  }

}
