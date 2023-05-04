import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import AppsPanel from 'app/sidebar/apps-panel';
import CromwellConfigurationPanel from 'app/sidebar/cromwell-configuration-panel';
import BaseElement from 'app/element/base-element';
import { waitForFn } from 'utils/waits-utils';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeRandomName } from 'utils/str-utils';
import { Language } from 'app/text-labels';
import path from 'path';

// Cluster provisioning can take a while, so set a 20 min timeout
jest.setTimeout(20 * 60 * 1000);
const wdlFileName = 'hello.wdl';
const jsonFileName = 'empty.json';
const fileBasePath = '../../../../resources/cromwell/';
const wdlFilePath = path.relative(process.cwd(), __dirname + fileBasePath + wdlFileName);
const jsonFilePath = path.relative(process.cwd(), __dirname + fileBasePath + jsonFileName);

// The use of %s in the following commands is temp, once docker image is updated, we will always use
// cromshell which will be alias for cromshell-beta
const cromshellSubmitPythonCmd = `!%s submit ${wdlFileName} ${jsonFileName}`;
const cromshellSubmitRCmd =
  `system2('%s', args = c('submit', '${wdlFileName}','${jsonFileName}'), ` + ' stdout = TRUE, stderr = TRUE)';

describe('Cromwell GKE App', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('Create and delete a Cromwell GKE app', async () => {
    await findOrCreateWorkspace(page, { workspaceName: 'e2eCreateCromwellGkeAppsPanelTest' });

    const configPanel = new CromwellConfigurationPanel(page);

    await configPanel.startCromwellGkeApp();

    // 1. closes the config panel
    // 2. waits a few seconds
    // 3. opens the apps panel

    await page.waitForXPath(configPanel.getXpath(), { visible: false });

    const appsPanel = new AppsPanel(page);
    await appsPanel.isVisible();
    const expandedCromwellXpath = `${appsPanel.getXpath()}//*[@data-test-id="Cromwell-expanded"]`;
    await page.waitForXPath(expandedCromwellXpath);

    // the Cromwell status should say PROVISIONING

    const cromwellText = await BaseElement.asBaseElement(
      page,
      await page.waitForXPath(expandedCromwellXpath)
    ).getTextContent();
    expect(cromwellText).toContain('Status: PROVISIONING');
    console.log('Cromwell status: PROVISIONING');

    // Poll until cromwell is running
    await appsPanel.pollForStatus(expandedCromwellXpath, 'Running', 15 * 60e3);

    // Cromwell is running, now lets delete it
    const isDeleted = await appsPanel.deleteCromwellGkeApp();
    expect(isDeleted).toBeTruthy();

    // Clean up: Delete workspace
    const workspaceDataPage = new WorkspaceDataPage(page);
    await workspaceDataPage.deleteWorkspace();
  });

  test.each([
    [Language.Python, 'All of Us Cromwell Setup Python snippets', cromshellSubmitPythonCmd],
    [Language.R, 'All of Us Cromwell Setup snippets', cromshellSubmitRCmd]
  ])('Run cromwell using notebook', async (language, snippetMenu, cromshellSubmitCommand) => {
    const appsPanel = new AppsPanel(page);
    const cromwellPanel = new CromwellConfigurationPanel(page);

    // Create or re-use workspace
    await findOrCreateWorkspace(page, { workspaceName: 'e2eSubmitCromwellJobsTest' });

    // Create and Open notebook
    const workspaceDataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName(`cromwell-${language}`);
    const notebook = await workspaceDataPage.createNotebook(notebookName, language);

    // Select and run Cromwell Setup snippet from menu
    await notebook.selectSnippet(snippetMenu, '(1) Setup');
    let snippetOutput = await notebook.runCodeCell(1);

    // Confirm Cromwell has not started
    /* The snippet output is usually like
      Scanning for cromshell 2 beta
      Scanning for cromshell 2 alpha...
      Found cromshell-alpha, please use cromshell-alpha
      Checking status for CROMWELL app....
     */
    // We are trying to find the correct version cromshell-alpha or cromshell-beta
    // This is temp, once docker image is updated, we will always use cromshell which will be alias for cromshell-beta
    const cromshell_version = snippetOutput.split(', please use ')[1].split('\n')[0];
    expect(snippetOutput.includes('CROMWELL app does not exist. Please create cromwell server from workbench')).toBe(
      true
    );

    //Start Cromwell
    await cromwellPanel.startCromwellGkeApp();
    await appsPanel.close();

    // Re-run the code snippet to check PROVISIONING state
    snippetOutput = await notebook.runCodeCell(1);
    expect(snippetOutput.includes('Existing CROMWELL app found')).toBe(true);
    expect(snippetOutput.includes('app_status=PROVISIONING')).toBe(true);

    // Re-run code snippet until cromwell is running
    const success = await waitForFn(
      async () => {
        snippetOutput = await notebook.runCodeCell(1);
        console.log(snippetOutput);
        expect(snippetOutput.includes('Existing CROMWELL app found')).toBe(true);
        return snippetOutput.includes('app_status=RUNNING');
      },
      15e3,
      10 * 60e3
    );

    expect(success).toBe(true);

    // Upload wdl and json files to notebook
    // TODO: Improvement: Create uploadFiles method
    await notebook.uploadFile(wdlFileName, wdlFilePath);
    await notebook.uploadFile(jsonFileName, jsonFilePath);

    // Submit wdl to cromwell
    const submitJob = await notebook.runCodeCell(2, {
      code: cromshellSubmitCommand.replace('%s', cromshell_version)
    });
    expect(submitJob.includes('Submitting job to server'));
    expect(submitJob.includes('"status": "Submitted"'));

    // TODO: Add more steps for the submitted cromwell jobs like confirm its running etc
    // In the meantime we will go ahead, delete cromwell and start cleanup
    const isDeleted = await appsPanel.deleteCromwellGkeApp();

    expect(isDeleted).toBeTruthy();

    // Clean up: notebook and workspace
    await notebook.deleteNotebook(notebookName);
    await workspaceDataPage.deleteWorkspace();
  });
});
