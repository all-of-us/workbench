import {Page} from 'puppeteer';
import Modal from 'src/app/component/modal';
import {makeRandomName} from 'utils/str-utils';
import RadioButton from 'src/app/element/radiobutton';
import Textbox from 'src/app/element/textbox';
import Textarea from 'src/app/element/textarea';
import {LinkText} from 'src/app/text-labels';
import {getPropValue} from 'utils/element-utils';
import {waitWhileLoading} from 'utils/waits-utils';

const faker = require('faker/locale/en_US');

export enum SaveOption {
  CreateNewSet = 'Create new set',
  ChooseExistingSet = 'Choose existing set',
}

export default class ConceptSetSaveModal extends Modal {

  constructor(page: Page) {
    super(page);
  }

  /**
   *
   * Save new Concept Set: Fill in Concept name and Description.
   * @param {SaveOption} saveOption
   * @return {string} Concept name.
   */
  async fillOutSaveModal(saveOption: SaveOption = SaveOption.CreateNewSet, existingConceptSetName: string = '0'): Promise<string> {
    const createNewSetRadioButton = await RadioButton.findByName(this.page, {name: saveOption}, this);
    await createNewSetRadioButton.select();

    let conceptName: string;

    if (saveOption === SaveOption.CreateNewSet) {
      // Generate a random name as new Concept name.
      conceptName = makeRandomName();
      const nameTextbox = await Textbox.findByName(this.page, {name: 'Name'}, this);
      await nameTextbox.type(conceptName);

      // Type in Description
      const descriptionTextarea = await Textarea.findByName(this.page, {containsText: 'Description'}, this);
      await descriptionTextarea.type(faker.lorem.words());
    } else {
      const [selectedValue] = await this.page.select('[data-test-id="add-to-existing"] select', existingConceptSetName);
      const elem = await this.page.waitForSelector(`[data-test-id="add-to-existing"] select option[value="${selectedValue}"]`);
      conceptName = await getPropValue<string>(elem, 'textContent');
    }

    // Click SAVE button.
    await this.clickButton(LinkText.Save, {waitForClose: true});
    await waitWhileLoading(this.page);
    
    return conceptName;
  }

}
