import { Page } from 'puppeteer';
import { makeRandomName } from 'utils/str-utils';
import RadioButton from 'app/element/radiobutton';
import Textbox from 'app/element/textbox';
import Textarea from 'app/element/textarea';
import { LinkText } from 'app/text-labels';
import { getPropValue } from 'utils/element-utils';
import { waitWhileLoading } from 'utils/waits-utils';
import Modal from './modal';

const faker = require('faker/locale/en_US');

export enum SaveOption {
  CreateNewSet = 'Create new set',
  ChooseExistingSet = 'Choose existing set'
}

export default class ConceptSetSaveModal extends Modal {
  modalTitle: string;

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    const xpath = '//*[@data-test-id="add-concept-title"]';
    const title = await this.page.waitForXPath(xpath, { visible: true });
    this.modalTitle = await getPropValue<string>(title, 'textContent');
    return true;
  }

  /**
   *
   * Save new Concept Set: Fill in Concept name and Description.
   * @param {SaveOption} saveOption
   * @return {string} Concept name.
   */
  async fillOutSaveModal(
    saveOption: SaveOption = SaveOption.CreateNewSet,
    existingConceptSetName = '0'
  ): Promise<string> {
    const createNewSetRadioButton = RadioButton.findByName(this.page, { name: saveOption }, this);
    await createNewSetRadioButton.select();

    let conceptName: string;

    if (saveOption === SaveOption.CreateNewSet) {
      // Generate a random name as new Concept name.
      conceptName = makeRandomName();
      const nameTextbox = Textbox.findByName(this.page, { name: 'Name' }, this);
      await nameTextbox.type(conceptName);

      // Type in Description
      const descriptionTextarea = Textarea.findByName(this.page, { containsText: 'Description' }, this);
      await descriptionTextarea.type(faker.lorem.words());
    } else {
      const [selectedValue] = await this.page.select('[data-test-id="add-to-existing"] select', existingConceptSetName);
      const elem = await this.page.waitForSelector(
        `[data-test-id="add-to-existing"] select option[value="${selectedValue}"]`
      );
      conceptName = await getPropValue<string>(elem, 'textContent');
    }

    // Click SAVE button.
    await this.clickButton(LinkText.Save, { waitForClose: true });
    await waitWhileLoading(this.page);
    console.log(`"${this.modalTitle}" modal: Saved Concept Set "${conceptName}"`);
    return conceptName;
  }
}
