import {Page} from 'puppeteer';
import ClrIconLink from 'app/element/clr-icon-link';
import {getPropValue} from 'utils/element-utils';
import ReactSelect from 'app/element/react-select';
import {waitWhileLoading} from 'utils/waits-utils';
import Button from 'app/element/button';
import {LinkText, SideBarLink} from 'app/text-labels';
import Textarea from 'app/element/textarea'
import EditDeleteAnnotationsModal from 'app/modal/edit-delete-annotations-modal';
import AnnotationFieldModal from 'app/modal/annotation-field-modal';
import BaseHelpSidebar from './base-help-sidebar';

export enum ReviewStatus {
  Excluded = 'Excluded',
  Included = 'Included',
  NeedsFurtherReview = 'Needs Further Review '
}

export default class AnnotationsSidebar extends BaseHelpSidebar {

  constructor(page: Page) {
    super(page);
  }

  async open(): Promise<void> {
    const isOpen = await this.isVisible();
    if (isOpen) {
      return;
    }
    await this.clickIcon(SideBarLink.EditAnnotations);
    await this.waitUntilVisible();
    await this.page.waitForTimeout(1000);
    // Wait for visible text
    await this.page.waitForXPath(`${this.getXpath()}//h3`, {visible: true});
    // Wait for visible button
    await this.page.waitForXPath(`${this.getXpath()}//*[@role="button"]`, {visible: true});
    console.log(`Opened "${await this.getTitle()}" Participant & Annotations sidebar`);
  }

  async getParticipantID(): Promise<string> {
    const selector = `${this.getXpath()}//div[1][text()="Participant "]`;
    const pID = await this.extractParticipantDetails(selector);
    return pID;
  }

  /**
   * select participant status.
   * @param {ReviewStatus}  status
   */
  async selectReviewStatus(status: ReviewStatus): Promise<string> {
    const selectMenu = new ReactSelect(this.page, {name: 'Choose a Review Status for Participant'});
    await selectMenu.selectOption(status);
    await waitWhileLoading(this.page);
    return selectMenu.getSelectedOption();
  }

  // click the plus button located next to Annotations on the sidebar panel
  async clickAnnotationsButton(): Promise<AnnotationFieldModal> {
    const link = await ClrIconLink.findByName(this.page, {containsText: 'Annotations', iconShape: 'plus-circle'}, this);
    await link.click();
    const annotationFieldModal = new AnnotationFieldModal(this.page);
    await annotationFieldModal.waitForLoad();
    return annotationFieldModal
  }

  // get the annotations name displaying on the sidebar panel
  async getAnnotationsName(annotationFieldName: string): Promise<string> {
    const selector = this.getFieldNameSelector(annotationFieldName);
    const annotationfieldLabel = await this.extractFieldNameText(selector);
    return annotationfieldLabel;
  }

  // click on the Annotations EDIT button to edit the annotations field name
  async getAnnotationsEditModal(): Promise<EditDeleteAnnotationsModal> {
    const button = await Button.findByName(this.page, {name: LinkText.Edit}, this);
    await button.click();
    const modal = new EditDeleteAnnotationsModal(this.page);
    await modal.waitForLoad();
    return modal;
  }

  // text area
  async getAnnotationsTextArea(): Promise<Textarea> {
    const selector = `${this.getXpath()}//*[contains(normalize-space(text()), "Annotations")]/following::textarea`;
    const annotationTextArea = new Textarea(this.page, selector);
    return annotationTextArea;
  }

  // look for the field name
  async findFieldName(annotationFieldName: string): Promise<string | null> {
    try {
      this.getFieldNameSelector(annotationFieldName);
    } catch (err) {
      // no field.
      return null;
    }
  }

  private async extractParticipantDetails(selector: string): Promise<string> {
    const elemt = await this.page.waitForXPath(selector, {visible: true});
    const textContent = await getPropValue<string>(elemt, 'textContent');
    // const regex = new RegExp(/\d{1,3}(,?\d{3})*/); // Match numbers with comma
    const regex = new RegExp(/\d+/);
    return (regex.exec(textContent))[0];
  }

  // get the xpath of the respective annotation field label
  private getFieldNameSelector(fieldName: string): string {
    return `${this.getXpath()}//*[contains(normalize-space(text()), "Annotations")]/following::div[contains(normalize-space(), "${fieldName}")]`;
  }

  // extract only the annotation field name
  private async extractFieldNameText(selector: string): Promise<string> {
    const elemt = await this.page.waitForXPath(selector, {visible: true});
    const textContent = await getPropValue<string>(elemt, 'textContent');
    const regex = new RegExp(/(aoutest)-\d+/);
    return (regex.exec(`${textContent}`))[0];
  }

}
