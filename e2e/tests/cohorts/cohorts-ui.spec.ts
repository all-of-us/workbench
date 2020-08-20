import {PhysicalMeasurementsCriteria} from 'app/page/cohort-criteria-modal';
import CohortBuildPage from 'app/page/cohort-build-page';
import DataPage, {TabLabelAlias} from 'app/page/data-page';
import {pickWorkspace, signIn} from 'utils/test-utils';
import {waitForText} from 'utils/waits-utils';
import WorkspaceAboutPage from 'app/page/workspace-about-page';

describe('Cohorts UI tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * Add criteria in Group 1: Physical Measurements criteria => Weight (>= 190kg).
   * Click ABOUT tab.
   * Confirm Discard Changes.
   */
  test('Discard Changes', async () => {

    await pickWorkspace(page).then(card => card.clickWorkspaceName());

    // Wait for the Data page.
    const dataPage = new DataPage(page);

    const addCohortsButton = await dataPage.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // In Build Cohort Criteria page
    const cohortPage = new CohortBuildPage(page);
    await cohortPage.waitForLoad();

    // Include Participants Group 1
    const group1 = cohortPage.findIncludeParticipantsGroup('Group 1');
    const group1Count = await group1.includePhysicalMeasurement(PhysicalMeasurementsCriteria.Weight, 190);
    // Checking Group 1 Count.
    await waitForText(page, group1Count, {xpath: group1.getGroupCountXpath()});

    // Cannot verify graphical charts display, but we can check charts points existance.
    const chartPointsSelector = '//*[@class="highcharts-root"]//*[@x and @y and @width and @height and contains(@class, "highcharts-point")]';
    await page.waitForXPath(chartPointsSelector, {visible: true});

    // Copy button is disabled
    const copyButton = await cohortPage.getCopyButton();
    expect(await copyButton.isDisabled()).toBe(true);

    // Trash (Delete) button is disabled
    const trashButton = await cohortPage.getDeleteButton();
    expect(await trashButton.isDisabled()).toBe(true);

    // Export button is disabled
    const exportButton = await cohortPage.getExportButton();
    expect(await exportButton.isDisabled()).toBe(true);

    await dataPage.openTab(TabLabelAlias.About, {waitPageChange: false});

    // Don't save. Confirm Discard Changes
    const modalTextContent = await cohortPage.discardChangesConfirmationDialog();
    // Verify dialog content text
    expect(modalTextContent).toContain(`Your cohort has not been saved.`);
    expect(modalTextContent).toContain(`please click CANCEL and click CREATE COHORT to save your criteria.`);

    // Check ABOUT tab is open
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();
    const isOpen = await aboutPage.isOpen();
    expect(isOpen).toBe(true);
  });


});
