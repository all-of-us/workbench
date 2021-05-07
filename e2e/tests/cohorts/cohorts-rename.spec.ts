import DataResourceCard from 'app/component/data-resource-card';
import Link from 'app/element/link';
import CohortActionsPage from 'app/page/cohort-actions-page';
import CohortBuildPage, { FieldSelector } from 'app/page/cohort-build-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { waitForNumericalString, waitForText } from 'utils/waits-utils';

describe('User can create, modify, rename and delete Cohort', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = 'e2eCohortsRenameTest';
  /**
   * Test:
   * Find an existing workspace or create a new workspace if none exists.
   * Create new Cohort:
   *   - Add criteria in Include Group 1: Demographics Age range: 21 - 95
   *   - Add criteria in Exclude Group 3: Demographics Deceased
   * Save Cohort.
   * Modify Cohort: remove Exclude Group 3.
   * Save Changes.
   * Renaming Cohort.
   * Delete Cohort.
   */
  test('Add cohort including Demographics Age', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    // Click Add Cohorts button in Data page.
    await dataPage.getAddCohortsButton().clickAndWait();

    // Land on Build Cohort page.
    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Include Group 1: Demographics Age range: 21 - 95
    const minAge = 21;
    const maxAge = 95;
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeAge(minAge, maxAge);

    // Checking Group 1 count is numerical and greater than 1.
    const group1Count = await waitForNumericalString(page, group1.getGroupCountXpath(), 60000);
    const group1CountInt = Number(group1Count.replace(/,/g, ''));
    expect(group1CountInt).toBeGreaterThan(1);
    console.log(`Include Participants Group 1 Demographics Age Count = ${group1CountInt}`);

    let totalCount = await cohortBuildPage.getTotalCount();
    expect(totalCount).toEqual(group1Count);

    // Exclude Group 3: Demographics Deceased
    const group3 = cohortBuildPage.findExcludeParticipantsGroup('Group 3');
    const group3Count = await group3.includeDemographicsDeceased();

    const group3CountInt = Number(group3Count.replace(/,/g, ''));
    expect(group3CountInt).toBeGreaterThan(1);
    console.log(`Exclude Participants Group 3 Demographics Deceased Count = ${group3CountInt}`);

    // Log Total Count.
    totalCount = await cohortBuildPage.getTotalCount();
    const totalCountInt = Number(totalCount.replace(/,/g, ''));
    expect(totalCountInt).toBeGreaterThan(1);
    console.log(`Total Count: ${totalCountInt}`);

    // Save cohort.
    const cohortName = await cohortBuildPage.createCohort();
    console.log(`Created Cohort "${cohortName}"`);

    // Click cohort link. Open cohort build page.
    const cohortLink = Link.findByName(page, { name: cohortName });
    await cohortLink.clickAndWait();
    await waitForText(page, totalCount, { xpath: FieldSelector.TotalCount }, 60000);

    // Remove Exclude Group 3.
    await group3.deleteGroup();
    console.log('Removed Exclude Group 3');

    await cohortBuildPage.saveChanges();

    // Should land on Cohorts Actions page
    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();

    await dataPage.openCohortsSubtab();

    // Rename cohort.
    const newCohortName = makeRandomName();
    await dataPage.renameResource(cohortName, newCohortName, ResourceCard.Cohort);

    // Verify rename successful.
    expect(await DataResourceCard.findCard(page, newCohortName)).toBeTruthy();

    // Delete cohort.
    const modalTextContent = await dataPage.deleteResource(newCohortName, ResourceCard.Cohort);

    // Verify Delete dialog content text
    expect(modalTextContent).toContain(`Are you sure you want to delete Cohort: ${newCohortName}?`);

    // Verify Delete successful.
    expect(await DataResourceCard.findCard(page, newCohortName, 5000)).toBeFalsy();
  });
});
