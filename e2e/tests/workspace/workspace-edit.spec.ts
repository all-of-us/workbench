import WorkspaceDataPage from 'app/page/workspace-data-page';
import {Option, WorkspaceAccessLevel} from 'app/text-labels';
import * as testData from 'resources/data/workspace-data';
import {createWorkspace, findOrCreateWorkspace, performActions, signInWithAccessToken} from 'utils/test-utils';
import WorkspaceAboutPage from 'app/page/workspace-about-page';
import WorkspaceEditPage from 'app/page/workspace-edit-page';

describe('Editing workspace via workspace card snowman menu', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Reuse same Workspace for all tests in this file to reduce test playback time.
  // Workspace to be created in first test. If first test fails, next test will create it.
  let workspaceName: string;

  /**
   * Test:
   * - Find an existing workspace. Create a new workspace if none exists.
   * - Edit workspace: changing Question 2 answers.
   * - Verify landing in DATA tab.
   * - Verify Workspace Information in ABOUT tab.
   */
  test('User as OWNER can edit workspace', async () => {
    const workspaceCard = await createWorkspace(page);
    workspaceName = await workspaceCard.getWorkspaceName();
    await workspaceCard.selectSnowmanMenu(Option.Edit, {waitForNavi: true});

    const workspaceEditPage = new WorkspaceEditPage(page);

    // CDR Version Select is readonly. Get selected value.
    const selectedOption = await workspaceEditPage.selectCdrVersion();
    const cdrVersionSelect = await workspaceEditPage.getCdrVersionSelect();
    const selectedValue = await cdrVersionSelect.getOptionValue(selectedOption);

    // Change question #2 answer
    await performActions(page, testData.defaultAnswersResearchPurposeSummary);

    const updateButton = await workspaceEditPage.getUpdateWorkspaceButton();
    await updateButton.waitUntilEnabled();
    await workspaceEditPage.clickCreateFinishButton(updateButton);

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.waitForLoad();

    // Check Workspace Information

    // Check CDR version
    await dataPage.openAboutPage();
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
  /**
   * Test:
   * - Find an existing workspace or Create a new workspace if none exists.
   * - navigate to About page
   * - Click the workspace action menu and Edit workspace: changing Question 2 answers.
   * - Verify landing in DATA tab.
   * - Verify Workspace Information in ABOUT tab.
   */

  test('User as OWNER can edit workspace via workspace action menu', async () => {
    const workspaceCard = await findOrCreateWorkspace(page, {workspaceName});
    await workspaceCard.getWorkspaceName();

    // Verify Workspace Access Level is OWNER.
    const accessLevel = await workspaceCard.getWorkspaceAccessLevel();
    expect(accessLevel).toBe(WorkspaceAccessLevel.Owner);

    await workspaceCard.clickWorkspaceName();

    const dataPage = new WorkspaceDataPage(page);
    await dataPage.openAboutPage();
    const aboutPage = new WorkspaceAboutPage(page);
    await aboutPage.editWorkspace();

    const workspaceEditPage = new WorkspaceEditPage(page);

     // CDR Version Select is readonly. Get selected value.
    const selectedOption = await workspaceEditPage.selectCdrVersion();
    const cdrVersionSelect = await workspaceEditPage.getCdrVersionSelect();
    const selectedValue = await cdrVersionSelect.getOptionValue(selectedOption);

    // Change question #2 answer
     await performActions(page, testData.defaultAnswersResearchPurposeSummary);

     const updateButton = await workspaceEditPage.getUpdateWorkspaceButton();
     await updateButton.waitUntilEnabled();
     await workspaceEditPage.clickCreateFinishButton(updateButton);

     await dataPage.waitForLoad();

     // navigate to About Page
    await dataPage.openAboutPage();

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
