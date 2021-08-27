import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, MenuOption } from 'app/text-labels';
import { findAllCards, signInWithAccessToken } from 'utils/test-utils';
import CohortBuildPage from 'app/page/cohort-build-page';
import ClrIconLink from 'app/element/clr-icon-link';
import { PhysicalMeasurementsCriteria } from 'app/page/cohort-participants-group';
import ReviewCriteriaSidebar from 'app/component/review-criteria-sidebar';
import * as fp from 'lodash/fp';
import { logger } from 'libs/logger';

describe('Cohort UI Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('Cancel Build Cohort', async () => {
    // Find all workspaces that are older than 10 min.
    const allWorkspaceCards = await findAllCards(page, 1000 * 60 * 10);
    if (allWorkspaceCards.length === 0) {
      logger.info('Cannot find a suitable existing workspace (created at least 10 min ago). Test end early.');
      return;
    }

    // Open one workspace.
    const aWorkspaceCard = fp.shuffle(allWorkspaceCards)[0];
    await aWorkspaceCard.clickWorkspaceName();

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
    await dataPage.openDataPage({ waitPageChange: false });
    const warning = await cohortBuildPage.discardChangesConfirmationDialog();
    const expectedWarningText =
      'Your cohort has not been saved. If you’d like to save your cohort criteria,' +
      ' please click CANCEL and save your changes';

    const foundMatch = warning.some((item) => item.indexOf(expectedWarningText) !== -1);
    expect(foundMatch).toBe(true);

    // Changes are discarded, back to the Data page.
    await dataPage.waitForLoad();
  });
});
