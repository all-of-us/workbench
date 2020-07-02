import {Page} from 'puppeteer';
import {waitWhileLoading} from 'utils/test-utils';
import {waitForDocumentTitle} from 'utils/waits-utils';
import EllipsisMenu from 'app/component/ellipsis-menu';
import {buildXPath} from 'app/xpath-builders';
import {ElementType} from 'app/xpath-options';
import {EllipsisMenuAction} from 'app/text-labels';
import AuthenticatedPage from './authenticated-page';
import ConceptsetCopyModal from './conceptset-copy-modal';


const PageTitle = 'Concept Set';

export default class ConceptsetPage extends AuthenticatedPage {

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
      console.log(`ConceptsetPage isLoaded() encountered ${e}`);
      return false;
    }
  }

  async openCopyToWorkspaceModal(conceptName: string): Promise<ConceptsetCopyModal> {
    const ellipsis = this.getEllipsisMenu(conceptName);
    await ellipsis.clickAction(EllipsisMenuAction.CopyToAnotherWorkspace, {waitForNav: false});
    const modal = new ConceptsetCopyModal(this.page);
    await modal.waitUntilVisible();
    await modal.getDestinationTextbox();
    await modal.getNameTextbox();
    return modal;
  }

  // Get Concept Ellipsis dropdown menu
  getEllipsisMenu(conceptName: string): EllipsisMenu {
    const ellipsisXpath = buildXPath( {name: conceptName, ancestorLevel: 2, type: ElementType.Icon, iconShape: 'ellipsis-vertical'});
    return new EllipsisMenu(this.page, ellipsisXpath);
  }

}
