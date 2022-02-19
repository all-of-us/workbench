import DataResourceCard from 'app/component/data-resource-card';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { ConceptSets, MenuOption, ResourceCard, Tabs, WorkspaceAccessLevel } from 'app/text-labels';
import {
  createDataset,
  findOrCreateWorkspace,
  findWorkspaceCard,
  openTab,
  signInWithAccessToken
} from 'utils/test-utils';
import { waitWhileLoading } from 'utils/waits-utils';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import { config } from 'resources/workbench-config';
import { makeWorkspaceName } from 'utils/str-utils';
import DatasetBuildPage from 'app/page/dataset-build-page';

// 10 minutes.
jest.setTimeout(10 * 60 * 1000);

describe('Create Dataset', () => {
  const workspace = makeWorkspaceName();
  let datasetName;

  test('Create dataset with all available inputs', async () => {
    await signInWithAccessToken(page);
    await findOrCreateWorkspace(page, { workspaceName: workspace });
    datasetName = await createDataset(page, {
      conceptSets: [
        ConceptSets.Demographics,
        ConceptSets.AllSurveys,
        ConceptSets.FitbitHeartRateSummary,
        ConceptSets.FitbitActivitySummary,
        ConceptSets.FitbitHeartRateLevel,
        ConceptSets.FitbitIntraDaySteps
      ]
    });

    const resourceCard = new DataResourceCard(page);
    const dataSetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset);
    expect(dataSetCard).toBeTruthy();

    // Share workspace with a READER.
    const aboutPage = new WorkspaceAboutPage(page);
    await openTab(page, Tabs.About, aboutPage);

    await aboutPage.shareWorkspaceWithUser(config.READER_USER, WorkspaceAccessLevel.Reader);
    await waitWhileLoading(page);

    // Don't delete dataset because it's needed in next test.
  });

  test('Workspace READER cannot edit dataset', async () => {
    // READER log in in new Incognito page.
    await signInWithAccessToken(page, config.READER_USER);

    // Find workspace created by previous test. If not found, test will fail.
    const workspaceCard = await findWorkspaceCard(page, workspace);
    await workspaceCard.clickWorkspaceName(true);

    const readerDataPage = new WorkspaceDataPage(page);
    await readerDataPage.waitForLoad();

    // Create Cohorts and Datasets button are disabled.
    expect(await readerDataPage.getAddDatasetButton().isCursorNotAllowed()).toBe(true);
    expect(await readerDataPage.getAddCohortsButton().isCursorNotAllowed()).toBe(true);

    await openTab(page, Tabs.Datasets, readerDataPage);

    // Verify Snowman menu: Rename, Edit Export to Notebook and Delete actions are not available for click in Dataset card.
    const resourceCard = new DataResourceCard(page);
    const dataSetCard = await resourceCard.findCard(datasetName, ResourceCard.Dataset);
    expect(dataSetCard).toBeTruthy();

    const snowmanMenu = await dataSetCard.getSnowmanMenu();
    expect(await snowmanMenu.isOptionDisabled(MenuOption.RenameDataset)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Edit)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.ExportToNotebook)).toBe(true);
    expect(await snowmanMenu.isOptionDisabled(MenuOption.Delete)).toBe(true);

    // Although Edit option is not available to click. User can click on dataset name and see the dataset details.
    await dataSetCard.clickResourceName();
    const dataSetEditPage = new DatasetBuildPage(page);
    await dataSetEditPage.waitForLoad();

    const analyzeButton = dataSetEditPage.getAnalyzeButton();
    expect(await analyzeButton.isCursorNotAllowed()).toBe(true);

    // No matter of what has changed, the Analyze button remains disabled.
    await dataSetEditPage.selectConceptSets([ConceptSets.FitbitIntraDaySteps]);
    await dataSetEditPage.getPreviewTableButton().click();
    await waitWhileLoading(page);
    expect(await analyzeButton.isCursorNotAllowed()).toBe(true);
  });
});
