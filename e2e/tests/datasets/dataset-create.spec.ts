import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { LinkText, MenuOption, ResourceCard, WorkspaceAccessLevel } from 'app/text-labels';
import { findOrCreateWorkspace, findWorkspaceCard, signInAs, signInWithAccessToken } from 'utils/test-utils';
import { waitWhileLoading } from 'utils/waits-utils';
import DatasetEditPage from 'app/page/dataset-edit-page';
import WorkspacesPage from 'app/page/workspaces-page';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import { config } from 'resources/workbench-config';
import { makeWorkspaceName } from 'utils/str-utils';

// 10 minutes.
jest.setTimeout(10 * 60 * 1000);

describe('Create Dataset', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspace = makeWorkspaceName();
  let datasetName;

  test('Cannot create dataset when required inputs are blank', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    // Click Add Dataset button.
    const dataPage = new WorkspaceDataPage(page);
    const datasetPage = await dataPage.clickAddDatasetButton();

    // Select Values (Columns): Select All checkbox is disabled.
    expect(await datasetPage.getSelectAllCheckbox().isDisabled()).toBe(true);

    // Step 1 Select Cohort: Choose "All Participants"
    await datasetPage.selectCohorts(['All Participants']);

    // Export button is disabled.
    const exportButton = datasetPage.getExportButton();
    expect(await exportButton.isCursorNotAllowed()).toBe(true);

    // Create Dataset button is disabled.
    const createDatasetButton = datasetPage.getCreateDatasetButton();
    expect(await createDatasetButton.isCursorNotAllowed()).toBe(true);

    // Select Values (Columns): Select All checkbox is disabled.
    expect(await datasetPage.getSelectAllCheckbox().isDisabled()).toBe(true);

    // Step 2 Select Concept Sets (Rows): select Demographics.
    await datasetPage.selectConceptSets([LinkText.Demographics]);

    // Export button is disabled.
    expect(await exportButton.isCursorNotAllowed()).toBe(true);

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
    expect(await exportButton.isCursorNotAllowed()).toBe(true);

    // Create Dataset button is enabled.
    expect(await createDatasetButton.isCursorNotAllowed()).toBe(false);

    // Step 1 uncheck "All Participants".
    await datasetPage.unselectCohort('All Participants');

    // Export button is disabled.
    expect(await exportButton.isCursorNotAllowed()).toBe(true);

    // Create Dataset button is disabled.
    expect(await createDatasetButton.isCursorNotAllowed()).toBe(true);

    // View Preview Table button is disabled.
    expect(await datasetPage.getPreviewTableButton().isCursorNotAllowed()).toBe(true);

    // Go to Workspaces page. There is no Discard Changes warning.
    await datasetPage.getBackToWorkspacesLink().clickAndWait();

    await new WorkspacesPage(page).waitForLoad();
  });

  test('Create dataset with all available inputs', async () => {
    await findOrCreateWorkspace(page, { workspaceName: workspace });

    // Click Add Dataset button.
    let dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();
    const datasetBuildPage = await dataPage.clickAddDatasetButton();

    // Step 1 Select Cohort: Choose "All Participants"
    await datasetBuildPage.selectCohorts(['All Participants']);

    // Step 2 Select Concept Sets (Rows): select all checkboxes.
    await datasetBuildPage.selectConceptSets([LinkText.Demographics]);
    await datasetBuildPage.selectConceptSets([LinkText.AllSurveys]);
    await datasetBuildPage.selectConceptSets([LinkText.FitbitHeartRateSummary]);
    await datasetBuildPage.selectConceptSets([LinkText.FitbitActivitySummary]);
    await datasetBuildPage.selectConceptSets([LinkText.FitbitHeartRateLevel]);
    await datasetBuildPage.selectConceptSets([LinkText.FitbitIntraDaySteps]);

    // Preview table exists and has one or more table rows.
    const previewTable = await datasetBuildPage.getPreviewTable();
    expect(await previewTable.exists()).toBe(true);
    expect(await previewTable.getRowCount()).toBeGreaterThan(1);

    const createModal = await datasetBuildPage.clickCreateButton();
    datasetName = await createModal.createDataset();

    // Verify dataset card in Data page.
    dataPage = await datasetBuildPage.clickDataTab();
    await dataPage.openDatasetsSubtab();
    await waitWhileLoading(page);

    const resourceCard = new DataResourceCard(page);
    const dataSetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset);
    expect(dataSetCard).toBeTruthy();

    // Share workspace with a READER.
    await dataPage.openAboutPage();
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.waitForLoad();

    const shareWorkspaceModal = await aboutPage.shareWorkspace();
    await shareWorkspaceModal.shareWithUser(config.readerUserName, WorkspaceAccessLevel.Reader);
    await waitWhileLoading(page);

    // Don't delete dataset because it's needed in next test.
  });

  test('Workspace READER cannot edit dataset', async () => {
    // READER log in in new Incognito page.
    const newPage = await signInAs(config.readerUserName, config.userPassword);

    // Find workspace created by previous test. If not found, test will fail.
    const workspaceCard = await findWorkspaceCard(newPage, workspace);
    await workspaceCard.clickWorkspaceName(true);

    const readerDataPage = new WorkspaceDataPage(newPage);
    await readerDataPage.waitForLoad();

    // Create Cohorts and Datasets button are disabled.
    expect(await readerDataPage.getAddDatasetButton().isCursorNotAllowed()).toBe(true);
    expect(await readerDataPage.getAddCohortsButton().isCursorNotAllowed()).toBe(true);

    await readerDataPage.openDatasetsSubtab();
    await waitWhileLoading(newPage);

    // Verify Snowman menu: Rename, Edit Export to Notebook and Delete actions are not available for click in Dataset card.
    const resourceCard = new DataResourceCard(newPage);
    const dataSetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset);
    expect(dataSetCard).toBeTruthy();

    const snowmanMenu = await dataSetCard.getSnowmanMenu();
    expect(await snowmanMenu.isOptionDisabled(MenuOption.RenameDataset)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.ExportToNotebook)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(true);

    // Although Edit option is not available to click. User can click on dataset name and see the dataset details.
    await dataSetCard.clickResourceName();
    const dataSetEditPage = new DatasetEditPage(newPage);
    await dataSetEditPage.waitForLoad();

    const exportButton = dataSetEditPage.getExportButton();
    expect(await exportButton.isCursorNotAllowed()).toBe(true);

    // No matter of what has changed, the Analyze button remains disabled.
    await dataSetEditPage.selectConceptSets([LinkText.FitbitIntraDaySteps]);
    await dataSetEditPage.getPreviewTableButton().click();
    await waitWhileLoading(newPage);
    expect(await exportButton.isCursorNotAllowed()).toBe(true);
  });
});
