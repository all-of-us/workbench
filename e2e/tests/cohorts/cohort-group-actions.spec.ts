import CohortActionsPage from 'app/page/cohort-actions-page';
import CohortBuildPage from 'app/page/cohort-build-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { MenuOption, ResourceCard } from 'app/text-labels';
import { makeRandomName, makeWorkspaceName } from 'utils/str-utils';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { waitWhileLoading } from 'utils/waits-utils';
import { getPropValue, waitUntilChanged } from 'utils/element-utils';
import ClrIconLink from 'app/element/clr-icon-link';
import { Ethnicity, PhysicalMeasurementsCriteria } from 'app/page/cohort-participants-group';

describe('Build cohort page actions', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = makeWorkspaceName();

  test('Refresh results', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.clickAddCohortsButton();

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Include Participants Group 1: Fitbit
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    const group1Count = await group1.includeFitbit();
    const totalCount = await cohortBuildPage.getTotalCount();

    // REFRESH button is disabled.
    const refreshButton = cohortBuildPage.findRefreshButton();
    expect(await refreshButton.isCursorNotAllowed()).toBe(true);

    const genderSelectMenu = await cohortBuildPage.findGenderSelectMenu();
    const genderOptions = await genderSelectMenu.getAllOptionTexts();
    expect(genderOptions).toEqual(expect.arrayContaining([MenuOption.GenderIdentity, MenuOption.SexAtBirth]));

    // Select Sex at Birth menuitem.
    await genderSelectMenu.select(MenuOption.SexAtBirth);

    const ageSelectMenu = await cohortBuildPage.findAgeSelectMenu();
    const ageOptions = await ageSelectMenu.getAllOptionTexts();
    expect(ageOptions).toEqual(
      expect.arrayContaining([MenuOption.CurrentAge, MenuOption.AgeAtConsent, MenuOption.AgeAtCDR])
    );

    // Reading Gender Identity chart x-labels.
    const css = '[data-highcharts-chart="0"] .highcharts-axis-labels.highcharts-xaxis-labels text';
    let xLabels = await page.$$(css);
    for (const xlabel of xLabels) {
      const label = await getPropValue<string>(xlabel, 'textContent');
      expect(
        ['Female', 'Male', 'Not man only, not woman only, prefer not to answer, or skipped'].some(
          (item) => item === label
        )
      );
    }

    // REFRESH button is enabled after change Results by.
    expect(await refreshButton.isCursorNotAllowed()).toBe(false);
    await refreshButton.click();
    await waitWhileLoading(page);
    await cohortBuildPage.getTotalCount();

    // Reading Sex at Birth chart x-labels.
    xLabels = await page.$$(css);
    for (const xlabel of xLabels) {
      const label = await getPropValue<string>(xlabel, 'textContent');
      expect(
        ['Female', 'Male', 'Not male, not female, prefer not to answer, or skipped', 'Unknown'].some(
          (item) => item === label
        )
      );
    }

    // Total Count is same.
    expect(await cohortBuildPage.getTotalCount()).toEqual(totalCount);
    // Group 1 Count is same.
    expect(await group1.getGroupCount()).toEqual(group1Count);

    await cohortBuildPage.createCohort();

    await new CohortActionsPage(page).deleteCohort();
  });

  test('Add, delete and rename group', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.clickAddCohortsButton();

    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Include Group 1: Demographics Age range: 21 - 95
    const minAge = 21;
    const maxAge = 95;
    let group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeAge(minAge, maxAge);
    expect(await group1.getGroupCount()).toBeGreaterThan(1);

    // Exclude Group 3: Demographics Deceased
    const excludeGroup3 = cohortBuildPage.findExcludeParticipantsGroup('Group 3');
    const group3Count = await excludeGroup3.includeDemographicsDeceased();
    expect(group3Count).toBeGreaterThan(1);

    // Log Total Count.
    let totalCount = await cohortBuildPage.getTotalCount();
    expect(totalCount).toBeGreaterThan(1);

    // Save cohort.
    const cohortName = await cohortBuildPage.createCohort();

    // Click cohort link. Open cohort build page.
    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();
    await cohortActionsPage.clickCohortName();
    expect(await cohortBuildPage.getTotalCount()).toEqual(totalCount);

    // Delete Exclude Group 3.
    await excludeGroup3.deleteGroup();
    totalCount = await cohortBuildPage.getTotalCount();
    await cohortBuildPage.saveChanges();

    // Open Build Cohort page again.
    await cohortActionsPage.waitForLoad();
    await cohortActionsPage.clickCohortName();
    await cohortBuildPage.getTotalCount();

    // Change Group 1 name.
    const newName = makeRandomName();
    group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.editGroupName(newName);

    // Verify new named group
    const groupName = cohortBuildPage.findIncludeParticipantsGroup(newName);
    expect(await groupName.exists()).toBe(true);

    // Add a new group in Include Group
    const newGroup = cohortBuildPage.findIncludeParticipantsEmptyGroup();
    await newGroup.includePhysicalMeasurement([PhysicalMeasurementsCriteria.Weight], { filterValue: 200 });

    await cohortBuildPage.getTotalCount();
    await cohortBuildPage.saveChanges();

    // Should land on Cohort Actions page
    await cohortActionsPage.waitForLoad();
    expect(await cohortActionsPage.getCohortLink().exists()).toBeTruthy();

    await dataPage.openDataPage();
    await dataPage.waitForLoad();
    await dataPage.deleteResource(cohortName, ResourceCard.Cohort);
  });

  test('Delete, edit, rename and suppress criteria', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.clickAddCohortsButton();

    // In Build Cohort Criteria page.
    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Add Fitbit to Participants Group 1.
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.includeFitbit();

    let totalCount = await cohortBuildPage.getTotalCount();
    const group1Count = await group1.getGroupCount();

    // Include Group 1 has 1 criteria.
    expect((await group1.findGroupCriteriaList()).length).toBe(1);

    // Add second criteria Genome Variant to Group 1.
    await group1.includeWholeGenomeVariant();

    // New Total Count and Group 1 Total Count are different after adding second criteria.
    expect((await cohortBuildPage.getTotalCount()) === totalCount).toBe(false);
    expect((await group1.getGroupCount()) === group1Count).toBe(false);

    // Include Group 1 has 2 criteria.
    expect((await group1.findGroupCriteriaList()).length).toBe(2);
    totalCount = await cohortBuildPage.getTotalCount(); // New Total Count by 2 criteria.

    // Rename Criteria: rename "Whole Genome Variant" to "NDA Sets".
    const newCriteriaName = 'DNA Sets';
    await group1.editCriteriaName('Whole Genome Variant', newCriteriaName);
    // New criteria name is visible in page.
    await page.waitForXPath(group1.getCriteriaXpath(newCriteriaName), { visible: true });

    // Suppress Criteria from total count: Suppress "Has any Fitbit data" criteria.
    const fitbitName = 'Has any Fitbit data';
    await group1.suppressCriteriaFromTotalCount(fitbitName);
    const suppressedTotalCount = await cohortBuildPage.getTotalCount();

    // Look for suppressed criteria icon.
    const suppressedIcon = ClrIconLink.findByName(page, {
      iconShape: 'eye-hide',
      containsText: 'This criteria has been suppressed'
    });
    expect(await suppressedIcon.exists()).toBe(true);
    // New Total Count is lesser.
    expect(suppressedTotalCount).toBeLessThan(totalCount);

    // Enable suppressed Fitbit criteria.
    await group1.getEnableCriteriaButton().click();
    await waitWhileLoading(page);
    // Calculated Total Count is same as before suppressed.
    expect(await cohortBuildPage.getTotalCount()).toEqual(totalCount);

    await cohortBuildPage.createCohort();

    const cohortActionsPage = new CohortActionsPage(page);
    await cohortActionsPage.waitForLoad();
    await cohortActionsPage.clickCohortName();

    // Delete Criteria: delete Fitbit criteria.
    await group1.deleteCriteria(fitbitName);
    // Calculated new Total Count is less than before delete criteria.
    expect(await cohortBuildPage.getTotalCount()).toBeLessThan(totalCount);

    const undoDeleteButton = group1.getUndoDeleteCriteriaButton();
    expect(await undoDeleteButton.exists()).toBe(true);
    // Wait until UNDO button is gone.
    await waitUntilChanged(page, await undoDeleteButton.asElementHandle());

    // Include Group 1 has 1 criteria after delete 1.
    expect((await group1.findGroupCriteriaList()).length).toBe(1);

    // Add Exclude Group 3: add Ethnicity criteria.
    const excludeGroup3 = cohortBuildPage.findExcludeParticipantsGroup('Group 3');
    const excludeGroup3Count = await excludeGroup3.includeEthnicity([Ethnicity.NotHispanicOrLatino]);

    // Edit Exclude Group 3: edit Ethnicity criteria.
    await excludeGroup3.editCriteria('Contains Ethnicity Code');
    await excludeGroup3.findAddCriteriaIcon(Ethnicity.PreferNotToAnswer).click();
    await excludeGroup3.criteriaAddedMessage();
    await excludeGroup3.finishAndReviewButton();
    await excludeGroup3.saveCriteria();

    const excludeGroup3NewCount = await excludeGroup3.getGroupCount();
    expect(excludeGroup3NewCount === excludeGroup3Count).toBe(false);

    // Save changes.
    await cohortBuildPage.saveChanges();
    await cohortActionsPage.deleteCohort();
  });
});
