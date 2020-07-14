import DataResourceCard, {CardType} from 'app/component/data-resource-card';
import ClrIconLink from 'app/element/clr-icon-link';
import CohortBuildPage from 'app/page/cohort-build-page';
import DataPage, {TabLabelAlias} from 'app/page/data-page';
import DatasetSaveModal from 'app/page/dataset-save-modal';
import {EllipsisMenuAction, LinkText} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn, waitWhileLoading} from 'utils/test-utils';
import {waitForText} from 'utils/waits-utils';

describe('Create Dataset', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Create a new Dataset from All Participants and All Surveys. Save dataset without Export to Notebook.
   * - Rename dataset.
   * - Delete Dataset.
   */
  test('Can create Dataset with defaults selections', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    // Click Add Datasets button
    const dataPage = new DataPage(page);
    const datasetPage = await dataPage.clickAddDatasetButton();

    await datasetPage.selectCohorts(['All Participants']);
    await datasetPage.selectConceptSets(['Demographics', 'All Surveys']);
    await datasetPage.clickSaveAndAnalyzeButton();

    const saveModal = new DatasetSaveModal(page);
    const datasetName = await saveModal.saveDataset();

    // Verify create successful
    const resourceCard = new DataResourceCard(page);
    const dataSetExists = await resourceCard.cardExists(datasetName, CardType.Dataset);
    expect(dataSetExists).toBe(true);

    // Rename Dataset
    const newDatasetName = makeRandomName();
    await dataPage.renameDataset(datasetName, newDatasetName);

    await dataPage.openTab(TabLabelAlias.Datasets, {waitPageChange: false});

    // Verify rename successful
    const newDatasetExists = await resourceCard.cardExists(newDatasetName, CardType.Dataset);
    expect(newDatasetExists).toBe(true);

    const oldDatasetExists = await resourceCard.cardExists(datasetName, CardType.Dataset);
    expect(oldDatasetExists).toBe(false);

    // Delete Dataset
    const textContent = await dataPage.deleteDataset(newDatasetName);
    expect(textContent).toContain(`Are you sure you want to delete Dataset: ${newDatasetName}?`);

  });

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Create a new Cohort from drug "hydroxychloroquine".
   * - Create a new Dataset from All Participants and All Surveys. Save dataset without Export to Notebook.
   * - Edit dataset. Save dataset without Export to Notebook.
   * - Delete Dataset.
   */
  test('Can create Dataset with user-defined cohort', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    // Click Add Cohorts button
    const dataPage = new DataPage(page);
    const addCohortsButton = await dataPage.getAddCohortsButton();
    await addCohortsButton.clickAndWait();

    // Build new Cohort
    const cohortBuildPage = new CohortBuildPage(page);
    // Include Participants Group 1: Add a Condition
    const group1 = cohortBuildPage.findIncludeParticipantsGroup('Group 1');
    const modal = await group1.includeDrugs();

    // Search for drug hydroxychloroquine
    const searchResultsTable = await modal.searchCondition('hydroxychloroquine');
    // Add drug in first row result
    const nameValue = await searchResultsTable.getCellValue(1, 1);
    const addIcon = await ClrIconLink.findByName(page, {containsText: nameValue, iconShape: 'plus-circle'}, modal);
    await addIcon.click();

    // Click FINISH button. Criteria dialog closes.
    await modal.clickButton(LinkText.Finish);
    await cohortBuildPage.getTotalCount();

    // Save new cohort.
    const cohortName = await cohortBuildPage.saveCohortAs();
    await waitForText(page, 'Cohort Saved Successfully');
    console.log(`Created Cohort "${cohortName}"`);

    await dataPage.openTab(TabLabelAlias.Data);

    // Click Add Datasets button.
    const datasetPage = await dataPage.clickAddDatasetButton();

    await datasetPage.selectCohorts([cohortName]);
    await datasetPage.selectConceptSets(['Demographics']);
    await datasetPage.clickSaveAndAnalyzeButton();

    const saveModal = new DatasetSaveModal(page);
    let datasetName = await saveModal.saveDataset({exportToNotebook: false});

    // Verify create successful.
    await dataPage.openTab(TabLabelAlias.Datasets, {waitPageChange: false});

    const resourceCard = new DataResourceCard(page);
    const dataSetExists = await resourceCard.cardExists(datasetName, CardType.Dataset);
    expect(dataSetExists).toBe(true);

    // Edit the dataset to include "All Participants".
    await resourceCard.findCard(datasetName)
    const menu = resourceCard.getEllipsis();
    await menu.clickAction(EllipsisMenuAction.Edit);
    await waitWhileLoading(page);

    await datasetPage.selectCohorts(['All Participants']);
    await datasetPage.clickAnalyzeButton();

    // Save Dataset in a new name.
    datasetName = await saveModal.saveDataset({exportToNotebook: false}, true);
    await dataPage.waitForLoad();

    await dataPage.openTab(TabLabelAlias.Datasets, {waitPageChange: false});
    await dataPage.deleteDataset(datasetName);
  });

});
