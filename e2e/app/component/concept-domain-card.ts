import { Page } from 'puppeteer';
import Button from 'app/element/button';
import Container from 'app/container';
import { getPropValue } from 'utils/element-utils';
import CriteriaSearchPage from '../page/criteria-search-page';

export enum Domain {
  Conditions = 'Conditions',
  DrugExposures = 'Drug Exposures',
  Measurements = 'Labs and Measurements',
  Observations = 'Observations',
  Procedures = 'Procedures'
}

const domainCardSelector = {
  cardXpath: '//*[@data-test-id="domain-box"]'
};

export default class ConceptDomainCard extends Container {
  static findDomainCard(page: Page, domain: Domain): ConceptDomainCard {
    const selector = `${domainCardSelector.cardXpath}[child::*[@tabindex="0" and @role="button" and text()="${domain}"]]`;
    return new ConceptDomainCard(page, selector);
  }

  private constructor(page: Page, xpath: string) {
    super(page, xpath);
  }

  async clickSelectConceptButton(): Promise<CriteriaSearchPage> {
    const selectConceptButton = await this.getSelectConceptButton();
    await selectConceptButton.waitUntilEnabled();
    await selectConceptButton.click();
    const criteriaSearch = new CriteriaSearchPage(this.page);
    await criteriaSearch.waitForLoad();
    return criteriaSearch;
  }

  /**
   * Find displayed number of Participants in this domain.
   */
  async getParticipantsCount(): Promise<string> {
    const selector = `${this.getXpath()}//*[contains(normalize-space(text()), "participants in domain")]`;
    const num = await this.extractConceptsCount(selector);
    console.log(`Participants in this domain: ${num}`);
    return num;
  }

  /**
   * Find displayed number of Concepts in this domain.
   */
  async getConceptsCount(): Promise<string> {
    const selector = `${this.getXpath()}//*[contains(normalize-space(text()), "concepts in this domain")]`;
    const num = await this.extractConceptsCount(selector);
    console.log(`Concepts in this domain: ${num}`);
    return num;
  }

  private async extractConceptsCount(selector: string): Promise<string> {
    const elemt = await this.page.waitForXPath(selector, { visible: true });
    const textContent = await getPropValue<string>(elemt, 'textContent');
    const regex = new RegExp(/\d{1,3}(,?\d{3})*/); // Match numbers with comma
    return regex.exec(textContent)[0];
  }

  private async getSelectConceptButton(): Promise<Button> {
    return Button.findByName(this.page, { name: 'Select Concepts' }, this);
  }
}
