import DataPage, {TabLabelAlias} from 'app/page/data-page';
import NotebookPage from 'app/page/notebook-page';
import WorkspaceAnalysisPage from 'app/page/workspace-analysis-page';
import {Language} from 'app/text-labels';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';
import DataResourceCard, {CardType} from 'app/component/data-resource-card';


describe('Notebook tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  describe('Create new notebooks', () => {

    test('Python 3 notebook', async () => {
      const workspaceCard = await findWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const dataPage = new DataPage(page);
      await dataPage.openTab(TabLabelAlias.Analysis);

      const notebookName = makeRandomName();
      const analysisPage = new WorkspaceAnalysisPage(page);
      await analysisPage.createNotebook(notebookName, Language.Python);

      const notebook = new NotebookPage(page, notebookName);
      await notebook.waitForLoad();

      const kernelName = await notebook.getKernelName();
      expect(kernelName).toBe('Python 3');

      // Always one empty code cell in an empty notebook
      const kernelState = await notebook.waitForKernelIdle();
      expect(kernelState).toBe(true);

      // Run few basic Python calls
      const code1 =
           'import sys\n' +
           'print(sys.version)';
      const result1 = await notebook.runCodeCell(1, {code: code1});
      // Python version is 3.7.6 at time of this writing. It can change.
      expect(result1).toEqual(expect.stringContaining('3.7.6'));

      const code2 = '!jupyter kernelspec list';
      const result2 = await notebook.runCodeCell(2, {code: code2});
      expect(result2).toEqual(expect.stringContaining('/usr/local/share/jupyter/kernels/python3'));

      // Delete Python notebook
      await notebook.notebookLink().then( (link) => link.click());
      await analysisPage.waitForLoad();

      await dataPage.openTab(TabLabelAlias.Analysis);
      const resourceCard = new DataResourceCard(page);
      const notebookCard = await resourceCard.findCard(notebookName, CardType.Notebook);
      expect(notebookCard).toBeTruthy();

      await analysisPage.deleteNotebook(notebookName);
    })

    // Bug in R notebook UI. See https://precisionmedicineinitiative.atlassian.net/browse/RW-5303
    test('R notebook', async () => {
      expect.assertions(5);

      const workspaceCard = await findWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      const dataPage = new DataPage(page);
      await dataPage.openTab(TabLabelAlias.Analysis);

      const notebookName = makeRandomName();
      const analysisPage = new WorkspaceAnalysisPage(page);
      await analysisPage.createNotebook(notebookName, Language.R);

      const notebook = new NotebookPage(page, notebookName);
      await notebook.waitForLoad();

      const kernelName = await notebook.getKernelName();
      expect(kernelName).toBe('R');

      const kernelState = await notebook.waitForKernelIdle();
      expect(kernelState).toBe(true);

      // Run few basic R calls in empty code cell [1]
      const code1 =
           'name <- c("Jon", "Bill", "Maria")\n' +
           'age <- c(1, 10, 20)\n' +
           'data <- data.frame(name, age)\n' +
           'print(max(data$age))';
      const result1 = await notebook.runCodeCell(1, {code: code1});
      expect(result1).toMatch(/20/);

      const code2 = 'sessionInfo()';
      const result2 = await notebook.runCodeCell(2, {code: code2});
      // R version is 3.6.2 at time of this writing. It can change.
      expect(result2).toMatch(/R version 3.6.2/);

      // Delete R notebook
      await notebook.notebookLink().then((link) => link.click());
      await analysisPage.waitForLoad();

      await dataPage.openTab(TabLabelAlias.Analysis);
      const resourceCard = new DataResourceCard(page);
      const notebookCard = await resourceCard.findCard(notebookName, CardType.Notebook);
      expect(notebookCard).toBeTruthy();

      await analysisPage.deleteNotebook(notebookName);
    })

  })

});
