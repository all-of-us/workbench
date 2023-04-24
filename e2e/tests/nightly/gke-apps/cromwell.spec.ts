import { findOrCreateWorkspace, signInWithAccessToken } from 'utils/test-utils';
import AppsPanel from 'app/sidebar/apps-panel';
import Button from 'app/element/button';
import CromwellConfigurationPanel from 'app/sidebar/cromwell-configuration-panel';
import BaseElement from 'app/element/base-element';
import { waitForFn } from 'utils/waits-utils';
import WarningDeleteCromwellModal from 'app/modal/warning-delete-cromwell-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import { makeRandomName } from 'utils/str-utils';
import { Language } from 'app/text-labels';
import expect from 'expect';
import path from 'path';

// Cluster provisioning can take a while, so set a 20 min timeout
jest.setTimeout(20 * 60 * 1000);
const wdlFileName = 'cromwell.wdl';
const jsonFileName = 'cromwell.json';
const fileBasePath = '../../../../resources/cromwell/';
const wdlFilePath = path.relative(process.cwd(), __dirname + fileBasePath + wdlFileName);
const jsonFilePath = path.relative(process.cwd(), __dirname + fileBasePath + jsonFileName);

describe('Cromwell GKE App', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  const workspaceName = 'e2eCreateCromwellGkeAppTest';

  test('Create and delete a Cromwell GKE app', async () => {
    await findOrCreateWorkspace(page, { workspaceName });

    const appsPanel = new AppsPanel(page);
    await appsPanel.open();

    // Cromwell is not running, so it appears in unexpanded mode

    const unexpandedCromwellXPath = `${appsPanel.getXpath()}//*[@data-test-id="Cromwell-unexpanded"]`;
    const unexpandedCromwell = new Button(page, unexpandedCromwellXPath);

    expect(await unexpandedCromwell.exists()).toBeTruthy();
    await unexpandedCromwell.click();

    // clicking an unexpanded GKE App:
    // 1. closes the apps panel
    // 2. opens the config panel

    const configPanel = new CromwellConfigurationPanel(page);
    await configPanel.isVisible();

    // now we can create a Cromwell app by clicking the button on this page

    const createXPath = `${configPanel.getXpath()}//*[@id="Cromwell-cloud-environment-create-button"]`;
    const createButton = new Button(page, createXPath);
    expect(await createButton.exists()).toBeTruthy();
    await createButton.click();

    // clicking create:
    // 1. closes the config panel
    // 2. waits a few seconds
    // 3. opens the apps panel

    await page.waitForXPath(configPanel.getXpath(), { visible: false });

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

    await appsPanel.pollForStatus(expandedCromwellXpath, 'Running', 15 * 60e3);

    const deleteXPath = `${expandedCromwellXpath}//*[@data-test-id="Cromwell-delete-button"]`;
    const deleteButton = new Button(page, deleteXPath);
    expect(await deleteButton.exists()).toBeTruthy();
    await deleteButton.click();

    const warningDeleteCromwellModal = new WarningDeleteCromwellModal(page);
    expect(warningDeleteCromwellModal.isLoaded());
    await warningDeleteCromwellModal.clickYesDeleteButton();

    await appsPanel.pollForStatus(expandedCromwellXpath, 'DELETING');

    // poll for deleted (unexpanded) by repeatedly closing and opening

    const isDeleted = await waitForFn(
      async () => {
        await appsPanel.close();
        await appsPanel.open();
        const unexpanded = new Button(page, unexpandedCromwellXPath);
        return await unexpanded.exists();
      },
      10e3, // every 10 sec
      2 * 60e3 // with a 2 min timeout
    );
    expect(isDeleted).toBeTruthy();
  });

  test('Create notebook and access %s snippets', async () => {
    // Create or re-use workspace
    await findOrCreateWorkspace(page, { workspaceName });

    // Create and Open notebook
    const workspaceDataPage = new WorkspaceDataPage(page);
    const notebookName = makeRandomName('cromwell-python');
    const notebook = await workspaceDataPage.createNotebook(notebookName, Language.Python);

    // Select and run Cromwell Setup snippet from menu
    await notebook.selectSnippet('All of Us Cromwell Setup Python snippets');
    let snippetOutput = await notebook.runCodeCell(1);

    // Confirm Cromwell has not started
    // TODO: Fix the snippet to use cromshell-alpha
    const cromshell_version = snippetOutput.includes('Found cromshell_alpha') ? 'cromshell-alpha' : 'cromshell-beta';
    expect(snippetOutput.includes('CROMWELL app does not exist. Please create cromwell server from workbench')).toBe(
      true
    );

    const cromwellPanel = new CromwellConfigurationPanel(page);
    //Start Cromwell
    await cromwellPanel.startCromwellGkeApp();

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
    // Improvement: Create uploadFiles method
    await notebook.uploadFile(wdlFileName, wdlFilePath);
    await notebook.uploadFile(jsonFileName, jsonFilePath);

    // Submit wdl to cromwell
    const submitJob = await notebook.runCodeCell(2, {
      code: `!${cromshell_version} submit ${wdlFileName} ${jsonFileName}`
    });
    expect(submitJob.includes('Submitting job to server'));
    expect(submitJob.includes('"status": "Submitted"'));

    const isDeleted = await cromwellPanel.deleteCromwellGkeApp();

    expect(isDeleted).toBeTruthy();

    // Clean up: notebook and workspace
    await notebook.deleteNotebook(notebookName);
    await workspaceDataPage.deleteWorkspace();
  });
});
