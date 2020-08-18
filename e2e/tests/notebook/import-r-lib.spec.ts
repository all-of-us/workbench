import DataPage from 'app/page/data-page';
import {Language} from 'app/text-labels';
import * as fs from 'fs';
import {config} from 'resources/workbench-config';
import {makeRandomName} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';
import {CellType} from 'app/page/notebook-cell';

// Notebook server start may take a long time. Set maximum test running time to 20 minutes.
jest.setTimeout(20 * 60 * 1000);

describe('R libraries', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  test('Print sys environment details', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new DataPage(page);
    const notebookName = makeRandomName('print-sys');
    const notebook = await dataPage.createNotebook(notebookName, Language.R);

    const outputText = await notebook.runCodeCell(1, {codeFile: 'resources/r-code/sys-print.R'});

    await notebook.deleteNotebook(notebookName);

    // UserEmail and Google-bucket is expected to be printed out.
    const email = config.userEmail
    expect(outputText).toEqual(expect.stringMatching(email));
    expect(outputText).toEqual(expect.stringMatching('gs://fc-secure-'))
  });

  test('Import common lib', async () => {
    const rCodeSnippet = fs.readFileSync('resources/r-code/import-libs.R', 'utf8');

    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    const dataPage = new DataPage(page);
    const notebookName = makeRandomName('import-r-lib');
    const notebook = await dataPage.createNotebook(notebookName, Language.R);

    // Note: In R language, autoCloseBrackets is true as default. It screws up code in Code cell
    //  when Puppeteer is typing code one line at a time.
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

    await notebook.deleteNotebook(notebookName);

    expect(cellOutput).toEqual('data.tableTRUEdplyrTRUEggplot2TRUEggthemesTRUEglueTRUEgridTRUEgridExtraTRUEjsonliteTRUEreticulateTRUEscalesTRUE');
  });

});
