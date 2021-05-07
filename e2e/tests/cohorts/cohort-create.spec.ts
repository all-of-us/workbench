import { FilterSign, PhysicalMeasurementsCriteria } from 'app/page/criteria-search-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { numericalStringToNumber } from 'utils/str-utils';
import CohortActionsPage from "../../app/page/cohort-actions-page";

describe('Cohorts', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = 'e2eCreateCohortsTest123delete';

  test('Create cohort from Physical Measurement Criteria', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    // Add new cohort by adding 4 categories: BMI, Weight, Height and Blood Pressure Hypotensive.
    // There are more physical measurements categories which are not covered here.
    const dataPage = new WorkspaceDataPage(page);
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    // Group 1: Physical Measurements BMI
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includePhysicalMeasurement(PhysicalMeasurementsCriteria.BMI, {
      filterSign: FilterSign.GreaterThanOrEqualTo,
      filterValue: 30
    });

    // Checking Group 1 count: should be numeric.
    const group1Count = numericalStringToNumber(await group1.getGroupCount());
    expect(Number.isNaN(group1Count)).toBe(false);
    expect(group1Count).toBeGreaterThan(1);

    // Group 2: Physical Measurements Weight
    const group2 = cohortBuildPage.findIncludeParticipantsGroup('Group 2');
    await group2.includePhysicalMeasurement(PhysicalMeasurementsCriteria.Weight, {
      filterSign: FilterSign.LessThanOrEqualTo,
      filterValue: 300
    });

    // Checking Group 2 count: should be numeric.
    const group2Count = numericalStringToNumber(await group2.getGroupCount());
    expect(Number.isNaN(group2Count)).toBe(false);
    expect(group2Count).toBeGreaterThan(1);

    // Group 3: Physical Measurements Height
    const group3 = cohortBuildPage.findIncludeParticipantsGroup('Group 3');
    await group3.includePhysicalMeasurement(PhysicalMeasurementsCriteria.Height, {
      filterSign: FilterSign.LessThanOrEqualTo,
      filterValue: 200
    });

    // Checking Group 3 count: should be numeric.
    const group3Count = numericalStringToNumber(await group3.getGroupCount());
    expect(Number.isNaN(group3Count)).toBe(false);
    expect(group3Count).toBeGreaterThan(1);

    // Group 4: Physical Measurements Blood Pressure Hypotensive
    const group4 = cohortBuildPage.findIncludeParticipantsGroup('Group 4');
    await group4.includePhysicalMeasurement(PhysicalMeasurementsCriteria.BPHypotensive);

    // Checking Group 4 count: should be numeric.
    const group4Count = numericalStringToNumber(await group4.getGroupCount());
    expect(Number.isNaN(group4Count)).toBe(false);
    expect(group4Count).toBeGreaterThan(1);

    // Checking Total Count: should be numeric.
    const totalCount = numericalStringToNumber(await cohortBuildPage.getTotalCount());
    expect(Number.isNaN(totalCount)).toBe(false);

    // Save new cohort.
    const cohortName = await cohortBuildPage.createCohort();

    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();
    await cohortActionsPage.clickCohortNameLink(cohortName);

    // Delete cohort.
    await cohortBuildPage.deleteCohort();
  });
});
