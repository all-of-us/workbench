import {Page} from 'puppeteer';
import Dialog, {ButtonLabel} from 'app/component/dialog';
import {makeRandomName} from 'utils/str-utils';
import RadioButton from 'app/element/radiobutton';
import Textbox from 'app/element/textbox';
import Textarea from 'app/element/textarea';
import ConceptsetActionsPage from './conceptset-actions-page';

const faker = require('faker/locale/en_US');

export default class ConceptsetSaveModal extends Dialog {

  constructor(page: Page) {
    super(page);
  }

  async saveAndCloseDialog(): Promise<string> {
    // Even though the radiobutton is selected by default, select it explicitly to confirm existances and not readonly.
    const createNewSetRadioButton = await RadioButton.findByName(this.page, {name: 'Create new set'}, this);
    await createNewSetRadioButton.select();

    // Generate a random name as new Concept name.
    const newConceptName = makeRandomName();
    const nameTextbox = await Textbox.findByName(this.page, {name: 'Name'}, this);
    await nameTextbox.type(newConceptName);

    // Type in Description
    const descriptionTextarea = await Textarea.findByName(this.page, {containsText: 'Description'}, this);
    await descriptionTextarea.type(faker.lorem.words());

    // Click SAVE button.
    await this.clickButton(ButtonLabel.Save);

    const conceptActionPage = new ConceptsetActionsPage(this.page);
    await conceptActionPage.waitForLoad();
    
    return newConceptName;
  }

}
