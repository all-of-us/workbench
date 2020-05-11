import {ElementHandle, Page} from 'puppeteer';
import EllipsisMenu from 'app/component/ellipsis-menu';
import AuthenticatedPage from 'app/page/authenticated-page';
import {WorkspaceAction} from 'app/page-identifiers';
import CriteriaModal from '../component/criteria-modal';
import TieredMenu from '../component/tiered-menu';
import {findIcon} from '../element/xpath-finder';

export enum PageTab {
  DATA = 'DATA',
  ANALYSIS = 'ANALYSIS',
  ABOUT = 'ABOUT'
}

export const TAB_SELECTOR = {
  COHORTS: 'Cohorts',
  DATASETS: 'Datasets',
  COHORTREVIEWS: 'Cohort Reviews',
  CONCEPTSETS: 'Concept Sets',
  SHOWALL: 'Show All',
};

export const PAGE = {
  TITLE: 'Data Page',
};


export const LABEL_ALIAS = {
  COHORTS: 'Cohorts',
  DATASETS: 'Datasets',
};

export const FIELD = {
  createCohortsButton: {
    textOption: {
      text: LABEL_ALIAS.COHORTS
    }
  },
  createDatasetsButton: {
    textOption: {
      text: LABEL_ALIAS.DATASETS
    }
  }
};

export default class WorkspaceDataPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    try {
      await Promise.all([
        this.waitUntilTitleMatch(PAGE.TITLE),
        this.waitUntilNoSpinner(),
      ]);
      return true;
    } catch (e) {
      return false;
    }
  }

  async selectWorkspaceAction(action: WorkspaceAction) {
    const ellipsisMenu = new EllipsisMenu(this.page, './/*[@data-test-id="workspace-menu-button"]');
    await ellipsisMenu.selectAction(action);
  }

  /**
   * Select DATA, ANALYSIS or ABOUT page tab.
   * @param tabName
   */
  async selectTab(tabName: PageTab): Promise<void> {
    const selector = '//*[@id="workspace-top-nav-bar"]/*[@aria-selected and @role="button"]';
    await this.page.waitForXPath(selector, {visible: true});
    const tabs = await this.page.$x(selector);
    for (const tab of tabs) {
      const contentProp = await tab.getProperty('textContent');
      if (await contentProp.jsonValue() === tabName) {
        return tab.click();
      }
      await tab.dispose();
    }
    throw new Error(`Failed to find page tab with name ${tabName}`);
  }

  /**
   * Select Show All, Cohorts, Cohort Reviews, Concept Sets, or Datasets tab.
   * @param tabName
   */
  async selectShowTab(tabName: PageTab): Promise<void> {
    const selector = '//*[@id="workspace-top-nav-bar"]/*[@aria-selected and @role="button"]';
    await this.page.waitForXPath(selector, {visible: true});
    const tabs = await this.page.$x(selector);
    for (const tab of tabs) {
      const contentProp = await tab.getProperty('textContent');
      if (await contentProp.jsonValue() === tabName) {
        return tab.click();
      }
      await tab.dispose();
    }
    throw new Error(`Failed to find page tab with name ${tabName}`);
  }

  async addCohortsButton(): Promise<ElementHandle> {
    return findIcon({pageInstance: this.page}, {text: LABEL_ALIAS.COHORTS}, 'plus-circle');
  }

  async addDataSetsButton(): Promise<ElementHandle> {
    return findIcon({pageInstance: this.page}, {text: LABEL_ALIAS.DATASETS}, 'plus-circle');
  }

  async selectCriteriaMenu(menu: string | string[], groupIndex: number = 1): Promise<CriteriaModal> {
    const addCriteriaButton = await this.page.waitForXPath(this.addCriteriaButtonXpath(groupIndex), {visible: true});
    await addCriteriaButton.click(); // Click menu dropdown trigger to open menu
    await TieredMenu.selectMenu(this.page, menu);
    const criteriaModal = new CriteriaModal(this.page);
    await criteriaModal.waitUntilVisible();
    return criteriaModal;
  }

  private addCriteriaButtonXpath(groundIndex: number) {
    return `${this.includeGroupXpath()}//*[normalize-space()="Group ${groundIndex}"]/ancestor::node()[1]/*[normalize-space()="Add Criteria"]/button`;
  }


  // @ts-ignore
  private chartsXpath() {
    return '//*[@id="list-charts"]';
  }
  // @ts-ignore
  private excludeGroupXpath() {
    return '//*[@id="list-exclude-groups"]';
  }
  // @ts-ignore
  private includeGroupXpath() {
    return '//*[@id="list-include-groups"]';
  }


}
