
import {Page} from 'puppeteer';
import HelpSidebar from 'app/component/help-sidebar';
import ClrIconLink from 'app/element/clr-icon-link';
import {getPropValue} from 'utils/element-utils';
import ReactSelect from 'app/element/react-select';
import {waitWhileLoading} from 'utils/waits-utils';


const defaultXpath = '//*[@data-test-id="sidebar-content"]';

export enum ReviewStatus {
  Excluded = 'Excluded',
  Included = 'Included',
  NeedsFurtherReview = 'Needs Further Review '
}

export default class SidebarContent extends  HelpSidebar {
  

    constructor(page: Page, xpath: string = defaultXpath) {
      super(page, xpath);
    }

    async getParticipantID(): Promise<string> {
        const selector = `${this.getXpath()}//div[1][text()="Participant "]`;
        const pID = await this.extractParticipantDetails(selector);
        console.log(`Participant id 1: ${pID}`);
        return pID;
    }

    async getParticipantDOB(): Promise<string> {
        const selector = `${this.getXpath()}//*[contains(normalize-space(text()), "DOB")]/parent::*//span/following-sibling::text()`;
        const pidDob = await this.extractParticipantDetails(selector);
        console.log(`DOB: ${pidDob}`);
        return pidDob;
    }

    
    private async extractParticipantDetails(selector: string): Promise<string> {
      const elemt = await this.page.waitForXPath(selector, {visible: true});
      const textContent = await getPropValue<string>(elemt, 'textContent');
      // const regex = new RegExp(/\d{1,3}(,?\d{3})*/); // Match numbers with comma
       const regex = new RegExp(/\d+/);
      return (regex.exec(textContent))[0];
    }

    /**
     * select participant status.
     * @param {ReviewStatus}  status
     */
    async selectReviewStatus(status: ReviewStatus): Promise<string> {
      const selectMenu = new ReactSelect(this.page, {name:'Choose a Review Status for Participant'});
      await selectMenu.selectOption(status);
      await waitWhileLoading(this.page);
      return selectMenu.getSelectedOption();
    }
  
    async getAnnotationsButton(): Promise<ClrIconLink> {
      return ClrIconLink.findByName(this.page, {iconShape: 'plus-circle'});
    }

 
}