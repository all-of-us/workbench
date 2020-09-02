import WorkspaceDataPage from 'app/page/workspace-data-page';
import {Language} from 'app/text-labels';
import * as fs from 'fs';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';
import {CellType} from 'app/page/notebook-cell';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('Jupyter notebook tests in R', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Import libraries', async () => {
    const rCodeSnippet = fs.readFileSync('resources/r-code/import-libs.R', 'utf8');

    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('import-r-lib');
    const notebook = await dataPage.createNotebook(notebookName, Language.R);

    // In Code cell, autoCloseBrackets is true as default. Screws up code when Puppeteer type code line by line.
    // Worksaround: Type code in Markdown cell, then change to Code cell to run.
    const cellIndex = 1;
    const codeCell = await notebook.findCell(cellIndex);
    await codeCell.focus();
    await notebook.changeToMarkdownCell();
    const markdownCell = await notebook.findCell(cellIndex, CellType.Markdown);
    const markdownCellInput = await markdownCell.focus();
    await markdownCellInput.type(rCodeSnippet);
    await notebook.changeToCodeCell();

    const cellOutput = await notebook.runCodeCell(cellIndex, {timeOut: 5 * 60 * 1000});
    expect(cellOutput).toContain('success');

    await notebook.deleteNotebook(notebookName);
  });

});
