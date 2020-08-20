import DataPage, {TabLabelAlias} from 'app/page/data-page';
import WorkspacesPage from 'app/page/workspaces-page';
import {EllipsisMenuAction} from 'app/text-labels';
import * as testData from 'resources/data/workspace-data';
import {pickWorkspace, performActions, signIn} from 'utils/test-utils';
import WorkspaceAboutPage from 'app/page/workspace-about-page';


describe('Editing workspace thru workspace card ellipsis menu', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Edit workspace: changing Question 2 answers.
   * - Verify landing in DATA tab.
   * - Verify Workspace Information in ABOUT tab.
   */
  test('User as OWNER can edit workspace', async () => {

    const workspaceCard = await pickWorkspace(page, {create: true});
    await (workspaceCard.getEllipsis()).clickAction(EllipsisMenuAction.Edit);

    const workspacesPage = new WorkspacesPage(page);

    // Synthetic Dataset Select is readonly. Get selected value.
    const selectedOption = await workspacesPage.selectDataset();
    const datasetSelect = await workspacesPage.getDatasetSelect();
    const selectedValue = await datasetSelect.getOptionValue(selectedOption);
    console.log(selectedValue);

    // Change question #2 answer
    await performActions(page, testData.defaultAnswersResearchPurposeSummary);

    const updateButton = await workspacesPage.getUpdateWorkspaceButton();
    await updateButton.waitUntilEnabled();
    await workspacesPage.clickCreateFinishButton(updateButton);

    const dataPage = new DataPage(page);
    await dataPage.waitForLoad();

    // Check Workspace Information

    // Check CDR version
    await dataPage.openTab(TabLabelAlias.About);
    const aboutPage = new WorkspaceAboutPage(page);
    const cdrValue = await aboutPage.getCdrVersion();
    expect(cdrValue).toEqual(expect.stringContaining(selectedValue));

    // Check CreationDate string is a valid date and compare year string.
    const creationDate = await aboutPage.getCreationDate();
    // Current AoU Date format: "Tue Jun 16 2020"
    const splitDate = creationDate.split(' ');
    const year = splitDate[splitDate.length - 1];
    const utcYear = new Date(creationDate).getFullYear();
    expect(Number(year)).toBe(utcYear);
    expect(!isNaN(Date.parse(creationDate))).toBe(true);

    // Check LastUpdatedDate (Same date format as in Creation Date) string is a valid date format.
    const lastUpdatedDate = await aboutPage.getLastUpdatedDate();
    expect(!isNaN(Date.parse(lastUpdatedDate))).toBe(true);

    // LastUpdatedDate should be today date. See format option https://www.w3schools.com/jsref/jsref_tolocalestring.asp
    const todayWeekday = new Date().toLocaleString('en-us', { weekday: 'short' });
    const todayYear = new Date().toLocaleString('en-us', { year: 'numeric' });
    const todayDay = new Date().toLocaleString('en-us', { day: '2-digit' });

    const lastUpdatedWeekday = new Date(lastUpdatedDate).toLocaleString('en-us', { weekday: 'short' });
    const lastUpdatedYear = new Date(lastUpdatedDate).toLocaleString('en-us', { year: 'numeric' });
    const lastUpdatedDay = new Date(lastUpdatedDate).toLocaleString('en-us', { day: '2-digit' });

    expect(todayWeekday).toBe(lastUpdatedWeekday);
    expect(todayYear).toBe(lastUpdatedYear);
    expect(todayDay).toBe(lastUpdatedDay);

  });

});
