import WorkspaceDataPage from 'app/page/workspace-data-page';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import CohortBuildPage, { FieldSelector } from 'app/page/cohort-build-page';
import { makeRandomName, makeWorkspaceName, numericalStringToNumber } from 'utils/str-utils';
import { PhysicalMeasurementsCriteria } from 'app/page/criteria-search-page';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import CohortReviewModal from 'app/modal/cohort-review-modal';
import { LinkText, MenuOption, ResourceCard } from 'app/text-labels';
import CohortReviewPage from 'app/page/cohort-review-page';
import DataResourceCard from 'app/component/data-resource-card';
import Link from 'app/element/link';

describe('Editing Cohort tests', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Tests require one and same workspace
  const workspace = makeWorkspaceName();
  let workspaceUrl;

  test('Discard changes in cohort', async () => {
    await findAWorkspace();

    const dataPage = new WorkspaceDataPage(page);
    const cohortCard = await dataPage.findOrCreateCohort();
    const cohortName = await cohortCard.getResourceName();
    await cohortCard.clickResourceName();

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Wait for Total Count that also indicates page load is ready.
    numericalStringToNumber(await cohortBuildPage.getTotalCount());

    // Switch on Temporal in Group 1.
    const group1 = cohortBuildPage.findIncludeParticipantsGroupByIndex();
    await group1.clickTemporalSwitch(true);

    // Click Data tab, Warning (Discard Changes) modal should open.
    await dataPage.openDataPage({ waitPageChange: false });

    await cohortBuildPage.discardChangesConfirmationDialog();

    // Back to Data page.
    await dataPage.waitForLoad();
    const resourceCard = new DataResourceCard(page);
    expect(await resourceCard.findCard(cohortName)).toBeTruthy();
  });

  test('Rename group in cohort', async () => {
    const cohortName = await setUpWorkspaceAndCohort().then((cohort: DataResourceCard) => cohort.clickResourceName());

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Save Total Count for comparison later
    const totalCount = numericalStringToNumber(await cohortBuildPage.getTotalCount());

    // Edit Group 1 name successfully
    const newName = makeRandomName();
    const group1 = cohortBuildPage.findIncludeParticipantsGroupByIndex();
    await group1.editGroupName(newName);

    // Check new named group
    const groupName = cohortBuildPage.findIncludeParticipantsGroup(newName);
    expect(await groupName.exists()).toBe(true);

    // Check Total Count is unaffected by group name rename
    const newTotalCount = numericalStringToNumber(await cohortBuildPage.getTotalCount());
    expect(newTotalCount).toBe(totalCount);

    await cohortBuildPage.saveChanges();

    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();
    expect(await page.title()).toContain('Cohort Actions');

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.openDataPage();
    await dataPage.waitForLoad();
    await dataPage.deleteResource(cohortName, ResourceCard.Cohort);
  });

  test('Insert new group in cohort', async () => {
    const cohortName = await setUpWorkspaceAndCohort().then((cohort: DataResourceCard) => cohort.clickResourceName());

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();
    await cohortBuildPage.getTotalCount();

    // Insert new group in Include Participants
    const newGroup = cohortBuildPage.findIncludeParticipantsEmptyGroup();
    await newGroup.includePhysicalMeasurement(PhysicalMeasurementsCriteria.Weight, { filterValue: 200 });

    // Check Total Count and new Total Count is different
    const newTotalCount = numericalStringToNumber(await cohortBuildPage.getTotalCount());
    expect(newTotalCount).toBeGreaterThan(1);

    await cohortBuildPage.saveChanges();

    // Should land on Cohort Actions page
    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();

    await waitForText(page, 'Cohort Saved Successfully', { css: 'body h3' });
    const cohortLink = await page.waitForXPath(`//a[normalize-space()="${cohortName}"]`, { visible: true });
    expect(cohortLink).toBeTruthy();

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.openCohortsSubtab();
    await waitWhileLoading(page);

    await dataPage.deleteResource(cohortName, ResourceCard.Cohort);
  });

  test('Create cohort Review Set', async () => {
    const cohortName = await setUpWorkspaceAndCohort().then((cohort: DataResourceCard) => cohort.clickResourceName());

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    const reviewSetsButton = await cohortBuildPage.getCopyButton();
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
    const backToCohortButton = await cohortReviewPage.getBackToCohortButton();
    await backToCohortButton.click();

    await cohortBuildPage.waitForLoad();
    await cohortBuildPage.getTotalCount();

    // Back out to Data page
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.openDataPage();
    await dataPage.waitForLoad();

    // Verify Cohort Review card exists
    const resourceCard = new DataResourceCard(page);
    const reviewCohortCard = await resourceCard.findCard(cohortName, ResourceCard.CohortReview);
    expect(reviewCohortCard).toBeTruthy();

    await dataPage.deleteResource(cohortName, ResourceCard.CohortReview);
    await dataPage.deleteResource(cohortName, ResourceCard.Cohort);
  });

  test('Delete cohort', async () => {
    const cohortName = await setUpWorkspaceAndCohort().then((cohort: DataResourceCard) => cohort.clickResourceName());

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Delete cohort while inside the Cohort Build page
    const modalContent = await cohortBuildPage.deleteCohort();
    expect(modalContent).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);

    // Back to the Data page.
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    const resourceCard = new DataResourceCard(page);
    expect(await resourceCard.findCard(cohortName)).toBeFalsy();
  });

  test('Save as cohort', async () => {
    const originalCohortName = await setUpWorkspaceAndCohort().then((cohort: DataResourceCard) =>
      cohort.clickResourceName()
    );

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Save Total Count for comparison.
    const totalCount = await cohortBuildPage.getTotalCount();

    // Save as.
    const cohortName = await cohortBuildPage.saveChanges(MenuOption.SaveAs);
    console.log(`Saved as Cohort: "${cohortName}"`);

    // Click new cohort name link. Open Cohort Build page.
    const cohortLink = Link.findByName(page, { name: cohortName });
    await cohortLink.clickAndWait();

    // Total Count should be unchanged.
    await waitForText(page, totalCount, { xpath: FieldSelector.TotalCount }, 60000);

    // Delete cohort while inside the Cohort Build page
    const modalContent = await cohortBuildPage.deleteCohort();
    expect(modalContent).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);

    // Back to the Data page.
    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    const resourceCard = new DataResourceCard(page);
    // Save as cohort is gone.
    expect(await resourceCard.findCard(cohortName)).toBeFalsy();
    // Original cohort remains.
    expect(await resourceCard.findCard(originalCohortName)).toBeTruthy();
  });

  // Helper functions
  async function findAWorkspace(): Promise<void> {
    if (workspaceUrl) {
      // Faster: Load previously saved URL instead clicks thru pages to open workspace data page.
      await page.goto(workspaceUrl, { waitUntil: ['load', 'domcontentloaded', 'networkidle0'] });
      return;
    }
    await findOrCreateWorkspace(page, { workspaceName: workspace });
    workspaceUrl = page.url();
  }

  async function setUpWorkspaceAndCohort(): Promise<DataResourceCard> {
    await findAWorkspace();
    await deleteAllCohort('Duplicate');
    const dataPage = new WorkspaceDataPage(page);
    const cohortCard = await dataPage.findOrCreateCohort();
    return duplicateCohort(cohortCard);
  }

  // Make a duplicate of an existing cohort to avoid hitting issue of concurrent data editing.
  async function duplicateCohort(cohortCard: DataResourceCard): Promise<DataResourceCard> {
    const cohortName = await cohortCard.getResourceName();
    const snowmanMenu = await cohortCard.getSnowmanMenu();
    await snowmanMenu.select(MenuOption.Duplicate, { waitForNav: false });
    await waitWhileLoading(page);
    const duplicateCohortName = `Duplicate of ${cohortName}`;
    const dataPage = new WorkspaceDataPage(page);
    // Rename duplicate Cohort name
    const newCohortName = makeRandomName();
    await dataPage.renameResource(duplicateCohortName, newCohortName, ResourceCard.Cohort);
    // Verify rename successful.
    expect(await DataResourceCard.findCard(page, newCohortName)).toBeTruthy();
    return dataPage.findCohortCard(newCohortName);
  }

  // Delete existing Cohorts with name contains 'Duplicate' string.
  async function deleteAllCohort(pattern: string): Promise<void> {
    const dataResourceCard = new DataResourceCard(page);
    const cards = await dataResourceCard.getResourceCard(ResourceCard.Cohort);

    const cardNames = [];
    for (const card of cards) {
      const name = await card.getResourceName();
      if (name.includes(pattern)) {
        cardNames.push(await card.getResourceName());
      }
    }

    for (const name of cardNames) {
      const modalTextContent = await new WorkspaceDataPage(page).deleteResource(name, ResourceCard.Cohort);
      // Verify Delete Confirmation modal text
      expect(modalTextContent).toContain(`Are you sure you want to delete Cohort: ${name}?`);
      expect(await DataResourceCard.findCard(page, name, 2000)).toBeFalsy();
      console.log(`deleted cohort ${name}`);
    }
  }
});
