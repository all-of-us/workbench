import { Page } from 'puppeteer';
import Textbox from 'app/element/textbox';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle, waitForUrl, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import Textarea from 'app/element/textarea';

export const PageTitle = 'Profile';

const LabelAlias = {
  ResearchBackground: 'Your research background, experience and research interests',
  SaveProfile: 'Save Profile'
};

const DataTestIdAlias = {
  FirstName: 'givenName',
  LastName: 'familyName',
  ProfessionalUrl: 'professionalUrl',
  Address1: 'streetAddress1',
  Address2: 'streetAddress2',
  City: 'city',
  State: 'state',
  Zip: 'zipCode',
  Country: 'country'
};

export const MissingErrorAlias = {
  FirstName: 'First Name',
  LastName: 'Last Name',
  ResearchBackground: 'Current Research',
  Address1: 'Street address1',
  City: 'City',
  State: 'State',
  Zip: 'Zip code',
  Country: 'Country'
};

export default class ProfilePage extends AuthenticatedPage {
  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      waitForUrl(this.page, '/profile'),
      waitForDocumentTitle(this.page, PageTitle),
      waitWhileLoading(this.page)
    ]);
    return true;
  }

  async getFirstNameInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.FirstName });
  }

  async getLastNameInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.LastName });
  }

  async getProfessionalUrlInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.ProfessionalUrl });
  }

  async getResearchBackgroundTextarea(): Promise<Textarea> {
    return Textarea.findByName(this.page, { normalizeSpace: LabelAlias.ResearchBackground });
  }

  async getAddress1Input(): Promise<Textbox> {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.Address1 });
  }

  async getAddress2Input(): Promise<Textbox> {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.Address2 });
  }

  async getCityInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.City });
  }

  async getStateInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.State });
  }

  async getZipCodeInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.Zip });
  }

  async getCountryInput(): Promise<Textbox> {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.Country });
  }

  async getSaveProfileButton(): Promise<Button> {
    return Button.findByName(this.page, { name: LabelAlias.SaveProfile });
  }
}
