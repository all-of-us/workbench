import CohortBuildPage from 'app/page/cohort-build-page';
import { PhysicalMeasurementsCriteria } from 'app/page/criteria-search-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { TabLabels } from 'app/page/workspace-base';

describe('Cohorts UI tests', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  /**
   * Test:
   * Add criteria in Group 1: Physical Measurements criteria => Weight (>= 190kg).
   * Click ABOUT tab.
   * Confirm Discard Changes.
   */
  test('Discard Changes', async () => {
    const workspaceCard = await findOrCreateWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    // Wait for the Data page.
    const dataPage = new WorkspaceDataPage(page);

    const addCohortsButton = await dataPage.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // In Build Cohort Criteria page
    const cohortPage = new CohortBuildPage(page);
    await cohortPage.waitForLoad();

    // Include Participants Group 1
    const group1 = cohortPage.findIncludeParticipantsGroup('Group 1');
    const group1Count = await group1.includePhysicalMeasurement(PhysicalMeasurementsCriteria.Weight, 190);
    // Checking Group 1 Count.
    expect(group1Count).toEqual(await group1.getGroupCount());

    // Cannot verify graphical charts display, but we can check charts points existance.
    const chartPointsSelector =
      '//*[@class="highcharts-root"]//*[@x and @y and @width and @height and contains(@class, "highcharts-point")]';
    await page.waitForXPath(chartPointsSelector, { visible: true });

    // Copy button is disabled
    const copyButton = await cohortPage.getCopyButton();
    expect(await copyButton.isDisabled()).toBe(true);

    // Trash (Delete) button is disabled
    const trashButton = await cohortPage.getDeleteButton();
    expect(await trashButton.isDisabled()).toBe(true);

    // Export button is disabled
    const exportButton = await cohortPage.getExportButton();
    expect(await exportButton.isDisabled()).toBe(true);

    await dataPage.openAboutPage({ waitPageChange: false });

    // Don't save. Confirm Discard Changes
    const modalTextContent = await cohortPage.discardChangesConfirmationDialog();
    // Verify dialog content text
    expect(modalTextContent).toContain('Warning!');
    const warningText =
      'Your cohort has not been saved. If you’d like to save your cohort criteria, please click CANCEL and click CREATE COHORT to save your criteria.';
    expect(modalTextContent).toContain(warningText);

    // Check ABOUT tab is open
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();
    const isOpen = await aboutPage.isOpen(TabLabels.About);
    expect(isOpen).toBe(true);
  });
});
