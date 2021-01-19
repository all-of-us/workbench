import {Page} from 'puppeteer';
import {waitForDocumentTitle, waitWhileLoading} from 'utils/waits-utils';
import SnowmanMenu from 'app/component/snowman-menu';
import {buildXPath} from 'app/xpath-builders';
import {ElementType} from 'app/xpath-options';
import {Option} from 'app/text-labels';
import Button from 'app/element/button';
import Textbox from 'app/element/textbox';
import {getPropValue} from 'utils/element-utils';
import AuthenticatedPage from './authenticated-page';
import CopyModal from 'app/component/copy-modal';


const PageTitle = 'Concept Set';

export default class ConceptSetPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      waitForDocumentTitle(this.page, PageTitle),
      waitWhileLoading(this.page),
    ]);
    return true;
  }

  async openCopyToWorkspaceModal(conceptSetName: string): Promise<CopyModal> {
    await this.getSnowmanMenu(conceptSetName).then(menu => menu.select(Option.CopyToAnotherWorkspace, {waitForNavi: false}));
    const modal = new CopyModal(this.page);
    await modal.waitForLoad();
    return modal;
  }

  /**
   * Get Concept snowman menu after click Snowman icon to open the menu.
   */
  async getSnowmanMenu(conceptName: string): Promise<SnowmanMenu> {
    const iconXpath = buildXPath( {name: conceptName, ancestorLevel: 2, type: ElementType.Icon, iconShape: 'ellipsis-vertical'});
    await this.page.waitForXPath(iconXpath, {visible: true}).then(icon => icon.click());
    return new SnowmanMenu(this.page);
  }

  async getConceptSetName(): Promise<string> {
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
    await this.page.waitForXPath(xpath, {visible: true});
    return new Button(this.page, xpath);
  }

}
