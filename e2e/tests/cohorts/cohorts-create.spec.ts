import {signIn, waitWhileLoading} from 'utils/test-utils';
import WorkspacesPage from 'app/page/workspaces-page';
import {WorkspaceAccessLevel} from 'app/page-identifiers';
import WorkspaceCard from 'app/component/workspace-card';
import DataPage from 'app/page/data-page';
import {PhysicalMeasurementsCriteria} from 'app/component/create-criteria-modal';
import * as fp from 'lodash/fp';
import {waitForText} from 'utils/waits-utils';
import Link from 'app/element/link';
import CohortBuildPage, {FieldSelector} from 'app/page/cohort-build-page';

describe('User can create new Cohorts', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * Add criteria in Group 1: Physical Measurements criteria => BMI (>= 30).
   * Add criteria in Group 2: Demographics => Deceased.
   * Checking counts.
   */
  test('Create new cohorts of Physical Measurements BMI', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

      // Choose one existing workspace on "Your Workspaces" page
    const workspaceCard = new WorkspaceCard(page);
    const retrievedWorkspaces = await workspaceCard.getWorkspaceMatchAccessLevel(WorkspaceAccessLevel.OWNER);
    const oneWorkspaceCard: WorkspaceCard = fp.shuffle(retrievedWorkspaces)[0];
    await oneWorkspaceCard.clickWorkspaceName();

    const dataPage = new DataPage(page);
    await dataPage.waitForLoad();

    const addCohortsButton = await dataPage.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // In Build Cohort Criteria page
    const cohortPage = new CohortBuildPage(page);
    await cohortPage.waitForLoad();

    // Include Participants Group 1
    const group1Count = await cohortPage.includePhysicalMeasurement(1, PhysicalMeasurementsCriteria.BMI, 30);

    // Checking Group 1 Count: Group Count. should match Group 1 participants count.
    await waitForText(page, group1Count, {xpath: cohortPage.getIncludedGroupCountXpath(1)});
    const group1CountInt = Number(group1Count.replace(/,/g, ''));
    expect(group1CountInt).toBeGreaterThan(1);
    console.log('Group 1: Physical Measurement -> BMI count: ' + group1CountInt);

    // Checking Total Count: should match Group 1 participants count.
    await waitForText(page, group1Count, {xpath: FieldSelector.TotalCount});
    console.log('Total Count: ' + group1Count);

    // Rule: Adding additional groups should decrease Total Count

    // Include Participants Group 2: Select menu Demographics -> Deceased
    const group2Count = await cohortPage.includeDemographicsDeceased(2);
    const group2CountInt = Number(group2Count.replace(/,/g, ''));
    expect(group2CountInt).toBeGreaterThan(1);
    console.log('Group 2: Demographics -> Deceased count: ' + group2CountInt);

    // Compare the new Total Count with the old Total Count.
    const newTotalCount = await cohortPage.getTotalCount();
    const newTotalCountInt = Number(newTotalCount.replace(/,/g, ''));
    expect(newTotalCountInt).toBeLessThan(group1CountInt);
    console.log('new Total Count: ' + newTotalCountInt);

    // Cannot verify graphical charts, but we can check charts points exists.
    const chartPointsSelector = '//*[@class="highcharts-root"]//*[@x and @y and @width and @height and contains(@class, "highcharts-point")]';
    await page.waitForXPath(chartPointsSelector, {visible: true});

    // Save new cohort.
    const cohortName = await cohortPage.saveCohortAs();
    await waitForText(page, 'Cohort Saved Successfully');

    // Clean up: delete cohort
    const cohortLink = await Link.forLabel(page, {name: cohortName});
    await cohortLink.clickAndWait();
    await waitWhileLoading(page);
    await waitForText(page, newTotalCount, {xpath: FieldSelector.TotalCount});
    const dialogContent = await cohortPage.deleteCohort();
    // Verify dialog content text
    expect(dialogContent).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);
    console.log(`Cohort "${cohortName}" deleted.`);
  });

});
