import {
  Ethnicity,
  FilterSign,
  GenderIdentity,
  PhysicalMeasurementsCriteria,
  Race,
  Sex
} from 'app/page/cohort-participants-group';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { findOrCreateWorkspace, openTab, signInWithAccessToken } from 'utils/test-utils';
import { makeWorkspaceName } from 'utils/str-utils';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { MenuOption, ResourceCard, Tabs } from 'app/text-labels';
import ClrIconLink from 'app/element/clr-icon-link';
import ReviewCriteriaSidebar from 'app/sidebar/review-criteria-sidebar';
import { Page } from 'puppeteer';
import { getPropValue } from 'utils/element-utils';

describe.skip('Create Cohorts Test', () => {
  const workspaceName = makeWorkspaceName();
  let workspaceUrl: string;

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Add new cohort includes 4 categories: BMI; Weight; Height; Blood Pressure Hypotensive.
  // There are more physical measurements categories but they are not tested here.
  test('Create cohort from physical measurement criteria', async () => {
    await loadWorkspaceUrl(page, workspaceName);

    const dataPage = new WorkspaceDataPage(page);
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    // Group 1: Physical Measurements BMI.
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includePhysicalMeasurement([PhysicalMeasurementsCriteria.BMI], {
      filterSign: FilterSign.GreaterThanOrEqualTo,
      filterValue: 30
    });

    // Checking Group 1 count: should be numeric and greater than 1.
    const group1Count = await group1.getGroupCount();
    expect(Number.isNaN(group1Count)).toBe(false);
    expect(group1Count).toBeGreaterThan(1);

    // Before add a second group, perform basic UI checks.
    // Copy button exists but is disabled
    expect(await cohortBuildPage.getCopyButton().isDisabled()).toBe(true);
    // Trash (Delete) button exists but is disabled
    expect(await cohortBuildPage.getDeleteButton().isDisabled()).toBe(true);
    // Export button exists but is disabled
    expect(await cohortBuildPage.getExportButton().isDisabled()).toBe(true);
    // "Results by" REFRESH button is disabled
    expect(await cohortBuildPage.findRefreshButton().isCursorNotAllowed()).toBe(true);
    // Include Group 2 is visible and empty.
    const group2 = cohortBuildPage.findIncludeParticipantsGroup('Group 2');
    expect((await group2.findGroupCriteriaList()).length).toBe(0);
    // Exclude Participants Group 3 is visible and empty.
    const excludeGroup = cohortBuildPage.findExcludeParticipantsGroup('Group 3');
    expect((await excludeGroup.findGroupCriteriaList()).length).toBe(0);

    // Add Group 2: Physical Measurements Weight.
    await group2.includePhysicalMeasurement([PhysicalMeasurementsCriteria.Weight], {
      filterSign: FilterSign.LessThanOrEqualTo,
      filterValue: 200
    });

    // Checking Group 2 count: should be numeric and greater than 1.
    const group2Count = await group2.getGroupCount();
    expect(Number.isNaN(group2Count)).toBe(false);
    expect(group2Count).toBeGreaterThan(1);

    // Group 3: Physical Measurements Height.
    const group3 = cohortBuildPage.findIncludeParticipantsGroup('Group 3');
    await group3.includePhysicalMeasurement([PhysicalMeasurementsCriteria.Height], {
      filterSign: FilterSign.LessThanOrEqualTo,
      filterValue: 182 // unit is cm. 182 cm == 6 ft.
    });

    // Checking Group 3 count: should be numeric.
    const group3Count = await group3.getGroupCount();
    expect(Number.isNaN(group3Count)).toBe(false);
    expect(group3Count).toBeGreaterThan(1);

    // Group 4: Physical Measurements Blood Pressure Hypotensive.
    const group4 = cohortBuildPage.findIncludeParticipantsGroup('Group 4');
    await group4.includePhysicalMeasurement([PhysicalMeasurementsCriteria.BPHypotensive]);

    // Checking Group 4 count: should be numeric.
    const group4Count = await group4.getGroupCount();
    expect(Number.isNaN(group4Count)).toBe(false);
    expect(group4Count).toBeGreaterThan(1);

    // Checking Total Count: should be numeric.
    const totalCount = await cohortBuildPage.getTotalCount();
    expect(Number.isNaN(totalCount)).toBe(false);

    // Save new cohort.
    await cohortBuildPage.createCohort();

    // Delete cohort in Cohort Build page.
    await new CohortActionsPage(page).deleteCohort();
  });

  test('Create cohort from demographics criteria', async () => {
    await loadWorkspaceUrl(page, workspaceName);

    const dataPage = new WorkspaceDataPage(page);
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    // Include Group 1: Demographics Deceased AND Demographics Age range: 21 - 90.
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    const minAge = 21;
    const maxAge = 90;
    await group1.includeAge(minAge, maxAge);
    await group1.includeDemographicsDeceased();
    const group1Count = await group1.getGroupCount();
    let totalCount = await cohortBuildPage.getTotalCount();

    expect(Number.isNaN(group1Count)).toBe(false);
    expect(group1Count).toEqual(totalCount);

    // Include Group 2: Demographics Ethnicity.
    const group2 = cohortBuildPage.findIncludeParticipantsGroup('Group 2');
    // Choose all ethnicities.
    await group2.includeEthnicity([
      Ethnicity.HispanicOrLatino,
      Ethnicity.NotHispanicOrLatino,
      Ethnicity.RaceEthnicityNoneOfThese,
      Ethnicity.PreferNotToAnswer,
      Ethnicity.Skip
    ]);
    const group2Count = await group2.getGroupCount();
    expect(Number.isNaN(group2Count)).toBe(false);

    // Include Group 3: Demographics Gender Identity.
    const group3 = cohortBuildPage.findIncludeParticipantsGroup('Group 3');
    await group3.includeGenderIdentity([GenderIdentity.WOMAN]);
    await group3.getGroupCount();
    expect(Number.isNaN(await group3.getGroupCount())).toBe(false);

    // Include Group 4: Demographics Race.
    const group4 = cohortBuildPage.findIncludeParticipantsGroup('Group 4');
    await group4.includeRace([Race.WHITE, Race.BLACK, Race.UNKNOWN]);
    await group4.getGroupCount();

    totalCount = await cohortBuildPage.getTotalCount();
    expect(Number.isNaN(totalCount)).toBe(false);

    // Include Group 5: Demographics Sex Assigned at Birth. Include all choices.
    const group5 = cohortBuildPage.findIncludeParticipantsGroup('Group 5');
    await group5.includeSexAssignedAtBirth([Sex.UNKNOWN, Sex.FEMALE, Sex.SKIPPED, Sex.MALE]);

    // Save new cohort.
    const cohortName = await cohortBuildPage.createCohort();

    // Verify cohort created successfully.
    await openTab(page, Tabs.Data, dataPage);

    // In Show All tab, new cohort entry is found in table.
    const cohortNameCell = await dataPage.findCohortEntry(cohortName);
    const cohortCellValue = await getPropValue<string>(cohortNameCell, 'textContent');
    expect(cohortCellValue).toEqual(cohortName);

    // Delete cohort in Workspace Data page.
    await dataPage.deleteResourceFromTable(cohortName, ResourceCard.Cohort);
    expect(await dataPage.findCohortEntry(cohortName)).toBeNull();
  });

  test('Create cohort from EKG conditions with modifiers', async () => {
    await loadWorkspaceUrl(page, workspaceName);

    const dataPage = new WorkspaceDataPage(page);
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    // Add Include Participants Group 1: Add a Condition
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.addCriteria([MenuOption.Conditions]);
    // First, search for non-existent condition, expect search returns no results.
    let searchResultsTable = await group1.searchCriteria('allergist');
    expect(await searchResultsTable.exists()).toBe(false);
    // Next, search for condition EKG
    searchResultsTable = await group1.searchCriteria('EKG');

    // Check cell value in column "Code" (column #2)
    const codeValue = await searchResultsTable.getCellValue(1, 2);
    expect(Number(codeValue)).not.toBeNaN();

    // Add the condition in first row. We don't know what the condition name is, so we get the cell value first.
    const nameValue = await searchResultsTable.getCellValue(1, 1);
    const addIcon = ClrIconLink.findByName(
      page,
      { containsText: nameValue, iconShape: 'plus-circle' },
      searchResultsTable
    );
    await addIcon.click();
    await group1.finishAndReviewButton();

    // Add a Condition Modifier: Age At Event >= 50
    const helpSidebar = new ReviewCriteriaSidebar(page);
    await helpSidebar.waitUntilVisible();
    await helpSidebar.addAgeModifier(FilterSign.GreaterThanOrEqualTo, 50);

    // Click SAVE CRITERIA button. Sidebar closes.
    await helpSidebar.clickSaveCriteriaButton();

    // Check Group 1 Count.
    const group1Count = await group1.getGroupCount();
    expect(group1Count).toBeGreaterThan(1);
    expect(Number.isNaN(group1Count)).toBe(false);

    // Check Total Count.
    const totalCount = await cohortBuildPage.getTotalCount();
    expect(totalCount).toBe(group1Count);

    // Save new cohort
    await cohortBuildPage.createCohort();

    // Delete cohort in Cohort Build page.
    await new CohortActionsPage(page).deleteCohort();
  });

  // Helper functions
  async function loadWorkspaceUrl(page: Page, workspaceName: string): Promise<void> {
    if (workspaceUrl) {
      // Faster: Load previously saved URL instead clicks thru links to open workspace data page.
      await page.goto(workspaceUrl, { waitUntil: ['load', 'networkidle0'] });
      return;
    }
    await findOrCreateWorkspace(page, { workspaceName });
    workspaceUrl = page.url(); // Save URL for load workspace directly without search.
  }
});
