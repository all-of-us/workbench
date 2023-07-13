import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeRandomName } from 'utils/str-utils';
import { Language } from 'app/text-labels';
import expect from 'expect';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe('Create R kernel notebook', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eCreateRKernelNotebookTest';
  const rNotebookName = makeRandomName('R');

  test('Run R code', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const dataPage = new WorkspaceDataPage(page);
    const notebook = await dataPage.createNotebook(rNotebookName, Language.R);

    const kernelName = await notebook.getKernelName();
    expect(kernelName).toBe('R');

    // Run math function in Code cell [1].
    let cellIndex = 1;
    const code1Output = await notebook.runCodeCell(cellIndex, {
      codeFile: 'resources/r-code/calculate-max.R',
      markdownWorkaround: true
    });
    expect(code1Output).toEqual('[1] 20');

    // Print sys environment details in Code cell [2].
    cellIndex = 2;
    const code2Output = await notebook.runCodeCell(cellIndex, {
      codeFile: 'resources/r-code/sys-print.R',
      markdownWorkaround: true
    });
    expect(code2Output).toMatch(/success$/);

    // Import R libs in Code cell [3].
    cellIndex = 3;
    const cell3Output = await notebook.runCodeCell(cellIndex, {
      codeFile: 'resources/r-code/import-libs.R',
      markdownWorkaround: true,
      timeOut: 5 * 60 * 1000
    });
    await notebook.save();
    expect(cell3Output).toMatch(/success$/);
  });
});
