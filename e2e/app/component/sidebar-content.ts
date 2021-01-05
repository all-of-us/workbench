
import {Page} from 'puppeteer';
import HelpSidebar from 'app/component/help-sidebar';
import ClrIconLink from 'app/element/clr-icon-link';
import {getPropValue} from 'utils/element-utils';
import ReactSelect from 'app/element/react-select';
import {waitWhileLoading} from 'utils/waits-utils';
import BaseElement from 'app/element/base-element';
import Button from 'app/element/button';
import {LinkText} from 'app/text-labels';

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

    // click the plus button located next to Annotations on the sidebar panel
    async getAnnotationsButton(): Promise<ClrIconLink> {
      return ClrIconLink.findByName(this.page, {containsText: 'Annotations', iconShape: 'plus-circle'});
    }
   
    // get the annotations name displaying on the sidebar panel
    async getAnnotationsName(): Promise<string> {
      const xpath = '//*[contains(normalize-space(text()), "Annotations")]/following::div[4]';
      const element = BaseElement.asBaseElement(this.page, await this.page.waitForXPath(xpath, {visible: true}));
      return element.getTextContent();
    }

     // click on the Annotations EDIT button to edit the annotations name
     async getAnnotationsEdit(): Promise<Button>{
      return Button.findByName(this.page, {name: LinkText.Edit});
    }

    
}