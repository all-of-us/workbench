import {Domain} from 'app/component/concept-domain-card';
import DataResourceCard, {CardType} from 'app/component/data-resource-card';
import WorkspaceCard from 'app/component/workspace-card';
import ConceptsetActionsPage from 'app/page/conceptset-actions-page';
import ConceptsetPage from 'app/page/conceptset-page';
import {SaveOption} from 'app/page/conceptset-save-modal';
import DataPage, {TabLabelAlias} from 'app/page/data-page';
import {LinkText, WorkspaceAccessLevel} from 'app/text-labels';
import {makeRandomName, makeString} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';
import Link from 'app/element/link';


describe('Editing and Copying Concept Sets', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * - Create new workspace.
   * - Create first new Concept Set in Procedure domain.
   * - Create second new Concept Set in Procedure domain. Add to first Concept Set.
   */
  test('Add to existing Concept Set in same workspace', async () => {
    // Create a workspace
    const workspaceCard = await findWorkspace(page, true);
    const workspaceName = await workspaceCard.getWorkspaceName();
    await workspaceCard.clickWorkspaceName();

    const dataPage = new DataPage(page);
    let conceptSearchPage = await dataPage.openConceptSearch(Domain.Procedures);

    // Select first two rows.
    const row1 = await conceptSearchPage.dataTableSelectRow(1, 1);
    const row2 = await conceptSearchPage.dataTableSelectRow(3, 1);

    console.log('Selected Procedures table row 1: ', row1);
    console.log('Selected Procedures table row 2: ', row2);

    // Verify Code are numberical values
    expect(Number.isNaN(parseInt(row1.code, 10))).toBe(false);
    expect(Number.isNaN(parseInt(row2.code, 10))).toBe(false);

    // Verify Participant Count are numberical values
    expect(Number.isNaN(parseInt(row1.participantCount.replace(/,/g, ''), 10))).toBe(false);
    expect(Number.isNaN(parseInt(row2.participantCount.replace(/,/g, ''), 10))).toBe(false);

    // Verify "Add to Set" dynamic button text label.
    let addButtonLabel = await conceptSearchPage.clickAddToSetButton();
    expect(addButtonLabel).toBe('Add (2) to set');

    // Save new Concept Set.
    const conceptName = await conceptSearchPage.saveConcept(SaveOption.CreateNewSet);
    console.log(`Created Concept Set: "${conceptName}"`);

    // Add another Concept in Procedures domain.
    const conceptsetActionPage = new ConceptsetActionsPage(page);
    conceptSearchPage = await conceptsetActionPage.openConceptSearch(Domain.Procedures);

    // Search in Procedures domain
    const searchWords = 'Screening for disorder';
    await conceptSearchPage.searchConcepts(`"${searchWords}"`); // needs double-quotes or search fails.

    // Select first row. Its name cell should match the search words.
    const row = await conceptSearchPage.dataTableSelectRow(1, 1);
    expect(row.name).toBe(searchWords);

    addButtonLabel = await conceptSearchPage.clickAddToSetButton();
    expect(addButtonLabel).toBe('Add (1) to set');

    // Save to Existing Set: Only one Concept set and it is the new Concept Set created earlier in same workspace.
    const existingConceptName = await conceptSearchPage.saveConcept(SaveOption.ChooseExistingSet);
    expect(existingConceptName).toBe(conceptName);
    console.log(`Added new Concept to existing Concept Set "${conceptName}"`);

    // Concept Set saved. Click Concept Set link. Land on Concept Set page.
    const conceptsetPage = await conceptsetActionPage.openConceptSet(conceptName);

    // Verify Concept Set name is displayed.
    const value = await conceptsetPage.getConceptName();
    expect(value).toBe(conceptName);

    // Edit Concept name and description.
    const newName = makeRandomName();
    await conceptsetPage.edit(newName, makeString(20));
    console.log(`Renamed Concept Set: "${conceptName}" to "${newName}"`);

    // Navigate to workspace Data page, then delete Concept Set
    await (new Link(page, `//a[text()="${workspaceName}"]`)).click();
    await dataPage.waitForLoad();
    await dataPage.openTab(TabLabelAlias.ConceptSets, {waitPageChange: false});
    await dataPage.deleteConceptSet(newName);
  });

  /**
   * Test:
   * - Copy Concept Set from one workspace to another workspace.
   * Note: Use existing workspaces and Concept Set when possible. Otherwise create new workspaces and Concept Sets.
   */
  test('Copy Concept Set to another workspace', async () => {
    // Find all workspaces.
    const workspaceCard = new WorkspaceCard(page);
    const allWorkspaces = await workspaceCard.getWorkspaceMatchAccessLevel(WorkspaceAccessLevel.Owner);

    // Need two workspaces.
    let workspace1: WorkspaceCard;
    let workspace2: WorkspaceCard;

    if (allWorkspaces.length > 1) {
      workspace1 = allWorkspaces[0];
      workspace2 = allWorkspaces[1];
    } else {
      // Don't have two. Create two new workspaces.
      workspace1 = await findWorkspace(page, true);
      workspace2 = await findWorkspace(page, true);
    }

    // Note: workspace1 is the Copy to workspace. workspace2 is the Copy from workspace.
    const copyToWorkspace = await workspace1.getWorkspaceName();
    const copyFromWorkspace = await workspace2.getWorkspaceName();

    // Open workspace2 Data Page.
    await workspace2.clickWorkspaceName();

    // Look for one existing Concept Set in workspace2. If none exists, create a new Concept Set.
    const dataPage = new DataPage(page);
    await dataPage.openTab(TabLabelAlias.ConceptSets, {waitPageChange: false});

    const dataCards = new DataResourceCard(page);
    const conceptCards = await dataCards.getResourceCard(CardType.ConceptSet);

    let conceptName;
    if (conceptCards.length === 0) {
      // Create new Concept Set
      const conceptSearchPage = await dataPage.openConceptSearch(Domain.Procedures);
      // Select all rows.
      await conceptSearchPage.dataTableSelectAllRows();
      const addButtonLabel = await conceptSearchPage.clickAddToSetButton();
      // Table pagination displays 20 rows. If this changes, then update the check below.
      expect(addButtonLabel).toBe('Add (20) to set');
      conceptName = await conceptSearchPage.saveConcept(SaveOption.CreateNewSet);
      console.log(`Created Concept Set: "${conceptName}"`);
      // Click on link to open Concept Set page.
      const conceptsetActionPage = new ConceptsetActionsPage(page);
      await conceptsetActionPage.openConceptSet(conceptName);
    } else {
      // Find existing Concept Set, click it to open Concept Set.
      const conceptCard = await conceptCards[0];
      conceptName = await conceptCard.getResourceName();
      const nameLink = await conceptCard.getLink();
      const value = await (await nameLink.getProperty('textContent')).jsonValue();
      expect(conceptName).toBe(value.toString());
      await nameLink.click();
    }

    // Concept Set page is open.
    const conceptsetPage = new ConceptsetPage(page);
    await conceptsetPage.waitForLoad();

    // Copy Concept Set to another workspace with new Concept name.
    const conceptSetCopyName = makeRandomName();

    const conceptCopyModal = await conceptsetPage.openCopyToWorkspaceModal(conceptName);
    await conceptCopyModal.copyToAnotherWorkspace(copyToWorkspace, conceptSetCopyName);

    await conceptCopyModal.waitForButton(LinkText.GoToCopiedConceptSet).then(butn => butn.click());

    // Verify GoTo works
    await dataPage.waitForLoad();

    const url = await page.url();
    expect(url).toContain(copyToWorkspace.replace(/-/g, ''));

    const resourceCard = new DataResourceCard(page);
    const exists = await resourceCard.cardExists(conceptSetCopyName, CardType.ConceptSet);
    expect(exists).toBe(true);

    console.log(`Copied Concept Set: "${conceptName} from workspace: "${copyFromWorkspace}" to Concept Set: "${conceptSetCopyName}" in another workspace: "${copyToWorkspace}"`)

    // Delete Concept Sets
    await dataPage.deleteConceptSet(conceptSetCopyName);
  });


});
