import HelpSidebar from 'app/component/help-sidebar';
import {FilterSign, PhysicalMeasurementsCriteria} from 'app/page/criteria-search-page';
import DataResourceCard from 'app/component/data-resource-card';
import Button from 'app/element/button';
import ClrIconLink from 'app/element/clr-icon-link';
import Link from 'app/element/link';
import {Option, LinkText, ResourceCard} from 'app/text-labels';
import CohortBuildPage, {FieldSelector} from 'app/page/cohort-build-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {findOrCreateWorkspace, signInWithAccessToken} from 'utils/test-utils';
import {waitForText, waitWhileLoading} from 'utils/waits-utils';


describe('User can create new Cohorts', () => {

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
  test('Add Cohort of Physical Measurements BMI', async () => {

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

    // Save new cohort.
    const cohortName = await cohortPage.saveCohortAs();
    console.log(`Created Cohort "${cohortName}"`);

    // Open Cohort details.
    const cohortLink = await Link.findByName(page, {name: cohortName});
    await cohortLink.clickAndWait();
    await waitForText(page, newTotalCount, {xpath: FieldSelector.TotalCount}, 60000);

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

  /**
   * Test:
   * Find an existing workspace.
   * Create new cohort with Condition = EKG.
   * Check Group and Total Count.
   * Check cohort open okay.
   * Duplicate cohort.
   * Delete cohort.
   */
  test('Add Cohort of EKG condition with modifiers', async () => {

    const workspaceCard = await findOrCreateWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    // Wait for the Data page.
    const dataPage = new WorkspaceDataPage(page);

    // Save url
    const workspaceDataUrl = page.url();

    // Click Add Cohorts button
    const addCohortsButton = await dataPage.getAddCohortsButton();
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
    const addIcon = await ClrIconLink.findByName(page, {containsText: nameValue, iconShape: 'plus-circle'}, search2ResultsTable);
    await addIcon.click();

    // Click Finish & Review button to open selection list and add modifier
    const finishAndReviewButton = await Button.findByName(page, {name: LinkText.FinishAndReview});
    await finishAndReviewButton.waitUntilEnabled();
    await finishAndReviewButton.click();

    // Add Condition Modifiers: Age At Event >= 50
    const helpSidebar = new HelpSidebar(page);
    await helpSidebar.addAgeModifier(FilterSign.GreaterThanOrEqualTo, 50);

    // Click SAVE CRITERIA button. Sidebar closes.
    await helpSidebar.clickSaveCriteriaButton();

    // Check Group 1 Count.
    const group1Count = await group1.getGroupCount();
    const group1CountInt = Number(group1Count.replace(/,/g, ''));
    expect(group1CountInt).toBeGreaterThan(1);
    console.log('Group 1: ' + group1CountInt);

    // Check Total Count.
    const totalCount = await cohortBuildPage.getTotalCount();
    const totalCountInt = Number(totalCount.replace(/,/g, ''));
    expect(totalCountInt).toBe(group1CountInt);
    console.log('Total Count: ' + totalCountInt);

    // Save new cohort.
    const cohortName = await cohortBuildPage.saveCohortAs();
    await waitForText(page, 'Cohort Saved Successfully');
    console.log(`Created Cohort "${cohortName}"`);

    // Open Workspace, search for created cohort.
    await page.goto(workspaceDataUrl);
    await dataPage.waitForLoad();

    // Cohort can be opened from resource card link.
    let cohortCard = await DataResourceCard.findCard(page, cohortName);
    await cohortCard.clickResourceName();
    // Wait for page ready
    await cohortBuildPage.waitForLoad();
    await waitWhileLoading(page);

    await dataPage.openDataPage();

    // Duplicate cohort using Ellipsis menu.
    const origCardsCount = (await DataResourceCard.findAllCards(page)).length;
    cohortCard = await DataResourceCard.findCard(page, cohortName);
    await cohortCard.selectSnowmanMenu(Option.Duplicate, {waitForNav: false});
    await waitWhileLoading(page);
    const newCardsCount = (await DataResourceCard.findAllCards(page)).length;
    // cards count increase by 1.
    expect(newCardsCount).toBe(origCardsCount + 1);

    // Delete duplicated cohort.
    let modalTextContent = await dataPage.deleteResource(`Duplicate of ${cohortName}`, ResourceCard.Cohort);
    expect(modalTextContent).toContain(`Are you sure you want to delete Cohort: Duplicate of ${cohortName}?`);

    // Delete new cohort.
    modalTextContent = await dataPage.deleteResource(cohortName, ResourceCard.Cohort);
    expect(modalTextContent).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);

  });


});
