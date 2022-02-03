import { Page } from 'puppeteer';
import { waitForDocumentTitle, waitForText, waitWhileLoading } from 'utils/waits-utils';
import SnowmanMenu from 'app/component/snowman-menu';
import { buildXPath } from 'app/xpath-builders';
import { ElementType } from 'app/xpath-options';
import { MenuOption } from 'app/text-labels';
import Button from 'app/element/button';
import Textbox from 'app/element/textbox';
import { getPropValue } from 'utils/element-utils';
import AuthenticatedPage from './authenticated-page';
import CopyToWorkspaceModal from 'app/modal/copy-to-workspace-modal';

const PageTitle = 'Concept Set';

export default class ConceptSetPage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await waitForDocumentTitle(this.page, PageTitle);
    await waitWhileLoading(this.page);
    return true;
  }

  async openCopyToWorkspaceModal(conceptSetName: string): Promise<CopyToWorkspaceModal> {
    await this.getSnowmanMenu(conceptSetName).then((menu) =>
      menu.select(MenuOption.CopyToAnotherWorkspace, { waitForNav: false })
    );
    const modal = new CopyToWorkspaceModal(this.page);
    await modal.waitForLoad();
    return modal;
  }

  /**
   * Get Concept snowman menu after click Snowman icon to open the menu.
   */
  async getSnowmanMenu(conceptName: string): Promise<SnowmanMenu> {
    const iconXpath = buildXPath({
      name: conceptName,
      ancestorLevel: 2,
      type: ElementType.Icon,
      iconShape: 'ellipsis-vertical'
    });
    await this.page.waitForXPath(iconXpath, { visible: true }).then((icon) => icon.click());
    return new SnowmanMenu(this.page);
  }

  async getConceptSetName(): Promise<string> {
    const xpath = '//*[@data-test-id="concept-set-title"]';
    const title = await this.page.waitForXPath(xpath, { visible: true });
    return getPropValue<string>(title, 'innerText');
  }

  async edit(originalConceptName: string, newConceptName?: string, newDescription?: string): Promise<void> {
    await this.getEditButton().then((button) => button.click());

    // Wait and find the Name input web-element with value.
    const inputXpath = `//input[@data-test-id="edit-name" and @value="${originalConceptName}"]`;
    await new Textbox(this.page, inputXpath).waitForXPath({ visible: true });
    await waitForText(this.page, originalConceptName);

    const saveButton = Button.findByName(this.page, { name: 'Save', ancestorLevel: 0 });
    await saveButton.waitUntilEnabled();

    // Enter name
    if (newConceptName !== undefined) {
      const xpath = '//*[@data-test-id="edit-name"]';
      const nameInput = new Textbox(this.page, xpath);
      await nameInput.type(newConceptName);
    }

    // Enter description
    if (newDescription !== undefined) {
      const descInputXpath = '//*[@id="edit-description"]';
      const descInput = new Textbox(this.page, descInputXpath);
      await descInput.paste(newDescription);
    }

    await saveButton.click();
    await this.getEditButton().then((butn) => butn.waitUntilEnabled());
    await waitWhileLoading(this.page);
  }

  /**
   * Find the Edit (pencil) button.
   */
  async getEditButton(): Promise<Button> {
    const xpath = '//*[@role="button"]/*[normalize-space()="Edit"]';
    await this.page.waitForXPath(xpath, { visible: true });
    return new Button(this.page, xpath);
  }
}
