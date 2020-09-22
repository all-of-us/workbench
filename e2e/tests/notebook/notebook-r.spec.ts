import WorkspaceDataPage from 'app/page/workspace-data-page';
import {Language} from 'app/text-labels';
import fs from 'fs';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';
import {CellType} from 'app/page/notebook-cell';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Jupyter notebook tests in R language', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Run code from file', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const notebookName = makeRandomName('r-notebook');
    const dataPage = new WorkspaceDataPage(page);
    const notebook = await dataPage.createNotebook(notebookName, Language.R);

    const kernelName = await notebook.getKernelName();
    expect(kernelName).toBe('R');

    // Run math function in Code cell [1].
    let cellIndex = 1;
    const code1Output = await notebook.runCodeCell(cellIndex, {codeFile: 'resources/r-code/calculate-max.R'});
    expect(code1Output).toEqual('[1] 20');

    // Print sys environment details in Code cell [2].
    cellIndex = 2;
    const code2Output = await notebook.runCodeCell(cellIndex, {codeFile: 'resources/r-code/sys-print.R'});
    expect(code2Output).toContain('success');

    // Import R libs in Code cell [3].
    cellIndex = 3;
    const rCodeSnippet = fs.readFileSync('resources/r-code/import-libs.R', 'utf8');
    // In Code cell, autoCloseBrackets is true as default and it screws up the R code when Puppeteer types code line by line.
    // Worksaround: Type code in Markdown cell, then change to Code cell to run.
    const codeCell = await notebook.findCell(cellIndex);
    await codeCell.focus();
    await notebook.changeToMarkdownCell();
    const markdownCell = await notebook.findCell(cellIndex, CellType.Markdown);
    const markdownCellInput = await markdownCell.focus();
    await markdownCellInput.type(rCodeSnippet);
    await notebook.changeToCodeCell();

    const cell3Output = await notebook.runCodeCell(cellIndex, {timeOut: 5 * 60 * 1000});
    expect(cell3Output).toContain('success');

    // Delete R notebook
    await notebook.deleteNotebook(notebookName);
  })

});
