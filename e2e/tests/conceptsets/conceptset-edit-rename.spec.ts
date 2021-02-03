import ConceptDomainCard, {Domain} from 'app/component/concept-domain-card';
import Link from 'app/element/link';
import ConceptSetActionsPage from 'app/page/conceptset-actions-page';
import {SaveOption} from 'app/modal/conceptset-save-modal';
import WorkspaceDataPage from 'app/page/workspace-data-page';
import {ResourceCard} from 'app/text-labels';
import {makeRandomName, makeString} from 'utils/str-utils';
import {createWorkspace, signInWithAccessToken} from 'utils/test-utils';

describe('Editing and rename Concept Set', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  /**
   * Test:
   * - Create new workspace.
   * - Create first new Concept Set in Procedure domain.
   * - Add new concept to first (existing) Concept Set.
   * - Rename Concept Set.
   * - Delete Concept Set.
   */
  test('Workspace OWNER can edit Concept Set', async () => {

    const workspaceName = await createWorkspace(page).then(card => card.clickWorkspaceName());

    const dataPage = new WorkspaceDataPage(page);
    let {conceptSearchPage, criteriaSearch} = await dataPage.openConceptSetSearch(Domain.Procedures);

    // Search by Procedure name.
    let procedureName = 'Radiologic examination';
    await criteriaSearch.searchCriteria(procedureName);

    // Select first two rows.
    const row1 = await criteriaSearch.resultsTableSelectRow(1, 1);
    const row2 = await criteriaSearch.resultsTableSelectRow(2, 1);

    console.log('Selected Procedures table row 1: ', row1);
    console.log('Selected Procedures table row 2: ', row2);

    // Verify Code are numberical values
    expect(Number.isNaN(parseInt(row1.code, 10))).toBe(false);
    expect(Number.isNaN(parseInt(row2.code, 10))).toBe(false);

    // Verify Participant Count are numberical values
    expect(Number.isNaN(parseInt(row1.rollUpCount.replace(/,/g, ''), 10))).toBe(false);
    expect(Number.isNaN(parseInt(row2.rollUpCount.replace(/,/g, ''), 10))).toBe(false);

    await conceptSearchPage.reviewAndSaveConceptSet();

    // Save new Concept Set.
    const conceptSetName = await conceptSearchPage.saveConceptSet(SaveOption.CreateNewSet);

    // Add another Concept in Procedures domain.
    const conceptSetActionsPage = new ConceptSetActionsPage(page);
    conceptSearchPage = await conceptSetActionsPage.openConceptSearch();

    const procedures = await ConceptDomainCard.findDomainCard(page, Domain.Procedures);
    criteriaSearch = await procedures.clickSelectConceptButton();

    // Search in Procedures domain
    procedureName = 'Screening procedure';
    await criteriaSearch.searchCriteria(procedureName);

    // Select first row. Its name cell should match the search words.
    const row = await criteriaSearch.resultsTableSelectRow();
    expect(row.name).toBe(procedureName);

    await conceptSearchPage.reviewAndSaveConceptSet();

    // Save to Existing Set: Only one Concept set and it is the new Concept Set created earlier in same workspace.
    const existingConceptSetName = await conceptSearchPage.saveConceptSet(SaveOption.ChooseExistingSet);
    expect(existingConceptSetName).toBe(conceptSetName);
    console.log(`Added new Concept to existing Concept Set "${conceptSetName}"`);

    // Concept Set saved. Click Concept Set link. Land on Concept Set page.
    const conceptSetPage = await conceptSetActionsPage.openConceptSet(conceptSetName);

    // Verify Concept Set name is displayed.
    const value = await conceptSetPage.getConceptSetName();
    expect(value).toBe(conceptSetName);

    // Edit Concept name and description.
    const newName = makeRandomName();
    await conceptSetPage.edit(newName, makeString(20));
    console.log(`Renamed Concept Set: "${conceptSetName}" to "${newName}"`);

    // Navigate to workspace Data page, then delete Concept Set
    await (new Link(page, `//a[text()="${workspaceName}"]`)).click();
    await dataPage.waitForLoad();
    await dataPage.openConceptSetsSubtab();
    await dataPage.deleteResource(newName, ResourceCard.ConceptSet);
  });


});
