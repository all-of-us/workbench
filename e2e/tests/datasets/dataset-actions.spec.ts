import DataResourceCard from 'app/component/card/data-resource-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { Cohorts, ConceptSets, LinkText, MenuOption, ResourceCard, Tabs } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import {
  cleanupWorkspace,
  createDataset,
  findOrCreateWorkspace,
  openTab,
  signInWithAccessToken
} from 'utils/test-utils';
import DatasetRenameModal from 'app/modal/dataset-rename-modal';
import DatasetBuildPage from 'app/page/dataset-build-page';
import { waitForText } from '../../utils/waits-utils';

// 10 minutes.
jest.setTimeout(10 * 60 * 1000);

describe.skip('Dataset rename', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eDatasetActionTest';

  test('Can edit dataset via snowman menu', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const datasetName = await createDataset(page, {
      cohorts: [Cohorts.AllParticipants],
      conceptSets: [ConceptSets.Demographics]
    });
    const resourceCard = new DataResourceCard(page);
    const datasetNameCell = await resourceCard.findNameCellLinkFromTable({
      name: datasetName
    });
    expect(datasetNameCell).toBeTruthy();
    // Verify Dataset Build or Edit page renders correctly.
    await resourceCard.selectSnowmanMenu(MenuOption.Edit, { name: datasetName, waitForNav: true });

    const datasetEditPage = new DatasetBuildPage(page);
    await datasetEditPage.waitForLoad();

    // Verify displayed dataset name.
    const strArray = (await datasetEditPage.getDatasetName()).split(' - ');
    expect(strArray[1]).toEqual(datasetName);

    // Verify Cohort checkbox is checked.
    const cohortCheckBox = datasetEditPage.getCohortCheckBox(Cohorts.AllParticipants);
    expect(await cohortCheckBox.isChecked()).toBe(true);

    // Verify Concept set checkbox is checked.
    const conceptSetCheckBox = datasetEditPage.getConceptSetCheckBox(ConceptSets.Demographics);
    expect(await conceptSetCheckBox.isChecked()).toBe(true);

    // Export button is enabled.
    const analyzeButton = datasetEditPage.getAnalyzeButton();
    expect(await analyzeButton.isCursorNotAllowed()).toBe(false);

    // Save button is disabled.
    const saveButton = datasetEditPage.getSaveButton();
    expect(await saveButton.isCursorNotAllowed()).toBe(true);

    await datasetEditPage.selectConceptSets([ConceptSets.AllSurveys]);

    await datasetEditPage.getSaveButton().clickAndWait();

    const dataPage = new WorkspaceDataPage(page);
    await openTab(page, Tabs.Data, dataPage);

    // Verify Dataset Build or Edit page renders correctly.
    await resourceCard.selectSnowmanMenu(MenuOption.Edit, { name: datasetName, waitForNav: true });

    await datasetEditPage.waitForLoad();
    const conceptSetCheckBox1 = datasetEditPage.getConceptSetCheckBox(ConceptSets.AllSurveys);
    const conceptSetCheckBox2 = datasetEditPage.getConceptSetCheckBox(ConceptSets.Demographics);
    expect(await conceptSetCheckBox1.isChecked()).toBe(true);
    expect(await conceptSetCheckBox2.isChecked()).toBe(true);
  });

  test('Can rename dataSet Via snowman menu', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const datasetName = await createDataset(page, {
      cohorts: [Cohorts.AllParticipants],
      conceptSets: [ConceptSets.Demographics]
    });

    const resourceCard = new DataResourceCard(page);
    const datasetNameCell = await resourceCard.findNameCellLinkFromTable({
      name: datasetName
    });
    expect(datasetNameCell).toBeTruthy();

    // Rename Dataset.
    await resourceCard.selectSnowmanMenu(MenuOption.RenameDataset, { name: datasetName, waitForNav: false });

    const renameModal = new DatasetRenameModal(page);
    await renameModal.waitForLoad();

    const modalText = await renameModal.getTitle();
    expect(modalText).toEqual(`Enter new name for ${datasetName}`);

    // Type in same dataset name will not work.
    await renameModal.typeNewName(datasetName);
    await waitForText(page, 'New name already exists', { container: renameModal });
    expect(await renameModal.waitForButton(LinkText.RenameDataset).isCursorNotAllowed()).toBe(true);

    // lets add a new name
    const newDatasetName = makeRandomName();
    await renameModal.typeNewName(newDatasetName);
    await renameModal.typeDescription('rename dataset test');
    await renameModal.clickButton(LinkText.RenameDataset, { waitForClose: true });

    // Verify existences of old and new Datasets rows.
    const newDatasetExists = await resourceCard.findNameCellLinkFromTable({ name: newDatasetName });
    expect(newDatasetExists).not.toBeNull();

    const oldDatasetExists = await resourceCard.findNameCellLinkFromTable({ name: datasetName });
    expect(oldDatasetExists).toBeNull();

    const dataPage = new WorkspaceDataPage(page);
    await openTab(page, Tabs.Data, dataPage);
    await dataPage.deleteResourceFromTable(newDatasetName, ResourceCard.Dataset);
  });

  afterAll(async () => {
    await signInWithAccessToken(page);
    await cleanupWorkspace(page, workspaceName);
  });
});
