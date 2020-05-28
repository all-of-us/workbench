import {Page} from 'puppeteer';
import EllipsisMenu from 'app/component/ellipsis-menu';
import {WorkspaceAction} from 'app/page-identifiers';
import AuthenticatedPage from 'app/page/authenticated-page';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import ClrIconLink from 'app/element/clr-icon-link';

export enum LabelAlias {
  Data = 'DATA',
  Analysis = 'ANALYSIS',
  About = 'ABOUT',
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

  async selectWorkspaceAction(action: WorkspaceAction): Promise<void> {
    const ellipsisMenu = new EllipsisMenu(this.page, './/*[@data-test-id="workspace-menu-button"]');
    return ellipsisMenu.selectAction(action);
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
    return ClrIconLink.forLabel(this.page, {name: LabelAlias.Datasets, iconShape: 'plus-circle'});
  }

  async getAddCohortsButton(): Promise<ClrIconLink> {
    return ClrIconLink.forLabel(this.page, {name: LabelAlias.Cohorts, iconShape: 'plus-circle'});
  }

}
