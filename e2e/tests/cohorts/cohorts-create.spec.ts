import { PhysicalMeasurementsCriteria } from 'app/page/criteria-search-page';
import Link from 'app/element/link';
import CohortBuildPage, { FieldSelector } from 'app/page/cohort-build-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { waitForText } from 'utils/waits-utils';

describe('Cohorts', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  /**
   * Test:
   * Find an existing workspace or create a new workspace if none exists.
   * Add criteria in Group 1: Physical Measurements criteria => BMI (>= 30).
   * Add criteria in Group 2: Demographics => Deceased.
   * Checking counts.
   * Renaming Group 1 and 2 names.
   */
  test('Create, edit and delete', async () => {
    const workspaceCard = await findOrCreateWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    // Wait for the Data page.
    const dataPage = new WorkspaceDataPage(page);

    // Click Add Cohorts button
    const addCohortsButton = await dataPage.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // In Build Cohort Criteria page.
    const cohortPage = new CohortBuildPage(page);
    await cohortPage.waitForLoad();

    // Include Participants Group 1.
    const group1 = cohortPage.findIncludeParticipantsGroup('Group 1');
    const group1Count = await group1.includePhysicalMeasurement(PhysicalMeasurementsCriteria.BMI, 30);

    // Checking Group 1 Count. should match Group 1 participants count.
    const group1CountInt = Number((await group1.getGroupCount()).replace(/,/g, ''));
    expect(group1CountInt).toBeGreaterThan(1);
    console.log('Group 1: Physical Measurement -> BMI count: ' + group1CountInt);

    // Checking Total Count: should match Group 1 participants count.
    const totalCount = await cohortPage.getTotalCount();
    expect(group1Count).toEqual(totalCount);

    // Include Participants Group 2: Select menu Demographics -> Deceased
    const group2 = cohortPage.findIncludeParticipantsGroup('Group 2');
    const group2Count = await group2.includeDemographicsDeceased();
    const group2CountInt = Number(group2Count.replace(/,/g, ''));
    expect(group2CountInt).toBeGreaterThan(1);
    console.log('Group 2: Demographics -> Deceased count: ' + group2CountInt);

    // Compare the new Total Count with the old Total Count.
    const newTotalCount = await cohortPage.getTotalCount();
    const newTotalCountInt = Number(newTotalCount.replace(/,/g, ''));
    // Adding additional group decreased Total Count.
    expect(newTotalCountInt).toBeLessThan(group1CountInt);
    console.log('New Total Count: ' + newTotalCountInt);

    // Save new cohort - click create cohort button
    const cohortName = await cohortPage.saveCohortAs();
    console.log(`Created Cohort "${cohortName}"`);

    // Open Cohort details.
    const cohortLink = await Link.findByName(page, { name: cohortName });
    await cohortLink.clickAndWait();
    await waitForText(page, newTotalCount, { xpath: FieldSelector.TotalCount }, 60000);

    // Modify Cohort: Edit Group 1 name successfully.
    const newName1 = 'Group 1: BMI';
    await group1.editGroupName(newName1);
    // Check new named group
    const groupBMI = cohortPage.findIncludeParticipantsGroup(newName1);
    expect(await groupBMI.exists()).toBe(true);

    // Modify Cohort: Edit Group 2 name successfully.
    const newName2 = 'Group 2: Deceased';
    await group2.editGroupName(newName2);
    // Check new name
    const groupDeceased = cohortPage.findIncludeParticipantsGroup(newName2);
    expect(await groupDeceased.exists()).toBe(true);

    // Check Total Count is unaffected by group name rename.
    const newTotalCount2 = await cohortPage.getTotalCount();
    const newTotalCountInt2 = Number(newTotalCount2.replace(/,/g, ''));
    expect(newTotalCountInt2).toBe(newTotalCountInt);

    // Clean up: delete cohort
    const modalContent = await cohortPage.deleteCohort();
    // Verify dialog content text
    expect(modalContent).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);
    console.log(`Deleted Cohort "${cohortName}"`);
  });
});
