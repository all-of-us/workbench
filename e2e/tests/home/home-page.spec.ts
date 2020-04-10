import ClrIconLink from 'app/aou-elements/clr-icon-link';
import Link from 'app/aou-elements/link';
import BaseElement from 'app/aou-elements/base-element';
import HomePage, {FIELD_LABEL as editPageFieldLabel} from 'app/home-page';
import WorkspaceCard from 'app/workspace-card';
import WorkspaceEditPage from 'app/workspace-edit-page';
import WorkspacesPage from 'app/workspaces-page';
import {signIn} from 'tests/app';


describe('Home page ui tests', () => {

  beforeEach(async () => {
    await signIn(page);
  });

  afterEach(async () => {
    await jestPuppeteer.resetBrowser();
  });


  test('Check visibility of Workspace cards', async () => {
    const cards = await WorkspaceCard.getAllCards(page);
    let width;
    let height;
    for (const card of cards) {
      const cardElem = new BaseElement(page, card.asElementHandle());
      expect(await cardElem.isVisible()).toBe(true);
      const size = await cardElem.getSize();
      expect(size).toBeTruthy();
      expect(size.height).toBeGreaterThan(1);
      expect(size.width).toBeGreaterThan(1);

      if (width === undefined || height === undefined) {
        width = size.width; // Initialize width and height with first card element's size, compare with rest cards
        height = size.height;
      } else {
        expect(size.height).toEqual(height);
        expect(size.width).toEqual(width);
      }

      // check workspace name has characters
      const cardName = await card.getWorkspaceName();
      expect(cardName).toMatch(new RegExp(/^[a-zA-Z]+/));

      // check ellipsis icon existed
      expect(await card.getEllipsisIcon()).toBeTruthy();

      // Assumption: test user is workspace Owner.
      // Check Workspace Actions ellipsis dropdown displayes the right set of options
      const links = await card.getPopupLinkTextsArray();
      expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));
    }
  });

   // Click See All Workspaces link => Opens Your Workspaces page
  test('Click on See All Workspace link', async () => {
    const seeAllWorkspacesLink = await Link.forLabel(page, editPageFieldLabel.SEE_ALL_WORKSPACES);
    await seeAllWorkspacesLink.click();
    const workspaces = new WorkspacesPage(page);
    await workspaces.waitForLoad();
    expect(await workspaces.isLoaded()).toBe(true);
    await seeAllWorkspacesLink.dispose();
  });

   // Click Create New Workspace link => Opens Create Workspace page
  test('Click on Create New Workspace link', async () => {
    const home = new HomePage(page);
    await home.getCreateNewWorkspaceLink().then((link) => link.click());

    const workspaceEdit = new WorkspaceEditPage(page);
    await workspaceEdit.waitForLoad();
    // expect Workspace name Input textfield exists and NOT disabled
    const workspaceNameTextbox = await workspaceEdit.getWorkspaceNameTextbox();
    expect(await workspaceNameTextbox.isVisible()).toBe(true);
  });

  test('Check Create New Workspace link on Home page', async () => {
    const anyLink = await ClrIconLink.forLabel(page, {normalizeSpace: editPageFieldLabel.CREATE_NEW_WORKSPACE}, 'plus-circle');
    expect(anyLink).toBeTruthy();
    const classname = await anyLink.getProperty('className');
    expect(classname).toBe('is-solid');
    const shape = await anyLink.getAttribute('shape');
    expect(shape).toBe('plus-circle');
    const hasShape = await anyLink.hasAttribute('shape');
    expect(hasShape).toBe(true);
    const disabled = await anyLink.isDisabled();
    expect(disabled).toBe(false);
    const cursor = await anyLink.getComputedStyle('cursor');
    expect(cursor).toBe('pointer');
    expect(await anyLink.isVisible()).toBe(true);

    await anyLink.dispose();
    expect(await anyLink.isVisible()).toBe(false);
  });


});
