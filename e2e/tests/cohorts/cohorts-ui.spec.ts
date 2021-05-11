import CohortBuildPage from 'app/page/cohort-build-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { TabLabels } from 'app/page/workspace-base';
import { PhysicalMeasurementsCriteria } from 'app/page/cohort-participants-group';
import {LinkText, MenuOption} from "app/text-labels";
import ClrIconLink from "app/element/clr-icon-link";
import ReviewCriteriaSidebar from "app/component/review-criteria-sidebar";
import {buildXPath} from "app/xpath-builders";
import {ElementType} from "app/xpath-options";

describe('Cohort build page UI', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('Discard Changes', async () => {
    await findOrCreateWorkspace(page);

    // Wait for the Data page.
    const dataPage = new WorkspaceDataPage(page);

    const addCohortsButton = dataPage.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // In Build Cohort Criteria page.
    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Include Participants Group 1.
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.clickCriteriaMenuItems([MenuOption.PhysicalMeasurements]);
    const addIcon = ClrIconLink.findByName(page, {
      startsWith: PhysicalMeasurementsCriteria.WheelChairUser,
      iconShape: 'plus-circle',
      ancestorLevel: 1
    });
    await addIcon.click();
    const message = await group1.getCriteriaAddedSuccessMessage();
    expect(message).toEqual('Criteria Added');

    await group1.clickFinishAndReviewButton();
    const reviewCriteriaSidebar = new ReviewCriteriaSidebar(page);
    await reviewCriteriaSidebar.waitUntilVisible();

    // Find the Remove Selected Criteria icon next to the added criteria in sidebar.
    const iconSelector = buildXPath({
      type: ElementType.Icon,
      iconShape: 'times-circle',
      containsText: PhysicalMeasurementsCriteria.WheelChairUser
    }, reviewCriteriaSidebar);
    expect(await page.waitForXPath(iconSelector, {visible: true, timeout: 2000})).toBeTruthy();

    // Click Back button to close sidebar.
    await reviewCriteriaSidebar.clickButton(LinkText.Back);
    await reviewCriteriaSidebar.waitUntilClose();

    // Click Data tab, Warning (Discard Changes) modal should open.
    await dataPage.openDataPage({ waitPageChange: false });
    const warning = await cohortBuildPage.discardChangesConfirmationDialog();
    const warningText =
        'Your cohort has not been saved. If you’d like to save your cohort criteria,' +
        ' please click CANCEL and save your changes';
    expect(warning).toContain(warningText);

    // Changes are discarded, back to the Data page.
    await dataPage.waitForLoad();
    expect(await dataPage.getAddCohortsButton().asElementHandle()).toBeTruthy();
    expect(await dataPage.getAddDatasetButton().asElementHandle()).toBeTruthy();
  });

  test('Discard Changes', async () => {
    await findOrCreateWorkspace(page);

    // Wait for the Data page.
    const dataPage = new WorkspaceDataPage(page);

    const addCohortsButton = dataPage.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // In Build Cohort Criteria page
    const cohortBuildPage = new CohortBuildPage(page);
    await cohortBuildPage.waitForLoad();

    // Include Participants Group 1
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    await group1.clickCriteriaMenuItems([MenuOption.PhysicalMeasurements]);
    const addIcon = ClrIconLink.findByName(page, {
      startsWith: PhysicalMeasurementsCriteria.WheelChairUser,
      iconShape: 'plus-circle',
      ancestorLevel: 1
    });
    await addIcon.click();
    const message = await group1.getCriteriaAddedSuccessMessage();
    expect(message).toEqual('Criteria added');

    await group1.clickFinishAndReviewButton();
    const reviewCriteriaSidebar = new ReviewCriteriaSidebar(page);
    await reviewCriteriaSidebar.waitUntilVisible();

    // Find the Remove Selected Criteria icon in sidebar.
    const iconSelector = buildXPath({
      type: ElementType.Icon,
      iconShape: 'times-circle',
      containsText: PhysicalMeasurementsCriteria.WheelChairUser
    }, reviewCriteriaSidebar);
    expect(await page.waitForXPath(iconSelector, {visible: true, timeout: 2000})).toBeTruthy();

    // Click Back button to close sidebar.
    await reviewCriteriaSidebar.clickButton(LinkText.Back);
    await reviewCriteriaSidebar.waitUntilClose();

    // Click Data tab, Warning (Discard Changes) modal should open.
    await dataPage.openDataPage({ waitPageChange: false });
    const warning = await cohortBuildPage.discardChangesConfirmationDialog();
    const warningText =
        'Your cohort has not been saved. If you’d like to save your cohort criteria,' +
        ' please click CANCEL and save your changes';
    expect(warning).toContain(warningText);

    // Back to the Data page.
    await dataPage.waitForLoad();
    expect(await dataPage.getAddCohortsButton().asElementHandle()).toBeTruthy();
    expect(await dataPage.getAddDatasetButton().asElementHandle()).toBeTruthy();

    const group1Count = await group1.includePhysicalMeasurement([PhysicalMeasurementsCriteria.Weight], {
      filterValue: 190
    });
    // Checking Group 1 Count.
    expect(group1Count).toEqual(await group1.getGroupCount());

    // Cannot verify graphical charts display, but we can check charts points existance.
    const chartPointsSelector =
        '//*[@class="highcharts-root"]//*[@x and @y and @width and @height and contains(@class, "highcharts-point")]';
    await page.waitForXPath(chartPointsSelector, { visible: true });

    // Copy button is disabled
    const copyButton = cohortPage.getCopyButton();
    expect(await copyButton.isDisabled()).toBe(true);

    // Trash (Delete) button is disabled
    const trashButton = cohortPage.getDeleteButton();
    expect(await trashButton.isDisabled()).toBe(true);

    // Export button is disabled
    const exportButton = cohortPage.getExportButton();
    expect(await exportButton.isDisabled()).toBe(true);

    await dataPage.openAboutPage({ waitPageChange: false });

    // Don't save. Confirm Discard Changes
    const modalTextContent = await cohortPage.discardChangesConfirmationDialog();
    // Verify dialog content text
    expect(modalTextContent).toContain('Warning!');
    const warningText =
        'Your cohort has not been saved. If you’d like to save your cohort criteria,' +
        ' please click CANCEL and click CREATE COHORT to save your criteria.';
    expect(modalTextContent).toContain(warningText);

    // Check ABOUT tab is open
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();
    const isOpen = await aboutPage.isOpen(TabLabels.About);
    expect(isOpen).toBe(true);
  });

});
