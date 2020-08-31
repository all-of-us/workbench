import {findWorkspace, isValidDate, signIn, waitWhileLoading} from 'utils/test-utils';
import {EllipsisMenuAction, LinkText} from 'app/text-labels';
import CohortBuildPage from 'app/page/cohort-build-page';
import CohortParticipantDetailPage from 'app/page/cohort-participant-detail-page';
import CohortReviewModal from 'app/page/cohort-review-modal';
import CohortReviewPage from 'app/page/cohort-review-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {waitForText} from 'utils/waits-utils';
import {getPropValue} from 'utils/element-utils';

describe('Cohort review tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * Find an existing workspace or create a new workspace if none exists.
   * Create a new Cohort from Criteria: Visits -> Out-Patient Visit.
   * Create a Review Set for 100 participants.
   * Verify some UI.
   * Delete Cohort in Cohort Build page.
   */
  test('Create Cohort and a Review Set for 100 participants', async () => {
    const reviewSetNumberOfParticipants = 100;

    await findWorkspace(page).then(card => card.clickWorkspaceName());

    const dataPage = new WorkspaceDataPage(page);
    const cohortCard = await dataPage.createCohort();
    const cohortName = await cohortCard.getResourceName();
    console.log(`Created Cohort: "${cohortName}"`);

    await cohortCard.clickEllipsisAction(EllipsisMenuAction.Review);
    const modal = new CohortReviewModal(page);
    await modal.fillInNumberOfPartcipants(reviewSetNumberOfParticipants);
    await modal.clickButton(LinkText.CreateSet);
    console.log(`Created Review Set with ${reviewSetNumberOfParticipants} participants.`);

    const cohortReviewPage = new CohortReviewPage(page);
    await cohortReviewPage.waitForLoad();
    await waitForText(page, `Review Sets for ${cohortName}`);

    // Verify table pagination records count.
    const participantsTable = cohortReviewPage.getDataTable();
    const records = await participantsTable.getNumRecords();
    // Table records page numbering is in "1 - 25 of 100 records" format.
    expect(Number(records[0])).toEqual(1);
    expect(Number(records[1])).toEqual(25);
    expect(Number(records[2])).toEqual(reviewSetNumberOfParticipants);

    // Verify table column names match.
    const columns = ['Participant ID', 'Date of Birth', 'Deceased', 'Sex at Birth', 'Gender', 'Race', 'Ethnicity', 'Status'];
    const columnNames = await participantsTable.getColumnNames();
    expect(columnNames).toHaveLength(columns.length);
    expect(columnNames.sort()).toEqual(columns.sort());

    // Get Date of Birth in row 2.
    const dobCell = await participantsTable.getCell(2, 2);
    const cellValue = await getPropValue<string>(dobCell, 'textContent');
    // Check birth date is valid format.
    isValidDate(cellValue);

    // Check table row link navigation works. Click ParticipantId link in the second row.
    await cohortReviewPage.clickParticipantLink(2);

    // Not checking anything in Participant Detail page.
    const participantDetailPage = new CohortParticipantDetailPage(page);
    await participantDetailPage.waitForLoad();
    // Page navigate back.
    await participantDetailPage.getBackToReviewSetButton().then(btn => btn.click());
    await waitWhileLoading(page);

    // Click ParticipantId link in the fifth row
    await cohortReviewPage.clickParticipantLink(5);
    await participantDetailPage.waitForLoad();
    await participantDetailPage.getBackToReviewSetButton().then(btn => btn.click());
    await waitWhileLoading(page);

    await cohortReviewPage.getBackToCohortButton().then(btn => btn.clickAndWait());

    // Land on Cohort Build page
    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Delete cohort.
    await cohortBuildPage.deleteCohort();
    // Land in Data page.
    await dataPage.waitForLoad();
    console.log(`Deleted Cohort: "${cohortName}"`);
  });

});
