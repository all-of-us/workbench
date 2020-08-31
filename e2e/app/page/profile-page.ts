import {Page} from 'puppeteer';
import Textbox from 'app/element/textbox';
import AuthenticatedPage from 'app/page/authenticated-page';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle, waitForUrl} from 'utils/waits-utils';
import Button from '../element/button';
import Textarea from '../element/textarea';

export const PageTitle = 'Profile';

export const LabelAlias = {
  FirstName: 'First Name',
  LastName: 'Last Name',
  SaveProfile: 'Save Profile',
};


export const DataTestIdAlias = {
  FirstName: 'First Name',
  LastName: 'Last Name',
  ProfessionalUrl: 'professionalUrl',
  Address1: 'streetAddress1',
  Address2: 'streetAddress2',
  City: 'city',
  State: 'state',
  Zip: 'zipCode',
  Country: 'country',
};

export default class ProfilePage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForUrl(this.page, '/profile'),
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (err) {
      console.log(`ProfilePage isLoaded() encountered ${err}`);
      return false;
    }
  }

  async getFirstNameInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {name: LabelAlias.FirstName});
  }

  async getLastNameInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {name: LabelAlias.LastName});
  }

  async getProfessionalUrlInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.ProfessionalUrl});
  }

  async getResearchBackgroundTextarea(): Promise<Textarea> {
    return Textarea.findByName(this.page, {normalizeSpace: 'Your research background, experience and research interests'});
  }

  async getAddress1Input(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.Address1});
  }

  async getAddress2Input(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.Address2});
  }

  async getCityInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.City});
  }

  async getStateInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.State});
  }

  async getZipCodeInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.Zip});
  }

  async getCountryInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, {dataTestId: DataTestIdAlias.Country});
  }

  async getSaveProfileButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LabelAlias.SaveProfile});
  }
}
