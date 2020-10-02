import {Domain} from 'app/component/concept-domain-card';
import Link from 'app/element/link';
import ConceptsetActionsPage from 'app/page/conceptset-actions-page';
import {SaveOption} from 'app/page/conceptset-save-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {ResourceCard} from 'app/text-labels';
import {makeRandomName, makeString} from 'utils/str-utils';
import {findWorkspace, signIn} from 'utils/test-utils';


describe('Editing and rename Concept Sets', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * - Create new workspace.
   * - Create first new Concept Set in Procedure domain.
   * - Add new concept to first (existing) Concept Set.
   * - Rename first Concept Set.
   * - Delete first Concept Set.
   */
  test('Workspace OWNER can edit Concept Set', async () => {
    // Create a workspace
    const workspaceName = await findWorkspace(page, {create: true}).then(card => card.clickWorkspaceName());

    const dataPage = new WorkspaceDataPage(page);
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
    await dataPage.openConceptSetsSubtab({waitPageChange: false});
    await dataPage.deleteResource(newName, ResourceCard.ConceptSet);
  });


});
