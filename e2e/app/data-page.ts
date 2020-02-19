import {waitUntilTitleMatch} from "../driver/waitFuncs";
import authenticatedpage from "./mixin/authenticatedpage";

const tabsSelector = {
   cohortsTab: '//*[@role="button"][(text()="Cohorts")]',
   dataSetsTab: '//*[@role="button"][(text()="Datasets")]',
   cohortReviewsTab: '//*[@role="button"][(text()="Cohort Reviews")]',
   conceptSetsTab: '//*[@role="button"][(text()="Concept Sets")]',
   showAllTab: '//*[@role="button"][(text()="Show All")]',
};


export default class DataPage extends authenticatedpage {

   public async waitUntilPageReady() {
      await waitUntilTitleMatch(this.puppeteerPage, 'Data Page');
      await this.puppeteerPage.waitForXPath(tabsSelector.showAllTab, {visible: true});
      await this.waitForSpinner();
   }

}