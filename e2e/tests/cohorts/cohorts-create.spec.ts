import HelpSidebar from 'app/component/help-sidebar';
import {FilterSign} from 'app/page/cohort-search-page';
import DataResourceCard from 'app/component/data-resource-card';
import Button from 'app/element/button';
import ClrIconLink from 'app/element/clr-icon-link';
import {Option, LinkText, ResourceCard} from 'app/text-labels';
import CohortBuildPage from 'app/page/cohort-build-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {findWorkspace, signIn} from 'utils/test-utils';
import {waitForText, waitWhileLoading} from 'utils/waits-utils';


describe('User can create new Cohorts', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Add Cohort of EKG condition with modifiers', async () => {

    const workspaceCard = await findWorkspace(page);
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
    const search1ResultsTable = await searchPage.searchCondition('allergist');
    expect(await search1ResultsTable.exists()).toBe(false);

    // Next, search for condition EKG
    const search2ResultsTable = await searchPage.searchCondition('EKG');
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
