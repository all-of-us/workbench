import BaseElement from 'app/element/base-element';
import HomePage from 'app/page/home-page';
import WorkspaceCard from 'app/component/workspace-card';
import WorkspacesPage from 'app/page/workspaces-page';
import { signInWithAccessToken } from 'utils/test-utils';
import Navigation, { NavLink } from 'app/component/navigation';
import ReactSelect from 'app/element/react-select';
import { MenuOption } from 'app/text-labels';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { getPropValue } from 'utils/element-utils';

describe('Workspace UI tests', () => {
  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Tests don't require creating new workspace
  test('Workspace cards', async () => {
    // In Home page, Workspace cards have same size.
    const homePage = new HomePage(page);
    await homePage.waitForLoad();
    expect(await homePage.isLoaded()).toBe(true);

    const cards = await WorkspaceCard.findAllCards(page);
    let width;
    let height;
    for (const card of cards) {
      const cardElem = BaseElement.asBaseElement(page, card.asElementHandle());
      expect(await cardElem.isVisible()).toBe(true);
      const size = await cardElem.getSize();
      if (width === undefined) {
        width = size.width; // Initialize width and height with first card element's size, compare with rest cards
        height = size.height;
      } else {
        expect(size.height).toBeGreaterThan(0);
        expect(size.width).toBeGreaterThan(0);
        expect(size.height).toEqual(height);
        expect(size.width).toEqual(width);
      }
    }

    // In Your Workspaces page, check Workspace name, Last Changed Time and Access Role.
    await Navigation.navMenu(page, NavLink.YOUR_WORKSPACES);
    await new WorkspacesPage(page).waitForLoad();

    if ((await WorkspaceCard.findAllCards(page)).length === 0) {
      return; // End test now, no Workspace card.
    }

    // Randomly choose one card to check
    const card = await WorkspaceCard.findAnyCard(page);
    const workspaceName = await card.getWorkspaceName();
    const accessLevel = await card.getWorkspaceAccessLevel();
    const lastChangedTime = Date.parse(await card.getLastChangedTime());

    // Check workspace name string is made of english characters.
    await expect(workspaceName).toMatch(new RegExp(/^[a-zA-Z]+/));

    // Check Access Level is one of three levels
    const levels = ['WRITER', 'READER', 'OWNER'];
    expect(levels).toContain(accessLevel);

    // Check Last Changed time is valid date time.
    // Date.parse returns 'NaN' if string is not a valid time. NaN is never equal to itself.
    expect(lastChangedTime === lastChangedTime).toBe(true);

    // Check snowman menu options contains expected options.
    const snowmanMenu = await card.getSnowmanMenu();
    const links = await snowmanMenu.getAllOptionTexts();
    expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));
  });

  test('CANCEL in Edit page go back to Your Workspaces page', async () => {
    const workspaces = new WorkspacesPage(page);
    await workspaces.load();

    // Click Create New Workspace link on the Workspaces page.
    const editPage = await workspaces.clickCreateNewWorkspace();

    const dataAccessTierSelect = await editPage.getDataAccessTierSelect();
    expect(await dataAccessTierSelect.isDisabled()).toBe(false);

    const textAreaCharLimitElements = await page.$$('[data-test-id="characterLimit"]');
    for (const elm of textAreaCharLimitElements) {
      const charLimitTexts = await getPropValue<string>(elm, 'textContent');
      expect(charLimitTexts).toEqual('1000 characters remaining');
    }

    // Click Cancel button to exit Edit page.
    const cancelButton = editPage.getCancelButton();
    await cancelButton.clickAndWait();

    // Back to Your Workspaces page.
    await workspaces.waitForLoad();
    expect(await workspaces.isLoaded()).toBe(true);
  });

  test('Display workspaces by access levels', async () => {
    const filterMenuOptions = ['Owner', 'Writer', 'Reader'];

    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    // Default Filter by Select menu value is 'All'.
    const selectMenu = new ReactSelect(page, { name: 'Filter by' });
    const defaultSelectedValue = await selectMenu.getSelectedOption();
    expect(defaultSelectedValue).toEqual('All');

    // Change Workspace Filter.
    for (const menuOption of filterMenuOptions) {
      const selectedValue = await workspacesPage.filterByAccessLevel(menuOption);
      expect(selectedValue).toEqual(menuOption); // Verify selected option
      const cards = await WorkspaceCard.findAllCards(page);
      // If any card exists, get its Access Level and compare with filter level.
      for (const card of cards) {
        const cardLevel = await card.getWorkspaceAccessLevel();
        expect(cardLevel).toEqual(menuOption.toUpperCase());
      }
    }
  });

  test('Duplicate Workspace page', async () => {
    const homePage = new HomePage(page);
    await homePage.waitForLoad();

    const cards = await WorkspaceCard.findAllCards(page);
    if (cards.length === 0) {
      return; // End test now, no Workspace card.
    }

    const workspaceCard = cards[0];
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.waitForLoad();

    const pageTitle = await page.title();
    expect(pageTitle).toContain('Duplicate Workspace');

    const workspaceNameInput = await workspaceEditPage.getWorkspaceNameTextbox();
    expect(await workspaceNameInput.isDisabled()).toBe(false);

    const dataAccessTierSelect = await workspaceEditPage.getDataAccessTierSelect();
    expect(await dataAccessTierSelect.isDisabled()).toBe(true);

    const duplicateButton = await workspaceEditPage.getDuplicateWorkspaceButton();
    expect(await duplicateButton.isCursorNotAllowed()).toBe(true);

    const shareWorkspaceCheckbox = await workspaceEditPage.getShareWithCollaboratorsCheckbox();
    expect(await shareWorkspaceCheckbox.isChecked()).toBe(false);

    const cancelButton = workspaceEditPage.getCancelButton();
    await cancelButton.clickAndWait();

    // Back to Home page.
    await homePage.waitForLoad();
    expect(await homePage.isLoaded()).toBe(true);
  });

  /*

  // TODO add new test
  test('Recently Accessed Items table', async () => {
  });

   */
});
