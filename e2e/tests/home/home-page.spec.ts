import BaseElement from 'app/element/base-element';
import HomePage from 'app/page/home-page';
import WorkspaceCard from 'app/component/workspace-card';
import {signIn} from 'utils/test-utils';

describe('Home page ui tests', () => {

  beforeEach(async () => {
    await signInWithAccessToken(page);
  });

  test('Check visibility of Workspace cards', async () => {

    await checkCreateNewWorkspaceLink();

    const allCards = await WorkspaceCard.findAllCards(page);
    let width;
    let height;
    for (const card of allCards) {
      const cardElem = BaseElement.asBaseElement(page, card.asElementHandle());
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
      await expect(cardName).toMatch(new RegExp(/^[a-zA-Z]+/));

      // check Workspace Action menu for listed actions
      const snowmanMenu = await card.getSnowmanMenu();
      // Assumption: test user is workspace Owner.
      // Check Workspace Actions snowman menu displayes the right set of options.
      const links = await snowmanMenu.getAllOptionTexts();
      expect(links).toEqual(expect.arrayContaining(['Share', 'Edit', 'Duplicate', 'Delete']));
    }
  });

});

async function checkCreateNewWorkspaceLink(): Promise<void> {
  const homePage = new HomePage(page);
  const plusIcon = await homePage.getCreateNewWorkspaceLink();
  expect(plusIcon).toBeTruthy();
  const classname = await plusIcon.getProperty<string>('className');
  expect(classname).toBe('is-solid');
  const shape = await plusIcon.getAttribute('shape');
  expect(shape).toBe('plus-circle');
  const hasShape = await plusIcon.hasAttribute('shape');
  expect(hasShape).toBe(true);
  const disabled = await plusIcon.isDisabled();
  expect(disabled).toBe(false);
  const cursor = await plusIcon.getComputedStyle('cursor');
  expect(cursor).toBe('pointer');
  expect(await plusIcon.isVisible()).toBe(true);
}
