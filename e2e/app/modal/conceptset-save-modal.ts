import { Page } from 'puppeteer';
import { makeRandomName } from 'utils/str-utils';
import RadioButton from 'app/element/radiobutton';
import Select from 'app/element/select';
import Textbox from 'app/element/textbox';
import Textarea from 'app/element/textarea';
import { LinkText } from 'app/text-labels';
import { getPropValue } from 'utils/element-utils';
import { waitWhileLoading } from 'utils/waits-utils';
import Modal from './modal';
import faker from 'faker';

export enum SaveOption {
  CreateNewSet = 'Create new set',
  ChooseExistingSet = 'Choose existing set'
}

export default class ConceptSetSaveModal extends Modal {
  modalTitle: string;

  constructor(page: Page, opts?: { xpath?: string; modalIndex?: number }) {
    super(page, opts);
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
   * @param {string} existingConceptSetName
   * @return {string} Concept name.
   */
  async fillOutSaveModal(
    saveOption: SaveOption = SaveOption.CreateNewSet,
    existingConceptSetName: string
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
      await descriptionTextarea.paste(faker.lorem.words());
    } else {
      let selectedValue: string;
      if (existingConceptSetName) {
        const existingConceptSetSelect = new Select(this.page, '//*[@data-test-id="existing-set-select"]');
        selectedValue = await existingConceptSetSelect.selectOption(existingConceptSetName);
      } else {
        [selectedValue] = await this.page.select('[data-test-id="add-to-existing"] select', '0');
      }
      const elem = await this.page.waitForSelector(
        `[data-test-id="add-to-existing"] select option[value="${selectedValue}"]`
      );
      conceptName = await getPropValue<string>(elem, 'textContent');
    }

    // Click SAVE button.
    await this.clickButton(LinkText.Save, { waitForClose: true });
    await waitWhileLoading(this.page);
    return conceptName;
  }
}
