import ReviewCriteriaSidebar from 'app/component/review-criteria-sidebar';
import { FilterSign } from 'app/page/criteria-search-page';
import DataResourceCard from 'app/component/data-resource-card';
import ClrIconLink from 'app/element/clr-icon-link';
import { MenuOption, ResourceCard } from 'app/text-labels';
import CohortBuildPage from 'app/page/cohort-build-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import CohortActionsPage from 'app/page/cohort-actions-page';
import { Ethnicity } from 'app/page/cohort-search-page';

describe('Cohorts', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = 'e2eCohortsCloneTest';
  /**
   * Test:
   * Find an existing workspace.
   * Create new cohort with Condition = EKG.
   * Check Group and Total Count.
   * Edit Cohort
   * Duplicate cohort.
   * Delete duplicate cohort via ellipsis menu
   * Check cohort open okay.
   * Delete cohort via delete/trash icon on cohort build page
   */
  test('Create, duplicate and delete', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    // Wait for the Data page.
    const dataPage = new WorkspaceDataPage(page);

    // Save url
    const workspaceDataUrl = page.url();

    // Click Add Cohorts button
    const addCohortsButton = dataPage.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // Create new Cohort
    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Include Participants Group 1: Add a Condition
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    const searchPage = await group1.includeConditions();

    // First, search for non-existent condition, expect returns no results.
    const search1ResultsTable = await searchPage.searchCriteria('allergist');
    expect(await search1ResultsTable.exists()).toBe(false);

    // Next, search for condition EKG
    const search2ResultsTable = await searchPage.searchCriteria('EKG');
    // Check cell value in column "Code" (column #2)
    const codeValue = await search2ResultsTable.getCellValue(1, 2);
    expect(Number(codeValue)).not.toBeNaN();

    // Add the condition in first row. We don't know what the condition name is, so we get the cell value first.
    const nameValue = await search2ResultsTable.getCellValue(1, 1);
    const addIcon = ClrIconLink.findByName(
      page,
      { containsText: nameValue, iconShape: 'plus-circle' },
      search2ResultsTable
    );
    await addIcon.click();

    // Click Finish & Review button to open selection list and add modifier
    await searchPage.clickFinishAndReviewButton();

    // Add Condition Modifiers: Age At Event >= 50
    const helpSidebar = new ReviewCriteriaSidebar(page);
    await helpSidebar.addAgeModifier(FilterSign.GreaterThanOrEqualTo, 50);

    // Click SAVE CRITERIA button. Sidebar closes.
    await helpSidebar.clickSaveCriteriaButton();

    // Check Group 1 Count.
    const group1Count = await group1.getGroupCount();
    const group1CountInt = Number(group1Count.replace(/,/g, ''));
    expect(group1CountInt).toBeGreaterThan(1);
    console.log(`Group 1: ${group1CountInt}`);

    // Check Total Count.
    const totalCount = await cohortBuildPage.getTotalCount();
    const totalCountInt = Number(totalCount.replace(/,/g, ''));
    expect(totalCountInt).toBe(group1CountInt);
    console.log(`Total Count: ${totalCountInt}`);

    // Save new cohort - click Create Cohort button
    const cohortName = await cohortBuildPage.saveCohortAs();
    await waitForText(page, 'Cohort Saved Successfully');
    console.log(`Created Cohort "${cohortName}"`);

    // Navigate to the data page, find the new cohort.
    await page.goto(workspaceDataUrl);
    await dataPage.waitForLoad();
    let cohortCard = await DataResourceCard.findCard(page, `${cohortName}`);

    // Edit cohort using Ellipsis menu
    await cohortCard.selectSnowmanMenu(MenuOption.Edit, { waitForNav: false });
    await cohortBuildPage.waitForLoad();
    await waitWhileLoading(page);

    // Include Participants Group 1: Add a Condition
    const group1a = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    const ethnicityLookUp = await group1a.includeEthnicity();
    await ethnicityLookUp.addEthnicity([Ethnicity.HispanicOrLatino]);

    // Open selection list and click Save Criteria button
    await ethnicityLookUp.reviewAndSaveCriteria();

    // select SAVE option from the SAVE COHORT button drop-down menu
    await cohortBuildPage.saveChanges();

    // Should land on Cohorts Actions page
    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();

    // navigate back to the data page and open Cohorts sub tab
    await dataPage.openCohortsSubtab();
    await waitWhileLoading(page);

    // Duplicate cohort using Ellipsis menu.
    cohortCard = await DataResourceCard.findCard(page, `${cohortName}`);
    await cohortCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: false });
    await waitWhileLoading(page);

    const duplCohortName = `Duplicate of ${cohortName}`;
    console.log(`Duplicated Cohort "${cohortName}": "${duplCohortName}"`);
    // Delete duplicated cohort.
    const modalTextContent = await dataPage.deleteResource(duplCohortName, ResourceCard.Cohort);
    expect(modalTextContent).toContain(`Are you sure you want to delete Cohort: ${duplCohortName}?`);

    // Verify Delete successful.
    expect(await DataResourceCard.findCard(page, duplCohortName, 5000)).toBeFalsy();

    // find the original new cohortCard
    cohortCard = await DataResourceCard.findCard(page, `${cohortName}`);
    // Cohort can be opened from resource card link.
    await cohortCard.clickResourceName();
    // Wait for page ready
    await cohortBuildPage.waitForLoad();
    await waitWhileLoading(page);

    // click the delete icon on the cohort build page
    const modalTextContent1 = await cohortBuildPage.deleteCohort();

    // Verify dialog content text
    expect(modalTextContent1).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);

    // navigate to the sub cohort tab
    await dataPage.openCohortsSubtab();

    // verify that the cohort card does not exist on the subcohort tab
    expect(await DataResourceCard.findCard(page, `${cohortName}`, 5000)).toBeFalsy();
  });
});
