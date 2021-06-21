import HomePage from 'app/page/home-page';
import { Page } from 'puppeteer';
import { withPage } from 'libs/test-page-manager';
import { signIn } from 'utils/test-utils';
import { config } from 'resources/workbench-config';

describe('Home page ui tests', () => {
  test('Check visibility of Workspace cards', async () => {
    await withPage()(async (page) => {
      await signIn(page, config.readerUserName, config.userPassword);
      await checkCreateNewWorkspaceLink(page);
    });
  });
});

async function checkCreateNewWorkspaceLink(page: Page): Promise<void> {
  const homePage = new HomePage(page);
  const plusIcon = homePage.getCreateNewWorkspaceLink();
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
