import {Page} from 'puppeteer';
import Modal from 'app/component/modal';
import {makeRandomName} from 'utils/str-utils';
import RadioButton from 'app/element/radiobutton';
import Textbox from 'app/element/textbox';
import Textarea from 'app/element/textarea';
import {LinkText} from 'app/text-labels';
import ConceptsetActionsPage from './conceptset-actions-page';

const faker = require('faker/locale/en_US');

export enum SaveOption {
  CreateNewSet = 'Create new set',
  ChooseExistingSet = 'Choose existing set',
}

export default class ConceptsetSaveModal extends Modal {

  constructor(page: Page) {
    super(page);
  }

  /**
   *
   * Save new Concept Set: Fill in Concept name and Description.
   * @param {SaveOption} saveOption
   * @return {string} Concept name.
   */
  async fillOutSaveModal(saveOption: SaveOption = SaveOption.CreateNewSet, existingConceptName: string = '0'): Promise<string> {
    const createNewSetRadioButton = await RadioButton.findByName(this.page, {name: saveOption}, this);
    await createNewSetRadioButton.select();

    let conceptName;

    if (saveOption === SaveOption.CreateNewSet) {
      // Generate a random name as new Concept name.
      conceptName = makeRandomName();
      const nameTextbox = await Textbox.findByName(this.page, {name: 'Name'}, this);
      await nameTextbox.type(conceptName);

      // Type in Description
      const descriptionTextarea = await Textarea.findByName(this.page, {containsText: 'Description'}, this);
      await descriptionTextarea.type(faker.lorem.words());
    } else {
      const [selectedValue] = await this.page.select('[data-test-id="add-to-existing"] select', existingConceptName);
      const elem = await this.page.waitForSelector(`[data-test-id="add-to-existing"] select option[value="${selectedValue}"]`);
      const value = await (await elem.getProperty('textContent')).jsonValue();
      conceptName = value.toString();
    }

    // Click SAVE button.
    await this.clickButton(LinkText.Save);

    const conceptActionPage = new ConceptsetActionsPage(this.page);
    await conceptActionPage.waitForLoad();
    
    return conceptName;
  }

}
