import DataResourceCard from 'app/component/data-resource-card';
import CohortBuildPage from 'app/page/cohort-build-page';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, MenuOption, ResourceCard } from 'app/text-labels';
import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import { waitForText, waitWhileLoading } from 'utils/waits-utils';
import DatasetEditPage from 'app/page/dataset-edit-page';

describe('Dataset test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = 'e2eCreateDataSetsTest';

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Create a new Cohort from searching for drug name "hydroxychloroquine".
   * - Create a new Dataset from All Participants and All Surveys.
   * - Save dataset without Export to Notebook.
   * - Edit dataset. Save dataset without Export to Notebook.
   * - Delete Dataset.
   */
  test('Create Dataset from user-defined Cohorts', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });
    const dataPageUrl = page.url();

    // Click Add Cohorts button
    const dataPage = new WorkspaceDataPage(page);
    const addCohortsButton = dataPage.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // Build new Cohort
    const cohortBuildPage = new CohortBuildPage(page);
    // Include Participants Group 1: Add a Condition
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    // Search for drug hydroxychloroquine
    await group1.includeDrugs('hydroxychloroquine');

    // Save new cohort.
    const cohortName = await cohortBuildPage.createCohort();
    await waitForText(page, 'Cohort Saved Successfully');
    console.log(`Created Cohort "${cohortName}"`);

    await dataPage.openDataPage();

    // Click Add Datasets button.
    const datasetPage = await dataPage.clickAddDatasetButton();

    await datasetPage.selectCohorts([cohortName]);
    await datasetPage.selectConceptSets([LinkText.Demographics]);
    const createModal = await datasetPage.clickCreateButton();
    let datasetName = await createModal.createDataset();

    // Verify create successful.
    await dataPage.openDatasetsSubtab();

    const resourceCard = new DataResourceCard(page);
    const dataSetExists = await resourceCard.cardExists(datasetName, ResourceCard.Dataset);
    expect(dataSetExists).toBe(true);

    // Edit the dataset to include "All Participants".
    const datasetCard = await resourceCard.findCard(datasetName);
    await datasetCard.selectSnowmanMenu(MenuOption.Edit, { waitForNav: true });
    await waitWhileLoading(page);

    const datasetEditPage = new DatasetEditPage(page);
    await datasetEditPage.waitForLoad();
    await datasetEditPage.selectCohorts(['All Participants']);
    await datasetEditPage.clickExportButton();

    page.goto(dataPageUrl);
    await dataPage.waitForLoad()
    await dataPage.openDatasetsSubtab();
    await dataPage.deleteResource(datasetName, ResourceCard.Dataset);
  });
});
