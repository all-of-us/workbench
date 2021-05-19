import { FilterSign, PhysicalMeasurementsCriteria, Race, Sex, Visits } from 'app/page/cohort-participants-group';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { makeWorkspaceName } from 'utils/str-utils';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { Ethnicity } from 'app/page/cohort-participants-group';
import { LinkText, MenuOption, ResourceCard } from 'app/text-labels';
import CohortBuildPage from 'app/page/cohort-build-page';
import ClrIconLink from 'app/element/clr-icon-link';
import ReviewCriteriaSidebar from 'app/component/review-criteria-sidebar';

describe('Create Cohorts from Program Data criteria', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = makeWorkspaceName();

  test('Discard changes', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.clickAddCohortsButton();

    // Landing in Build Cohort Criteria page.
    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Copy button is not found.
    expect(await cohortBuildPage.getCopyButton().exists()).toBeFalsy();
    // Trash (Delete) button is not found.
    expect(await cohortBuildPage.getDeleteButton().exists()).toBeFalsy();
    // Export button is not found.
    expect(await cohortBuildPage.getExportButton().exists()).toBeFalsy();
    // Create Cohort button is not found.
    expect(await cohortBuildPage.getCreateCohortButton().exists()).toBeFalsy();

    // Include Participants Group 1.
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.addCriteria([MenuOption.PhysicalMeasurements]);
    let addIcon = ClrIconLink.findByName(page, {
      name: PhysicalMeasurementsCriteria.WheelChairUser,
      iconShape: 'plus-circle',
      ancestorLevel: 2
    });
    await addIcon.click();
    const message = await group1.criteriaAddedMessage();
    expect(message).toEqual('Criteria Added');
    await group1.finishAndReviewButton();

    const reviewCriteriaSidebar = new ReviewCriteriaSidebar(page);
    await reviewCriteriaSidebar.waitUntilVisible();

    // Remove Selected Criteria in sidebar.
    await reviewCriteriaSidebar.removeSelectedCriteria(PhysicalMeasurementsCriteria.WheelChairUser);

    // Add a different criteria.
    addIcon = ClrIconLink.findByName(page, {
      name: PhysicalMeasurementsCriteria.PregnantEnrollment,
      iconShape: 'plus-circle',
      ancestorLevel: 2
    });
    await addIcon.click();
    expect(Number(await reviewCriteriaSidebar.getCriteriaCount())).toEqual(1);

    // Click Back button to close sidebar.
    await reviewCriteriaSidebar.clickButton(LinkText.Back);
    await reviewCriteriaSidebar.waitUntilClose();

    // Click Data tab, Warning (Discard Changes) modal should open. Finish discarding changes.
    await dataPage.openDataPage({ waitPageChange: false });
    const warning = await cohortBuildPage.discardChangesConfirmationDialog();
    const expectedWarningText =
      'Your cohort has not been saved. If you’d like to save your cohort criteria,' +
      ' please click CANCEL and save your changes';

    const foundMatch = warning.some((item) => item.indexOf(expectedWarningText) !== -1);
    expect(foundMatch).toBe(true);

    // Changes are discarded, back to the Data page.
    await dataPage.waitForLoad();
  });

  // Add new cohort includes 4 categories: BMI; Weight; Height; Blood Pressure Hypotensive.
  // There are more physical measurements categories but they are not tested here.
  test('Create cohort from physical measurement criteria', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

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
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    // Include Group 1: Demographics Age range: 21 - 90.
    const minAge = 21;
    const maxAge = 90;
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeAge(minAge, maxAge);

    const group1Count = await group1.getGroupCount();
    const totalCount = await cohortBuildPage.getTotalCount();

    expect(group1Count).toEqual(totalCount);

    // Include Group 2: Demographics Deceased.
    const group2 = cohortBuildPage.findIncludeParticipantsGroup('Group 2');
    await group2.includeDemographicsDeceased();
    const group2Count = await group2.getGroupCount();
    expect(Number.isNaN(group2Count)).toBe(false);

    // Include Group 3: Demographics Ethnicity.
    const group3 = cohortBuildPage.findIncludeParticipantsGroup('Group 3');
    // Choose all ethnicities.
    await group3.includeEthnicity([
      Ethnicity.HispanicOrLatino,
      Ethnicity.NotHispanicOrLatino,
      Ethnicity.RaceEthnicityNoneOfThese,
      Ethnicity.PreferNotToAnswer,
      Ethnicity.Skip
    ]);
    const group3Count = await group3.getGroupCount();
    expect(Number.isNaN(group3Count)).toBe(false);

    // Include Group 4: Demographics Gender Identity.
    const group4 = cohortBuildPage.findIncludeParticipantsGroup('Group 4');
    await group4.includeGenderIdentity([Sex.FEMALE]);
    await group4.getGroupCount();
    expect(Number.isNaN(await group4.getGroupCount())).toBe(false);

    // Include Group 5: Demographics Race.
    const group5 = cohortBuildPage.findIncludeParticipantsGroup('Group 5');
    await group5.includeRace([Race.WHITE, Race.BLACK, Race.UNKNOWN]);
    await group5.getGroupCount();

    expect(Number.isNaN(await cohortBuildPage.getTotalCount())).toBe(false);

    // Include Group 6: Demographics Sex Assigned at Birth. Include all choices.
    const group6 = cohortBuildPage.findIncludeParticipantsGroup('Group 6');
    await group6.includeSexAssignedAtBirth([Sex.UNKNOWN, Sex.FEMALE, Sex.SKIPPED, Sex.MALE]);

    // Save new cohort.
    const cohortName = await cohortBuildPage.createCohort();

    // Verify cohort created successfully.
    await dataPage.openDataPage({ waitPageChange: true });
    await dataPage.waitForLoad();

    // In Show All tab, new cohort card is found.
    const cohortCard = await dataPage.findCohortCard(cohortName);
    expect(await cohortCard.getResourceName()).toEqual(cohortName);

    // Delete cohort in Workspace Data page.
    await dataPage.deleteResource(cohortName, ResourceCard.Cohort);
    expect(await dataPage.findCohortCard(cohortName)).toBeFalsy();
  });

  test('Create cohort from whole genome variant', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeWholeGenomeVariant();
    const group1Count = await group1.getGroupCount();
    const totalCount = await cohortBuildPage.getTotalCount();
    expect(group1Count).toEqual(totalCount);
    expect(Number.isNaN(group1Count)).toBe(false);

    // Save new cohort.
    await cohortBuildPage.createCohort();

    // Delete cohort in Cohort Build page.
    await new CohortActionsPage(page).deleteCohort();
  });

  // Include all visit types in a single Include Group.
  test('Create cohort from visits', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeVisits([
      Visits.AmbulanceVisit,
      Visits.AmbulatoryClinicCenter,
      Visits.AmbulatoryRehabilitationVisit,
      Visits.EmergencyRoomVisit,
      Visits.EmergencyRoomAndInpatientVisit,
      Visits.HomeVisit,
      Visits.InpatientVisit,
      Visits.LaboratoryVisit,
      Visits.NonhospitalInstitutionVisit,
      Visits.OfficeVisit,
      Visits.OutpatientVisit,
      Visits.PharmacyVisit
    ]);
    const group1Count = await group1.getGroupCount();
    const totalCount = await cohortBuildPage.getTotalCount();
    expect(group1Count).toEqual(totalCount);
    expect(Number.isNaN(group1Count)).toBe(false);

    // Save new cohort.
    await cohortBuildPage.createCohort();

    // Delete cohort in Cohort Build page.
    await new CohortActionsPage(page).deleteCohort();
  });

  test('Create cohort from EKG conditions with modifiers', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

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
});
