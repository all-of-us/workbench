import {Page} from 'puppeteer';
import {waitForDocumentTitle, waitWhileLoading} from 'utils/waits-utils';
import Button from 'app/element/button';
import {LinkText} from 'app/text-labels';
import AuthenticatedPage from './authenticated-page';
import {getPropValue} from 'utils/element-utils';

const PageTitle = 'Participant Detail';

export default class CohortParticipantDetailPage extends AuthenticatedPage {

  constructor(page: Page) {
    super(page);
  }

  async isLoaded(): Promise<boolean> {
    await Promise.all([
      waitForDocumentTitle(this.page, PageTitle),
      waitWhileLoading(this.page),
    ]);
    return true;
  }

  // get the particpant ID displayed between the two pi-angle button on participant detail page
  async getParticipantIDnum(): Promise<string> {
    const selector = `//*[@class="detail-header"]//span[contains(., "Participant ")]`;
    const pID = await this.extractParticipantID(selector);
    console.log(`Participant ID: ${pID}`);
    return pID;
  }

  // extract only the number of the participant
  async extractParticipantID(selector: string): Promise<string> {
    const elemt = await this.page.waitForXPath(selector, {visible: true});
    const textContent = await getPropValue<string>(elemt, 'textContent');
    const regex = new RegExp(/\d+/);
    return (regex.exec(textContent))[0];
  }
  
  // get the back to review set button
  async getBackToReviewSetButton(): Promise<Button> {
    return Button.findByName(this.page, {name: LinkText.BackToReviewSet});
  }
 
   // click on the pen icon located on the side bar
   async clickPenIconHelpSideBar(): Promise<void> {
    const clickPenIcon = await this.page.waitForXPath(`//*[@data-test-id="help-sidebar-icon-annotations"]`, {visible: true});
    await clickPenIcon.click();
    await this.page.waitForXPath('//*[@data-test-id="sidebar-content"]', {visible: true});
    await waitWhileLoading(this.page);
  }

  // click on pi-angle-right button to go to next participant

  async goToTheNextParticipant(): Promise<void> {
     const iconXpath = '//button[@title ="Go To the Next Participant"]';
    await this.page.waitForXPath(iconXpath, {visible: true}).then(icon => icon.click());
  }

  // click on pi-angle-left button to go to prior participant
  async goToThePriorParticipant(): Promise<void> {
    const iconXpath = '//button[@title ="Go To the Prior Participant"]';
    await this.page.waitForXPath(iconXpath, {visible: true}).then(icon => icon.click());
  }


}
