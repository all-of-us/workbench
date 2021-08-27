import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText } from 'app/text-labels';
import { findAllCards, signInWithAccessToken } from 'utils/test-utils';
import WorkspacesPage from 'app/page/workspaces-page';
import { logger } from 'libs/logger';
import * as fp from 'lodash/fp';

describe('Dataset UI Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Test reuse workspace that is older than 10 min. Test does not create new workspace.
  test('Cannot Create Dataset if Required Fields are Empty', async () => {
    // Find all workspaces that are older than 10 min.
    const allWorkspaceCards = await findAllCards(page, 1000 * 60 * 10);
    if (allWorkspaceCards.length === 0) {
      logger.info('Cannot find a suitable existing workspace (created at least 10 min ago). Test end early.');
      return;
    }

    // Open one workspace.
    const aWorkspaceCard = fp.shuffle(allWorkspaceCards)[0];
    await aWorkspaceCard.clickWorkspaceName();

    // Click Add Dataset button.
    const dataPage = new WorkspaceDataPage(page);
    const datasetPage = await dataPage.clickAddDatasetButton();

    // Select Values (Columns): Select All checkbox is disabled.
    expect(await datasetPage.getSelectAllCheckbox().isDisabled()).toBe(true);

    // Step 1 Select Cohort: Choose "All Participants"
    await datasetPage.selectCohorts(['All Participants']);

    // Export button is disabled.
    const analyzeButton = datasetPage.getAnalyzeButton();
    expect(await analyzeButton.isCursorNotAllowed()).toBe(true);

    // Create Dataset button is disabled.
    const createDatasetButton = datasetPage.getCreateDatasetButton();
    expect(await createDatasetButton.isCursorNotAllowed()).toBe(true);

    // Select Values (Columns): Select All checkbox is disabled.
    expect(await datasetPage.getSelectAllCheckbox().isDisabled()).toBe(true);

    // Step 2 Select Concept Sets (Rows): select Demographics.
    await datasetPage.selectConceptSets([LinkText.Demographics]);

    // Export button is disabled.
    expect(await analyzeButton.isCursorNotAllowed()).toBe(true);

    // Create Dataset button is enabled.
    expect(await createDatasetButton.isCursorNotAllowed()).toBe(false);

    // Select Values (Columns): Select All checkbox is enabled.
    expect(await datasetPage.getSelectAllCheckbox().isDisabled()).toBe(false);

    // Select Values (Columns): Select All checkbox is checked.
    expect(await datasetPage.getSelectAllCheckbox().isChecked()).toBe(true);

    // Step 2 Select Concept Sets (Rows): select all checkboxes.
    await datasetPage.selectConceptSets([LinkText.AllSurveys]);
    await datasetPage.selectConceptSets([LinkText.FitbitHeartRateSummary]);
    await datasetPage.selectConceptSets([LinkText.FitbitActivitySummary]);
    await datasetPage.selectConceptSets([LinkText.FitbitHeartRateLevel]);
    await datasetPage.selectConceptSets([LinkText.FitbitIntraDaySteps]);

    // Export button is disabled.
    expect(await analyzeButton.isCursorNotAllowed()).toBe(true);

    // Create Dataset button is enabled.
    expect(await createDatasetButton.isCursorNotAllowed()).toBe(false);

    // Step 1 uncheck "All Participants".
    await datasetPage.unselectCohort('All Participants');

    // Export button is disabled.
    expect(await analyzeButton.isCursorNotAllowed()).toBe(true);

    // Create Dataset button is disabled.
    expect(await createDatasetButton.isCursorNotAllowed()).toBe(true);

    // View Preview Table button is disabled.
    expect(await datasetPage.getPreviewTableButton().isCursorNotAllowed()).toBe(true);

    // Go to Workspaces page. There is no Discard Changes warning.
    await datasetPage.getBackToWorkspacesLink().clickAndWait();

    await new WorkspacesPage(page).waitForLoad();
  });
});
