import { Page } from 'puppeteer';
import Textbox from 'app/element/textbox';
import AuthenticatedPage from 'app/page/authenticated-page';
import { waitForDocumentTitle, waitForUrl, waitWhileLoading } from 'utils/waits-utils';
import Button from 'app/element/button';
import Textarea from 'app/element/textarea';
import { PageUrl } from 'app/text-labels';
import Link from 'app/element/link';

export const PageTitle = 'Profile';

const LabelAlias = {
  ResearchBackground: 'Your research background, experience and research interests',
  SaveProfile: 'Save Profile',
  NeedsConfirmation: 'Please update or verify your profile.',
  LooksGood: 'Looks Good'
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

  /**
   * Load 'Profile' page and ensure page load is completed.
   */
  async load(): Promise<this> {
    await this.loadPage({ url: PageUrl.Profile });
    await waitWhileLoading(this.page);
    return this;
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([waitForUrl(this.page, '/profile'), waitForDocumentTitle(this.page, PageTitle)]);
    await waitWhileLoading(this.page);
    return true;
  }

  getFirstNameInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.FirstName });
  }

  getLastNameInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.LastName });
  }

  getProfessionalUrlInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.ProfessionalUrl });
  }

  getResearchBackgroundTextarea(): Textarea {
    return Textarea.findByName(this.page, { normalizeSpace: LabelAlias.ResearchBackground });
  }

  getAddress1Input(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.Address1 });
  }

  getAddress2Input(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.Address2 });
  }

  getCityInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.City });
  }

  getStateInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.State });
  }

  getZipCodeInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.Zip });
  }

  getCountryInput(): Textbox {
    return Textbox.findByName(this.page, { dataTestId: DataTestIdAlias.Country });
  }

  getSaveProfileButton(): Button {
    return Button.findByName(this.page, { name: LabelAlias.SaveProfile });
  }

  async needsConfirmation(): Promise<boolean> {
    return await this.containsText(LabelAlias.NeedsConfirmation);
  }

  getLooksGoodLink(): Link {
    return Link.findByName(this.page, { name: LabelAlias.LooksGood });
  }
}
