import { findOrCreateWorkspace, isValidDate, signInWithAccessToken } from 'utils/test-utils';
import { MenuOption, LinkText, ResourceCard } from 'app/text-labels';
import { makeRandomName, makeWorkspaceName } from 'utils/str-utils';
import CohortParticipantDetailPage from 'app/page/cohort-participant-detail-page';
import CohortReviewModal from 'app/modal/cohort-review-modal';
import CohortReviewPage from 'app/page/cohort-review-page';
import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { waitForText } from 'utils/waits-utils';
import { getPropValue } from 'utils/element-utils';
import AnnotationsSidebar, { ReviewStatus } from 'app/component/annotations-sidebar';
import { AnnotationType } from 'app/modal/annotation-field-modal';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { Surveys } from 'app/page/cohort-participants-group';

jest.setTimeout(20 * 60 * 1000);

describe('Cohort review tests', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = makeWorkspaceName();
  const reviewSetNumberOfParticipants = 100;

  test('Create cohort review set in cohort build page', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    let cohortBuildPage = await dataPage.clickAddCohortsButton();

    // Include Participants Group 1: add a survey.
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeSurveys([Surveys.BASICS]);
    await cohortBuildPage.getTotalCount();
    await cohortBuildPage.createCohort();

    const cohortActionsPage = new CohortActionsPage(page);
    const cohortName = await cohortActionsPage.getCohortName();
    cohortBuildPage = await cohortActionsPage.clickCohortName();
    await cohortBuildPage.getTotalCount();

    const reviewSetsButton = cohortBuildPage.getCopyButton();
    await reviewSetsButton.click();

    const modal = new CohortReviewModal(page);
    await modal.waitForLoad();

    const reviewSetNumberOfParticipants = 100;
    await modal.fillInNumberOfParticipants(reviewSetNumberOfParticipants);
    await modal.clickButton(LinkText.CreateSet);

    const cohortReviewPage = new CohortReviewPage(page);
    await cohortReviewPage.waitForLoad();

    await waitForText(page, `Review Sets for ${cohortName}`);

    // Verify table pagination records count.
    const participantsTable = cohortReviewPage.getDataTable();
    const records = await participantsTable.getNumRecords();
    // Table records page numbering is in "1 - 25 of 100 records" format.
    expect(Number(records[2])).toEqual(reviewSetNumberOfParticipants);

    console.log(`Created Review Set with ${reviewSetNumberOfParticipants} participants.`);

    // Click Back to Cohort link
    const backToCohortButton = cohortReviewPage.getBackToCohortButton();
    await backToCohortButton.click();

    await cohortBuildPage.waitForLoad();
    await cohortBuildPage.getTotalCount();

    // Back out to Data page
    await dataPage.openDataPage();
    await dataPage.waitForLoad();

    // Verify Cohort Review card exists
    const resourceCard = new DataResourceCard(page);
    const reviewCohortCard = await resourceCard.findCard(cohortName, ResourceCard.CohortReview);
    expect(reviewCohortCard).toBeTruthy();

    await dataPage.deleteResource(cohortName, ResourceCard.CohortReview);
    await dataPage.deleteResource(cohortName, ResourceCard.Cohort);
  });

  /**
   * Test:
   * Create a new workspace.
   * Create a new Cohort from Criteria: drug and procedure.
   * Create a Review Set for 100 participants via card's ellipsis menu.
   * Verification: on Cohort review page and the Annotations side bar.
   * Add/edit/delete annotations fields.
   * Rename Cohort review.
   * Delete cohort review via card's ellipsis menu.
   */
  test('Create cohort review set from cohort card', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    // Include Participants Group 1: Add hydroxychloroquine drug.
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeDrugs('hydroxychloroquine', 1);
    // Include Participants Group 1: Add Hydrocodone drug.
    await group1.includeDrugs('Hydrocodone', 1);

    // Include Participants Group 2: Add colonoscopy procedures.
    const group2 = cohortBuildPage.findIncludeParticipantsGroup('Group 2');
    await group2.includeProcedures('Colonoscopy', 1);

    // Include Participants Group 3: Add Red cell indices labs and measurements.
    const group3 = cohortBuildPage.findIncludeParticipantsGroup('Group 3');
    await group3.includeLabsAndMeasurements('Red cell indices', 1);

    // Save new cohort
    const cohortName = await cohortBuildPage.createCohort();

    // Find new cohort card.
    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();
    await dataPage.openDataPage({ waitPageChange: true });
    const cohortCard = await dataPage.findCohortCard(cohortName);

    await cohortCard.selectSnowmanMenu(MenuOption.Review, { waitForNav: true });
    const modal = new CohortReviewModal(page);
    await modal.waitForLoad();
    await modal.fillInNumberOfParticipants(reviewSetNumberOfParticipants);
    await modal.clickButton(LinkText.CreateSet);
    console.log(`Created Review Set with ${reviewSetNumberOfParticipants} participants.`);

    let cohortReviewPage = new CohortReviewPage(page);
    await cohortReviewPage.waitForLoad();
    await waitForText(page, `Review Sets for ${cohortName}`);

    // Verify table pagination records count.
    let participantsTable = cohortReviewPage.getDataTable();
    const records = await participantsTable.getNumRecords();
    // Table records page numbering is in "1 - 25 of 100 records" format.
    expect(Number(records[0])).toEqual(1);
    expect(Number(records[1])).toEqual(25);
    expect(Number(records[2])).toEqual(reviewSetNumberOfParticipants);

    // Verify table column names match.
    const columns = [
      'Participant ID',
      'Date of Birth',
      'Deceased',
      'Sex at Birth',
      'Gender',
      'Race',
      'Ethnicity',
      'Status'
    ];
    const columnNames = await participantsTable.getColumnNames();
    expect(columnNames).toHaveLength(columns.length);
    expect(columnNames.sort()).toEqual(columns.sort());

    // Get Date of Birth in row 2.
    const dobCell = await participantsTable.getCell(2, 2);
    const cellValue = await getPropValue<string>(dobCell, 'textContent');
    // Check birth date is valid format.
    isValidDate(cellValue);

    // Check table row link navigation works. Click ParticipantId link in the second row.
    const participantId = await cohortReviewPage.clickParticipantLink(2);

    // Not checking anything in Participant Detail page.
    const participantDetailPage = new CohortParticipantDetailPage(page);
    await participantDetailPage.waitForLoad();

    // Open the participant sidebar
    const annotationsSidebar = new AnnotationsSidebar(page);
    await annotationsSidebar.open();

    const reviewParticipantid1 = await annotationsSidebar.getParticipantID();
    expect(participantId).toEqual(reviewParticipantid1);

    // select review status from dropdown option
    const participantStatus1 = await annotationsSidebar.selectReviewStatus(ReviewStatus.Excluded);

    // click on the plus-icon next to annotations. the annotations modal displays
    let annotationFieldModal = await annotationsSidebar.clickAnnotationsButton();

    await annotationFieldModal.selectAnnotationType(AnnotationType.FreeText);

    // create new annotation name
    const newAnnotationName = makeRandomName();
    await annotationFieldModal.createNewAnnotationName(newAnnotationName);

    // close the sidebar content
    await annotationsSidebar.close();

    // navigate to the next participant
    await participantDetailPage.goToTheNextParticipant();
    await participantDetailPage.waitForLoad();

    // get the participant ID on the detail page
    const detailPageParticipantid = await participantDetailPage.getParticipantIDnum();

    // click on the pen icon to open the sidebar
    await annotationsSidebar.open();

    // get the participant ID on the sidebar content
    const reviewParticipantid2 = await annotationsSidebar.getParticipantID();
    console.log(`reviewParticipantid2: ${reviewParticipantid2}`);
    // validate that the participant ID on detail page and the sidebar content match
    expect(detailPageParticipantid).toEqual(reviewParticipantid2);

    // select a review status
    const participantStatus2 = await annotationsSidebar.selectReviewStatus(ReviewStatus.Included);

    // verify if the same Annotations Name also displays for the next Participant ID.
    const annotationTextBoxName = await annotationsSidebar.getAnnotationsName(newAnnotationName);
    expect(annotationTextBoxName).toEqual(newAnnotationName);

    // create new annotation name
    const newAnnotationRename = makeRandomName();

    // click on the annotations EDIT button to open EditDeleteAnnotationsModal
    let editDeleteAnnotationsFieldModal = await annotationsSidebar.getAnnotationsEditModal();

    // edit the annotation field name
    await editDeleteAnnotationsFieldModal.renameAnnotation(newAnnotationRename);

    // verify that the Annotation textbox field is displaying the new name
    const annotationTextBoxName2 = await annotationsSidebar.getAnnotationsName(newAnnotationRename);
    expect(annotationTextBoxName2).toEqual(newAnnotationRename);

    await participantDetailPage.goToThePriorParticipant();
    await participantDetailPage.waitForLoad();

    // verify that the prior participant is displaying the same annotation field name
    expect(annotationTextBoxName2).toEqual(newAnnotationRename);

    // verify that the text area is also displaying fr prior participant
    await annotationsSidebar.open();
    const annotationsTextArea = annotationsSidebar.getAnnotationsTextArea();
    expect(await annotationsTextArea.asElementHandle()).toBeTruthy();

    // click on the plus-icon next to annotations
    annotationFieldModal = await annotationsSidebar.clickAnnotationsButton();

    // create a a new annotation field by selecting the annotation type option: numeric field
    await annotationFieldModal.selectAnnotationType(AnnotationType.NumericField);

    // create new annotation name for the numeric field
    const newAnnotationName2 = makeRandomName();
    await annotationFieldModal.createNewAnnotationName(newAnnotationName2);

    const annotationTextBoxName3 = await annotationsSidebar.getAnnotationsName(newAnnotationName2);
    expect(annotationTextBoxName3).toEqual(newAnnotationName2);

    // click the annotations edit  button to delete the annotation textbox field
    editDeleteAnnotationsFieldModal = await annotationsSidebar.getAnnotationsEditModal();
    await editDeleteAnnotationsFieldModal.deleteAnnotationsName();

    // verify that the deletion of the annotation field was successful
    expect(await annotationsSidebar.findFieldName(newAnnotationRename)).toBeFalsy();

    await annotationsSidebar.close();

    // navigate to review set page and check if the status column is displaying the review status for both participants
    cohortReviewPage = await participantDetailPage.getBackToReviewSetButton();

    participantsTable = cohortReviewPage.getDataTable();

    // Get the status of participant1
    const statusCell1 = await participantsTable.getCell(2, 8);
    const statusValue1 = await getPropValue<string>(statusCell1, 'textContent');
    expect(statusValue1).toEqual(participantStatus1);
    console.log(`${reviewParticipantid1}: ${statusValue1}`);

    // Get the status of participant2
    const statusCell2 = await participantsTable.getCell(3, 8);
    const statusValue2 = await getPropValue<string>(statusCell2, 'textContent');
    expect(statusValue2).toEqual(participantStatus2);
    console.log(`${reviewParticipantid2}: ${statusValue2}`);

    // return to cohort review page
    await cohortReviewPage.getBackToCohortButton().clickAndWait();

    // Land on Cohort Build page
    await cohortBuildPage.waitForLoad();

    // Land on the Data Page & click the Cohort Reviews SubTab
    await dataPage.openCohortReviewsSubtab();

    // Rename Cohort Review
    const newCohortReviewName = makeRandomName();
    await dataPage.renameResource(cohortName, newCohortReviewName, ResourceCard.CohortReview);

    // Verify Rename Cohort Review is successful.
    expect(await DataResourceCard.findCard(page, newCohortReviewName)).toBeTruthy();

    // Delete Cohort Review
    const modalTextContent = await dataPage.deleteResource(newCohortReviewName, ResourceCard.CohortReview);

    // Verify Delete Cohort Review dialog content text
    expect(modalTextContent).toContain(`Are you sure you want to delete Cohort Review: ${newCohortReviewName}?`);

    // Verify Delete Cohort Review successful.
    expect(await DataResourceCard.findCard(page, newCohortReviewName, 5000)).toBeFalsy();
  });
});
