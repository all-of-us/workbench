import ConceptDomainCard, {Domain} from 'app/component/concept-domain-card';
import DataResourceCard, {CardType} from 'app/component/data-resource-card';
import ConceptsetActionsPage from 'app/page/conceptset-actions-page';
import DataPage, {TabLabelAlias} from 'app/page/data-page';
import {findWorkspace, signIn} from 'utils/test-utils';
import {waitForText} from 'utils/waits-utils';

describe('Create Concept Sets from Domains', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * - Create new Concept Set from Conditions domain.
   * - Delete Concept Set.
   */
  test('Create Concept Set from Conditions domain', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    // Click Add Datasets button.
    const dataPage = new DataPage(page);
    const datasetBuildPage = await dataPage.clickAddDatasetButton();

    // Click Add Concept Sets button.
    const conceptPage = await datasetBuildPage.clickAddConceptSetsButton();

    // Start: Add a Concept in Conditions domain
    const conditionDomainCard = await ConceptDomainCard.findDomainCard(page, Domain.Conditions);

    // In Conditions domain, both Concepts and Participants counts should be non-zero numberical digits.
    const conceptsCount = await conditionDomainCard.getConceptsCount();
    const conceptsCountInt = Number(conceptsCount.replace(/,/g, ''));
    expect(conceptsCountInt).toBeGreaterThan(1);

    const participantsCount = await conditionDomainCard.getParticipantsCount();
    const participantsCountInt = Number(participantsCount.replace(/,/g, ''));
    expect(participantsCountInt).toBeGreaterThan(1);

    await conditionDomainCard.clickSelectConceptButton();

    // Hardcode Concept Condition name and code for search and verify
    const conditionCode = '59621000';
    const conditionName = 'Essential hypertension';

    // Search by Code.
    await conceptPage.searchConcepts(conditionCode);
    const rowCells = await conceptPage.dataTableSelectRow();
    // Verify condition name from search result
    expect(rowCells.name).toBe(conditionName);

    await conceptPage.clickAddToSetButton();

    // Save
    const conceptName = await conceptPage.saveConcept();

    // Verify Concept Set created successfully.
    const successMessage = `Concept Set Saved Successfully`;
    const isSuccess = await waitForText(page, successMessage);
    expect(isSuccess).toBe(true);

    const linkExists = await waitForText(page, conceptName);
    expect(linkExists).toBe(true);
    console.log(`Created Concept Set "${conceptName}"`);

    // Delete Concept Set
    await dataPage.openTab(TabLabelAlias.Data);
    await dataPage.openTab(TabLabelAlias.ConceptSets, {waitPageChange: false});

    const modalTextContent = await dataPage.deleteConceptSet(conceptName);
    expect(modalTextContent).toContain(`Are you sure you want to delete Concept Set: ${conceptName}?`);
  });

  /**
   * Test:
   * - Create new Concept Set from Drug Exposures domain.
   * - Create new Concept Set from Measurements domain.
   * - Create new Dataset using above two Concept Sets.
   * - Delete Dataset, Concept Set.
   */
  test('Create Concept Sets from Drug Exposures and Measurements domains', async () => {
    const workspaceCard = await findWorkspace(page);
    await workspaceCard.clickWorkspaceName();

    // Click Add Datasets button.
    const dataPage = new DataPage(page);
    const datasetBuildPage = await dataPage.clickAddDatasetButton();

    // Start: Create new Concept Set 1
    // Click Add Concept Sets button.
    const conceptSearchPage = await datasetBuildPage.clickAddConceptSetsButton();

    // Add new Concept in Drug Exposures domain
    const drugDomainCard = await ConceptDomainCard.findDomainCard(page, Domain.DrugExposures);

    // In Drug Exposures domain, both Concepts and Participants counts should be non-zero numberical digits.
    const conceptsCount = await drugDomainCard.getConceptsCount();
    const conceptsCountInt = Number(conceptsCount.replace(/,/g, ''));
    expect(conceptsCountInt).toBeGreaterThan(1);

    const participantsCount = await drugDomainCard.getParticipantsCount();
    const participantsCountInt = Number(participantsCount.replace(/,/g, ''));
    expect(participantsCountInt).toBeGreaterThan(1);

    await drugDomainCard.clickSelectConceptButton();

    // Hardcode drug name for search
    const drugName = 'Diphenhydramine';

    // Search by drug name
    await conceptSearchPage.searchConcepts(drugName);
    await conceptSearchPage.dataTableSelectAllRows();

    await conceptSearchPage.clickAddToSetButton();

    // Save
    const conceptName1 = await conceptSearchPage.saveConcept();
    console.log(`Created Concept Set "${conceptName1}"`);

    // Start: Create new Concept Set 2
    const conceptActionPage = new ConceptsetActionsPage(page);
    await conceptActionPage.clickCreateAnotherConceptSetButton();

    // Add new Concept in Measurements domain
    const measurementsDomainCard = await ConceptDomainCard.findDomainCard(page, Domain.Measurements);
    await measurementsDomainCard.clickSelectConceptButton();

    // Hardcode Measurements name for search
    const measurementName = 'Weight';

    // Search by drug name
    await conceptSearchPage.searchConcepts(measurementName);
    await conceptSearchPage.dataTableSelectAllRows();

    await conceptSearchPage.clickAddToSetButton();

    // Save
    const conceptName2 = await conceptSearchPage.saveConcept();
    console.log(`Created Concept Set "${conceptName2}"`);

    // Create new Dataset with two new Concept Sets
    await conceptActionPage.clickCreateDatasetButton();
    await datasetBuildPage.selectCohorts(['All Participants']);
    await datasetBuildPage.selectConceptSets([conceptName1, conceptName2]);
    const saveModal = await datasetBuildPage.clickSaveAndAnalyzeButton();
    const datasetName = await saveModal.saveDataset();

    // Verify Dataset created successful.
    await dataPage.openTab(TabLabelAlias.Data);
    await dataPage.openTab(TabLabelAlias.Datasets, {waitPageChange: false});

    const resourceCard = new DataResourceCard(page);
    const dataSetExists = await resourceCard.cardExists(datasetName, CardType.Dataset);
    expect(dataSetExists).toBe(true);

    // Delete Dataset.
    const textContent = await dataPage.deleteDataset(datasetName);
    expect(textContent).toContain(`Are you sure you want to delete Dataset: ${datasetName}?`);

    // Delete Concept Set.
    await dataPage.openTab(TabLabelAlias.ConceptSets, {waitPageChange: false});

    await dataPage.deleteConceptSet(conceptName1);
    await dataPage.deleteConceptSet(conceptName2);
  });

});
