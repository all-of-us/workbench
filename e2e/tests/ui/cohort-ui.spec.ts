import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, MenuOption, Tabs } from 'app/text-labels';
import { findAllCards, openTab, signInWithAccessToken } from 'utils/test-utils';
import CohortBuildPage from 'app/page/cohort-build-page';
import ClrIconLink from 'app/element/clr-icon-link';
import { PhysicalMeasurementsCriteria } from 'app/page/cohort-participants-group';
import ReviewCriteriaSidebar from 'app/sidebar/review-criteria-sidebar';
import * as fp from 'lodash/fp';
import { logger } from 'libs/logger';
import { Page } from 'puppeteer';
import { waitWhileLoading } from 'utils/waits-utils';
import expect from 'expect';
import HomePage from 'app/page/home-page';

describe.skip('Cohort UI Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Test reuse workspace that is older than 10 min. Test does not create new workspace.
  test('Cancel Build Cohort', async () => {
    // Find and open one workspace.
    const workspaceName = await openWorkspace(page);
    if (!workspaceName) {
      return;
    }

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
    await openTab(page, Tabs.Data);
    const warning = await cohortBuildPage.discardChangesConfirmationDialog();
    const expectedWarningText =
      'Your cohort has not been saved. If you’d like to save your cohort criteria,' +
      ' please click CANCEL and save your changes';

    const foundMatch = warning.some((item) => item.indexOf(expectedWarningText) !== -1);
    expect(foundMatch).toBe(true);

    // Changes are discarded, back to the Data page.
    await dataPage.waitForLoad();
  });

  test('Cohort search by code and description', async () => {
    // Find and open one workspace.
    const workspaceName = await openWorkspace(page);
    if (!workspaceName) {
      return;
    }

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.clickAddCohortsButton();

    // Landing in Build Cohort Criteria page.
    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.addCriteria([MenuOption.Procedures]);
    await waitWhileLoading(page);

    const resultsTable = group1.getSearchResultsTable();
    const originalRowNum = (await resultsTable.getRows()).length;
    expect(originalRowNum).toBeGreaterThanOrEqual(1);

    // Search by description.
    const description = 'surgical pathology';
    await group1.searchCriteria(description);
    // Verify getting any results for a valid search.
    let resultsRow = await resultsTable.getRowCount();
    expect(resultsRow).toBeGreaterThanOrEqual(1);

    // Search by code.
    const code = '128927009';
    await group1.searchCriteria(code);
    // Verify getting any results for a valid search.
    resultsRow = await resultsTable.getRowCount();
    expect(resultsRow).toBeGreaterThanOrEqual(1);

    // Search by Concept ID. DOESN'T RETURN ANY RESULT!
    const conceptId = '2213280';
    await group1.searchCriteria(conceptId);
    // Verify no results for an invalid search.
    resultsRow = await resultsTable.getRowCount();
    expect(resultsRow).toEqual(0);
  });

  async function openWorkspace(page: Page): Promise<string | null> {
    // Find all workspaces that are older than 30 min.
    await new HomePage(page).goToAllWorkspacesPage();
    const allWorkspaceCards = await findAllCards(page, 1000 * 60 * 30);
    // Don't create new workspace if none found.
    if (allWorkspaceCards.length === 0) {
      logger.info('Cannot find a suitable existing workspace (created at least 30 min ago). Test end early.');
      return null;
    }

    // Open any one workspace.
    const aWorkspaceCard = fp.shuffle(allWorkspaceCards)[0];
    const workspaceName = await aWorkspaceCard.clickName();
    return workspaceName;
  }
});
