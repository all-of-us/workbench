import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import ReplaceFileModal from 'app/modal/replace-file-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import Link from 'app/element/link';
import { ResourceCard } from 'app/text-labels';
import { makeRandomName } from 'utils/str-utils';
import expect from 'expect';
import path from 'path';
import { getPropValue } from 'utils/element-utils';
import { waitForText } from 'utils/waits-utils';
import { takeScreenshot } from 'utils/save-file-utils';

// 30 minutes.
jest.setTimeout(30 * 60 * 1000);

describe('Notebook Upload File Test', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eTestNotebookUploadFile';
  const pyNotebookName = makeRandomName('Py3');
  const pyFileName = 'nbstripoutput-filter.py';
  const pyFilePath = path.relative(process.cwd(), __dirname + `../../../resources/python-code/${pyFileName}`);

  test('Upload file and run Python code', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const dataPage = new WorkspaceDataPage(page);

    const notebook = await dataPage.createNotebook(pyNotebookName);

    // Select File menu => Open to open Upload tab.
    const newPage = await notebook.openUploadFilePage();

    // Before upload file starts, check upload button that uploads the file doesn't exist.
    const fileUploadButtonSelector =
      '//*[@id="notebook_list"]//*[contains(@class, "new-file")]' +
      `[.//input[@class="filename_input" and @value="${pyFileName}"]]//button[text()="Upload"]`;
    await newPage.waitForXPath(fileUploadButtonSelector, { hidden: true });

    // The first dialog that open up is "Data Use Policy" dialog: verify message and close dialog.
    await notebook.acceptDataUsePolicyDialog(newPage);

    // Upload button that triggers file selection dialog.
    await notebook.chooseFile(newPage, pyFilePath);

    // Cancel upload button is visible for selected file.
    const cancelFileUploadButtonSelector =
      '//*[@id="notebook_list"]//*[contains(@class, "new-file")]' +
      `[.//input[@class="filename_input" and @value="${pyFileName}"]]//button[text()="Cancel"]`;
    await newPage.waitForXPath(cancelFileUploadButtonSelector, { visible: true });

    // Upload button that uploads the file is visible.
    const uploadButton = new Link(newPage, fileUploadButtonSelector);
    await uploadButton.focus();
    await uploadButton.click({ delay: 10 });

    // Handle "Replace file" dialog if found: Do not overwrite existing file, click CANCEL button to dismiss dialog.
    // Previously uploaded file persist because same workspace is used during the day.
    const replaceFileMessage = `There is already a file named "${pyFileName}". Do you want to replace it?`;
    const replaceFileModal = new ReplaceFileModal(newPage);
    const exists = await replaceFileModal.isLoaded();
    if (exists) {
      const modalMessage = await replaceFileModal.getText();
      expect(modalMessage).toContain(replaceFileMessage);
      await replaceFileModal.clickCancelButton();
      await newPage.waitForTimeout(500);
      console.log(`Cancel to close "Replace file" "${pyFileName}" dialog`);
    }

    // Check file size is valid.
    const fileSizeXpath =
      '//*[@id="notebook_list"]//*[contains(@class,"list_item")]' +
      `[.//a[@class="item_link"]/*[normalize-space()="${pyFileName}"]]//*[contains(@class, "file_size")]`;
    await waitForText(newPage, 'kB', { xpath: fileSizeXpath });
    const fileSizeElement = await newPage.waitForXPath(fileSizeXpath, { visible: true });
    const fileSize = await getPropValue(fileSizeElement, 'textContent');
    expect(fileSize).toEqual('1.35 kB'); // Update size if file has changed.

    // In case page has to be checked after finish.
    await takeScreenshot(newPage, 'notebook-upload-file-test.png');

    await newPage.close();
    await page.bringToFront();
    await notebook.waitForKernelIdle();

    const codeCell = notebook.findCell(1);

    const cellInput = await codeCell.focus();
    await cellInput.type(`%load ${pyFileName}`);
    await notebook.run(10000);

    await codeCell.focus();
    await notebook.run();
    const codeOutput = await codeCell.waitForOutput(30000);
    expect(codeOutput).toMatch(/success$/);

    // Save, exit notebook page then delete notebook.
    await notebook.save();
    const analysisPage = await notebook.goAnalysisPage();
    await analysisPage.deleteResource(pyNotebookName, ResourceCard.Notebook);
    await analysisPage.waitForLoad();
  });
});
