import {Page} from 'puppeteer';
import EllipsisMenu from 'app/component/ellipsis-menu';
import {EllipsisMenuAction} from 'app/page-identifiers';
import AuthenticatedPage from 'app/page/authenticated-page';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import ClrIconLink from 'app/element/clr-icon-link';
import DataResourceCard from '../component/data-resource-card';
import CohortBuildPage from './cohort-build-page';

export enum LabelAlias {
  Data = 'Data',
  Analysis = 'Analysis',
  About = 'About',
  Cohorts = 'Cohorts',
  Datasets = 'Datasets',
  CohortReviews = 'Cohort Reviews',
  ConceptSets = 'Concept Sets',
  ShowAll = 'Show All',
}

const PageTitle = 'Data Page';

export default class DataPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        waitForDocumentTitle(this.page, PageTitle),
        waitWhileLoading(this.page),
      ]);
      return true;
    } catch (e) {
      console.log(`DataPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  async selectWorkspaceAction(action: EllipsisMenuAction): Promise<void> {
    const ellipsisMenu = new EllipsisMenu(this.page, './/*[@data-test-id="workspace-menu-button"]');
    return ellipsisMenu.clickAction(action);
  }

  /**
   * Select DATA, ANALYSIS or ABOUT page tab.
   * @param tabName
   */
  async openTab(tabName: LabelAlias): Promise<void> {
    const selector = `//*[@id="workspace-top-nav-bar"]/*[@aria-selected and @role="button" and text()="${tabName}"]`;
    const tab = await this.page.waitForXPath(selector, {visible: true});
    return tab.click();
  }

  async getAddDataSetsButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: LabelAlias.Datasets, iconShape: 'plus-circle'});
  }

  async getAddCohortsButton(): Promise<ClrIconLink> {
    return ClrIconLink.findByName(this.page, {name: LabelAlias.Cohorts, iconShape: 'plus-circle'});
  }

  /**
   * Delete cohort by look up its name using Ellipsis menu.
   * @param {string} cohortName
   */
  async deleteCohort(cohortName: string): Promise<string> {
    const cohortCard = await DataResourceCard.findCard(this.page, cohortName);
    const menu = await cohortCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.DELETE, false);
    const dialogContent = await (new CohortBuildPage(this.page)).deleteConfirmationDialog();
    console.log(`Cohort "${cohortName}" deleted.`);
    return dialogContent;
  }

}
