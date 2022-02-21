import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { Cohorts, ConceptSets, LinkText, MenuOption, ResourceCard, Tabs } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import { createDataset, findOrCreateWorkspace, openTab, signInWithAccessToken } from 'utils/test-utils';
import DatasetRenameModal from 'app/modal/dataset-rename-modal';
import { waitForText } from 'utils/waits-utils';
import DatasetBuildPage from 'app/page/dataset-build-page';

// 10 minutes.
jest.setTimeout(10 * 60 * 1000);

describe('Dataset rename', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eDatasetRenameTest';

  test('Via snowman menu', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const datasetName = await createDataset(page, {
      cohorts: [Cohorts.AllParticipants],
      conceptSets: [ConceptSets.Demographics]
    });

    const resourceCard = new DataResourceCard(page);
    let datasetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset, {
      timeout: 30000
    });
    expect(datasetCard).toBeTruthy();

    // Verify Dataset Build or Edit page renders correctly.
    await datasetCard.selectSnowmanMenu(MenuOption.Edit, { waitForNav: true });

    const datasetEditPage = new DatasetBuildPage(page);
    await datasetEditPage.waitForLoad();

    // Verify displayed dataset name.
    const strArray = (await datasetEditPage.getDatasetName()).split(' - ');
    expect(strArray[1]).toEqual(datasetName);

    // Verify Cohort checkbox is checked.
    const cohortCheckBox = datasetEditPage.getCohortCheckBox(Cohorts.AllParticipants);
    expect(await cohortCheckBox.isChecked()).toBe(true);

    // Verify Cohort checkbox is checked.
    const conceptSetCheckBox = datasetEditPage.getConceptSetCheckBox(ConceptSets.Demographics);
    expect(await conceptSetCheckBox.isChecked()).toBe(true);

    // Export button is enabled.
    const analyzeButton = datasetEditPage.getAnalyzeButton();
    expect(await analyzeButton.isCursorNotAllowed()).toBe(false);

    // Save button is disabled.
    const saveButton = datasetEditPage.getSaveButton();
    expect(await saveButton.isCursorNotAllowed()).toBe(true);

    // Exit out Dataset Build page.
    const dataPage = new WorkspaceDataPage(page);
    await openTab(page, Tabs.Data, dataPage);
    await openTab(page, Tabs.Datasets, dataPage);

    // Rename Dataset.
    datasetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset, {
      timeout: 30000
    });
    await datasetCard.selectSnowmanMenu(MenuOption.RenameDataset, { waitForNav: false });

    const renameModal = new DatasetRenameModal(page);
    await renameModal.waitForLoad();

    const modalText = await renameModal.getTitle();
    expect(modalText).toEqual(`Enter new name for ${datasetName}`);

    // Type in same dataset name will not work.
    await renameModal.typeNewName(datasetName);
    await waitForText(page, 'New name already exists', { container: renameModal });
    expect(await renameModal.waitForButton(LinkText.RenameDataset).isCursorNotAllowed()).toBe(true);

    const newDatasetName = makeRandomName();
    await renameModal.typeNewName(newDatasetName);
    await renameModal.typeDescription('rename dataset test');
    await renameModal.clickButton(LinkText.RenameDataset, { waitForClose: true });

    // Verify existences of old and new Datasets cards.
    const newDatasetExists = await resourceCard.cardExists(newDatasetName, ResourceCard.Dataset);
    expect(newDatasetExists).toBe(true);

    const oldDatasetExists = await resourceCard.cardExists(datasetName, ResourceCard.Dataset);
    expect(oldDatasetExists).toBe(false);

    await dataPage.deleteResource(newDatasetName, ResourceCard.Dataset);
  });
});
