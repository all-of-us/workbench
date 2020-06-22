import {PhysicalMeasurementsCriteria} from 'app/page/cohort-criteria-modal';
import Link from 'app/element/link';
import CohortBuildPage, {FieldSelector} from 'app/page/cohort-build-page';
import DataPage from 'app/page/data-page';
import {findWorkspace, signIn, waitWhileLoading} from 'utils/test-utils';
import {waitForText} from 'utils/waits-utils';


describe('User can rename Cohorts', () => {

   beforeEach(async () => {
      await signIn(page);
   });

   /**
    * Test:
    * Find an existing workspace or create a new workspace if none exists.
    * Create new Cohort:
    *   - Add criteria in Group 1: Age range: 21 - 95
    * Renaming Group 1 and 2 names.
    */
   test('Add cohort including Demographics Age', async () => {

      const workspaceCard = await findWorkspace(page);
      await workspaceCard.clickWorkspaceName();

      // Wait for the Data page.
      const dataPage = new DataPage(page);
      // Click Add Cohorts button
      await dataPage.getAddCohortsButton().then((butn) => butn.clickAndWait());

      // In Build Cohort Criteria page.
      const cohortPage = new CohortBuildPage(page);
      await cohortPage.waitForLoad();

      // Include Participants Group 1: Age range: 21 - 95
      const minAge = 21;
      const maxAge = 95;
      const group1 = cohortPage.findIncludeParticipantsGroup('Group 1');
      const group1Count = await group1.includeAge(minAge, maxAge);

      // Checking Group 1 count. should match Group 1 participants count.
      await waitForText(page, group1Count, {xpath: group1.getGroupCountXpath()});
      const group1CountInt = Number(group1Count.replace(/,/g, ''));
      expect(group1CountInt).toBeGreaterThan(1);
      console.log('Include in Group 1 Demographics Age: Count = ' + group1CountInt);

      // Exclude Participants Group 3: Select menu Demographics -> Deceased
      const group3 = cohortPage.findExcludeParticipantsGroup('Group 3');
      const group3Count = await group3.includeDemographicsDeceased();
      const group3CountInt = Number(group3Count.replace(/,/g, ''));
      expect(group3CountInt).toBeGreaterThan(1);
      console.log('Eclude in Group 3 Demographics Deceased: Count = ' + group3CountInt);

      // Compare the new Total Count with the old Total Count.
      const totalCount = await cohortPage.getTotalCount();
      const totalCountInt = Number(totalCount.replace(/,/g, ''));
      console.log('Total Count: ' + totalCountInt);

      // Save cohort.
      const cohortName = await cohortPage.saveCohortAs();
      console.log(`Created Cohort "${cohortName}"`);

      // Click on link to cohort. Open cohort details to modify.
      const cohortLink = await Link.findByName(page, {name: cohortName});
      await cohortLink.clickAndWait();
      await waitWhileLoading(page);
      await waitForText(page, totalCount, {xpath: FieldSelector.TotalCount});

      // Remove Exclude Group 3.
      const groupCriteriasList = await group3.deleteGroup();
      expect(groupCriteriasList.length).toBe(0);

      await cohortPage.save();

      // Rename cohort

      // Clean up: delete cohort
      const dialogContent = await cohortPage.deleteCohort();
      // Verify dialog content text
      expect(dialogContent).toContain(`Are you sure you want to delete Cohort: ${cohortName}?`);
      console.log(`Deleted Cohort "${cohortName}"`);
   });



});
