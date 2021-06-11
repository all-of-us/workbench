import BaseElement from 'app/element/base-element';
import HomePage from 'app/page/home-page';
import WorkspacesPage from 'app/page/workspaces-page';
import { findAllCards, signInWithAccessToken } from 'utils/test-utils';
import Navigation, { NavLink } from 'app/component/navigation';
import ReactSelect from 'app/element/react-select';
import { LinkText, MenuOption, WorkspaceAccessLevel } from 'app/text-labels';
import WorkspaceEditPage from 'app/page/workspace-edit-page';
import { getPropValue } from 'utils/element-utils';
import { waitForNumericalString } from 'utils/waits-utils';
import * as fp from 'lodash/fp';
import ShareModal from 'app/modal/share-modal';

describe('Workspace UI tests', () => {
  const charLimitXpath = '//*[@data-test-id="characterLimit"]';

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  // Tests don't require creating new workspace
  test('Workspace cards in Home and All Workspaces pages', async () => {
    // In Home page, Workspace cards have same size.
    const homePage = new HomePage(page);
    await homePage.waitForLoad();
    expect(await homePage.isLoaded()).toBe(true);

    const homePageCards = await findAllCards(page);
    let width;
    let height;
    for (const card of homePageCards) {
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

    const workspacesPageCards = await findAllCards(page);
    if (workspacesPageCards.length === 0) {
      return; // End test now, no Workspace card.
    }

    // Randomly choose one card to check
    const card = workspacesPageCards[0];
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

  xtest('CANCEL in Edit page goes back to Your Workspaces page', async () => {
    const workspaces = new WorkspacesPage(page);
    await workspaces.load();

    // Click Create New Workspace link on the Workspaces page.
    const editPage = await workspaces.clickCreateNewWorkspace();

    const dataAccessTierSelect = editPage.getDataAccessTierSelect();
    expect(await dataAccessTierSelect.isDisabled()).toBe(false);

    // 1000 characters is the limit in textarea.
    const textAreaCharLimitElements = await page.$x(charLimitXpath);
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

  xtest('Workspaces display by access level filter', async () => {
    const filter = ['Owner', 'Writer', 'Reader'];

    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    const workspacesPageCards = await findAllCards(page);
    if (workspacesPageCards.length === 0) {
      return; // End test now, no Workspace card.
    }

    // Default Filter by Select menu value is 'All'.
    const selectMenu = new ReactSelect(page, { name: 'Filter by' });
    const defaultSelectedValue = await selectMenu.getSelectedOption();
    expect(defaultSelectedValue).toEqual('All');

    // Change Filter by value.
    for (const accessLevel of filter) {
      const filterSelectedValue = await workspacesPage.filterByAccessLevel(accessLevel);
      expect(filterSelectedValue).toEqual(accessLevel); // Verify selected option
      const cards = await findAllCards(page);
      // If any card exists, get its Access Level and compare with filter level.
      for (const card of cards) {
        const workspaceAccessLevel = await card.getWorkspaceAccessLevel();
        expect(workspaceAccessLevel).toEqual(accessLevel.toUpperCase());
      }
    }
  });

  xtest('Duplicate Workspace page', async () => {
    const homePage = new HomePage(page);
    await homePage.waitForLoad();

    const cards = await findAllCards(page);
    if (cards.length === 0) {
      return; // End test now, no Workspace card.
    }

    const workspaceCard = fp.shuffle(cards)[0];
    await workspaceCard.asElementHandle().hover();
    await workspaceCard.selectSnowmanMenu(MenuOption.Duplicate, { waitForNav: true });

    const workspaceEditPage = new WorkspaceEditPage(page);
    await workspaceEditPage.waitForLoad();

    const pageTitle = await page.title();
    expect(pageTitle).toContain('Duplicate Workspace');

    const workspaceNameInput = workspaceEditPage.getWorkspaceNameTextbox();
    expect(await workspaceNameInput.isDisabled()).toBe(false);

    const dataAccessTierSelect = workspaceEditPage.getDataAccessTierSelect();
    expect(await dataAccessTierSelect.isDisabled()).toBe(true);

    const duplicateButton = workspaceEditPage.getDuplicateWorkspaceButton();
    expect(await duplicateButton.isCursorNotAllowed()).toBe(true);

    const shareWorkspaceCheckbox = workspaceEditPage.getShareWithCollaboratorsCheckbox();
    expect(await shareWorkspaceCheckbox.isChecked()).toBe(false);

    // Textarea character count should be a positive number and less than 1000.
    const charCount = await waitForNumericalString(page, charLimitXpath);
    expect(parseInt(charCount)).toBeGreaterThan(1);
    expect(parseInt(charCount)).toBeLessThan(1000);

    const cancelButton = workspaceEditPage.getCancelButton();
    await cancelButton.clickAndWait();

    // Back to Home page.
    await homePage.waitForLoad();
    expect(await homePage.isLoaded()).toBe(true);
  });

  xtest('Workspace Card Snowman menu options', async () => {
    const workspacesPage = new WorkspacesPage(page);
    await workspacesPage.load();

    const cards = await findAllCards(page);
    if (cards.length === 0) {
      return; // End test now, no Workspace card.
    }
    const selectedCard = fp.shuffle(cards)[0];

    // Verify: Share, Edit, Duplicate and Delete actions are available for click.
    await selectedCard.verifyWorkspaceCardMenuOptions(WorkspaceAccessLevel.Owner);

    // Verify Share Modal is accessible.
    await selectedCard.selectSnowmanMenu(MenuOption.Share, { waitForNav: false });
    const shareModal = new ShareModal(page);
    await shareModal.waitUntilVisible();
    await shareModal.clickButton(LinkText.Cancel, { waitForClose: true });
  });

  /*

  // TODO add new test
  test('Recently Accessed Items table', async () => {
  });

   */
});
