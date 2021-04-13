import { Page } from 'puppeteer';
import ReviewCriteriaSidebar from 'app/component/review-criteria-sidebar';
import Button from 'app/element/button';
import ClrIconLink from 'app/element/clr-icon-link';
import Textbox from 'app/element/textbox';
import { centerPoint, dragDrop } from 'utils/test-utils';
import { waitForNumericalString, waitWhileLoading } from 'utils/waits-utils';
import { LinkText } from 'app/text-labels';
import AuthenticatedPage from './authenticated-page';

export enum Ethnicity {
  NotHispanicOrLatino = 'Not Hispanic or Latino',
  HispanicOrLatino = 'Hispanic or Latino',
  RaceEthnicityNoneOfThese = 'Race Ethnicity None Of These',
  PreferNotToAnswer = 'Prefer Not To Answer',
  Skip = 'Skip'
}

export default class CohortSearchPage extends AuthenticatedPage {
  private containerXpath = '//*[@id="cohort-search-container"]';

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      this.page.waitForXPath('//*[@id="cohort-search-container"]', { visible: true }),
      this.page.waitForXPath('//*[@role="button"]/img[@alt="Go back"]', { visible: true })
    ]);
    await waitWhileLoading(this.page);
    return true;
  }

  async waitForEthnicityCriteriaLink(criteriaType: Ethnicity): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, { startsWith: criteriaType, iconShape: 'plus-circle', ancestorLevel: 0 });
  }

  async addEthnicity(ethnicities: Ethnicity[]): Promise<void> {
    for (const ethnicity of ethnicities) {
      const link = await this.waitForEthnicityCriteriaLink(ethnicity);
      await link.click();
    }
  }

  /**
   * Type age lower and upper bounds.
   * @param {number} minAge
   * @param {number} maxAge
   */
  async addAge(minAge: number, maxAge: number): Promise<string> {
    const selector = `${this.containerXpath}//input[@type="number"]`;
    await this.page.waitForXPath(selector, { visible: true });

    const [lowerNumberInput, upperNumberInput] = await this.page.$x(selector);
    await Textbox.asBaseElement(this.page, lowerNumberInput)
      .type(minAge.toString())
      .then((input) => input.pressTab());
    await Textbox.asBaseElement(this.page, upperNumberInput)
      .type(maxAge.toString())
      .then((input) => input.pressTab());

    // Get count from slider badge
    const count = await waitForNumericalString(this.page, `${this.containerXpath}//*[@id="age-count"]`);
    // Click ADD SELECTION to add selected age range
    await Button.findByName(this.page, { name: LinkText.AddSelection }).then((button) => button.click());
    await this.reviewAndSaveCriteria();
    return count;
  }

  async reviewAndSaveCriteria(): Promise<void> {
    // Click FINISH & REVIEW button. Sidebar should open.
    const finishAndReviewButton = await Button.findByName(this.page, { name: LinkText.FinishAndReview });
    await finishAndReviewButton.waitUntilEnabled();
    await finishAndReviewButton.click();

    // Click SAVE CRITERIA button. Sidebar closes.
    const reviewCriteriaSidebar = await new ReviewCriteriaSidebar(this.page);
    await reviewCriteriaSidebar.waitUntilVisible();
    await reviewCriteriaSidebar.clickSaveCriteriaButton();
  }

  // Experimental
  async drageAgeSlider(): Promise<void> {
    const getXpath = (classValue: string) => {
      return `${this.containerXpath}//*[text()="Age Range"]/ancestor::node()[1]//*[contains(@class,"${classValue}") and @role="slider"]`;
    };

    const lowerNumberInputHandle = await this.page.waitForXPath(getXpath('noUi-handle-lower'), { visible: true });
    const upperNumberInputHandle = await this.page.waitForXPath(getXpath('noUi-handle-upper'), { visible: true });

    const [x1, y1] = await centerPoint(lowerNumberInputHandle);
    // drag lowerHandle slider horizontally: 50 pixels to the right.
    await dragDrop(this.page, lowerNumberInputHandle, { x: x1 + 50, y: y1 });
    const [x2, y2] = await centerPoint(upperNumberInputHandle);
    // drag upperHandle slider horizontally: 50 pixels to the left.
    await dragDrop(this.page, upperNumberInputHandle, { x: x2 - 50, y: y2 });
  }
}
