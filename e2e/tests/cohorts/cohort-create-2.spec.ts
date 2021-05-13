import { FilterSign } from 'app/page/cohort-participants-group';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { makeWorkspaceName } from 'utils/str-utils';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { MenuOption } from 'app/text-labels';
import ClrIconLink from 'app/element/clr-icon-link';
import ReviewCriteriaSidebar from 'app/component/review-criteria-sidebar';

describe('Create Cohorts from Domain criteria', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = makeWorkspaceName();

  test('Create cohort from EKG conditions', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    // Add Include Participants Group 1: Add a Condition
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.clickCriteriaMenuItems([MenuOption.Conditions]);

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
    const addIcon = ClrIconLink.findByName(page, { containsText: nameValue, iconShape: 'plus-circle' }, searchResultsTable);
    await addIcon.click();
    console.log(`Added condition: ${nameValue}`);

    await group1.clickFinishAndReviewButton();

    // Add Condition Modifiers: Age At Event >= 50
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

  xtest('Create cohort from hydroxychloroquine drug', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    // Include Participants Group 1: Add first drug.
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeDrugs('hydroxychloroquine', 1);
    // Include Participants Group 1: Add a second drug.
    await group1.includeDrugs('Hydrocodone', 1);

    // Save new cohort
    await cohortBuildPage.createCohort();

    await new CohortActionsPage(page).deleteCohort();
  });

  xtest('Create cohort from colonoscopy procedures', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    // Include Participants Group 1: Add first drug.
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeProcedures('Colonoscopy');

    // Save new cohort
    await cohortBuildPage.createCohort();

    await new CohortActionsPage(page).deleteCohort();
  });

  xtest('Create cohort from Red cell indices labs and measurements', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    const cohortBuildPage = await dataPage.clickAddCohortsButton();

    // Include Participants Group 1: Add first drug.
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeLabsAndMeasurements('Red cell indices');

    // Save new cohort
    await cohortBuildPage.createCohort();

    await new CohortActionsPage(page).deleteCohort();
  });
});
